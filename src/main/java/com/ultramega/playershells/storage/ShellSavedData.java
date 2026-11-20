package com.ultramega.playershells.storage;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.packet.s2c.SyncShellDataPacket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.utils.MathUtils.findTargetLevel;

public class ShellSavedData extends SavedData {
    private static final SavedDataType<ShellSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(MODID, "player_shells"),
        () -> new ShellSavedData(ArrayListMultimap.create()),
        ShellState.MULTIMAP_CODEC.xmap(ShellSavedData::new, data -> data.entries)
    );

    private final Multimap<UUID, ShellState> entries;

    private ShellSavedData(final Multimap<UUID, ShellState> entries) {
        this.entries = ArrayListMultimap.create(entries);
    }

    public void add(final UUID playerUuid, final ShellState shellState) {
        this.entries.put(playerUuid, shellState);
        this.setDirty();
    }

    public void updateShellCreationProgress(final UUID playerUuid, final UUID shellUuid, final int newShellCreationProgress) {
        final ShellState oldShellState = this.get(playerUuid, shellUuid);
        if (oldShellState != null) {
            final ShellState newShellState = new ShellState(
                oldShellState.shellUUID(),
                oldShellState.shellForgePos(),
                oldShellState.playerData(),
                newShellCreationProgress
            );

            final Collection<ShellState> states = this.entries.get(playerUuid);
            states.remove(oldShellState);
            states.add(newShellState);

            this.setDirty();
        }
    }

    public Collection<ShellState> getAll(final UUID playerUuid) {
        return this.entries.get(playerUuid);
    }

    @Nullable
    public ShellState get(final UUID playerUuid, final UUID shellUuid) {
        for (final ShellState state : this.entries.get(playerUuid)) {
            if (state != null && state.shellUUID().equals(shellUuid)) {
                return state;
            }
        }

        return null;
    }

    @Nullable
    public ShellState getNearestActive(final UUID playerUuid, final Identifier location, final BlockPos playerPos) {
        ShellState nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        ShellState fallback = null;

        for (final ShellState shellState : this.getAll(playerUuid)) {
            if (shellState.shellCreationProgress() == 100) {
                if (shellState.shellForgePos().dimension().equals(location)) {
                    final double distSqr = playerPos.distSqr(shellState.shellForgePos().pos());
                    if (distSqr < bestDistSq) {
                        bestDistSq = distSqr;
                        nearest = shellState;
                    }
                } else if (fallback == null) {
                    fallback = shellState;
                }
            }
        }

        return nearest != null ? nearest : fallback;
    }

    @Override
    public void setDirty() {
        super.setDirty();
        this.syncToClient();
    }

    public void validateShellData(final ServerLevel level) {
        final Iterator<Entry<UUID, ShellState>> iterator = this.entries.entries().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, ShellState> entry = iterator.next();
            final ShellState shellState = entry.getValue();

            if (shellState == null) {
                iterator.remove();
                continue;
            }

            final ServerLevel targetLevel = findTargetLevel(level.getServer(), shellState.shellForgePos());
            if (targetLevel == null) {
                iterator.remove();
                continue;
            }

            final BlockPos pos = shellState.shellForgePos().pos();
            if (!targetLevel.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            final BlockEntity blockEntity = targetLevel.getBlockEntity(pos);
            if (!(blockEntity instanceof ShellForgeBlockEntity shellForge)) {
                iterator.remove();
                continue;
            }

            if (!shellForge.getShellUuid().equals(shellState.shellUUID())) {
                iterator.remove();
            }
        }
        this.setDirty();
    }

    public void syncToClient() {
        PacketDistributor.sendToAllPlayers(new SyncShellDataPacket(ImmutableListMultimap.copyOf(this.entries)));
    }

    public void syncToClient(final ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncShellDataPacket(ImmutableListMultimap.copyOf(this.entries)));
    }

    public static ShellSavedData getShellData(final ServerLevel level) {
        final MinecraftServer server = level.getServer();
        final SavedDataStorage storage = server.getDataStorage();
        final ShellSavedData existing = storage.get(TYPE);
        if (existing != null) {
            return existing;
        }

        final Path legacy = server.getWorldPath(LevelResource.DATA).resolve("player_shells.dat");
        final ShellSavedData data = new ShellSavedData(ArrayListMultimap.create());
        if (Files.isRegularFile(legacy)) {
            try {
                final CompoundTag root = NbtIo.readCompressed(legacy, NbtAccounter.unlimitedHeap());
                final int version = NbtUtils.getDataVersion(root, 3955);
                final var entries = ShellState.MULTIMAP_CODEC.parse(NbtOps.INSTANCE, root.getCompoundOrEmpty("data")).getOrThrow();
                entries.forEach((owner, shell) -> data.entries.put(owner, new ShellState(
                    shell.shellUUID(), shell.shellForgePos(),
                    DataFixTypes.PLAYER.updateToCurrentVersion(server.getFixerUpper(), shell.playerData(), version),
                    shell.shellCreationProgress()
                )));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read legacy Player Shells data", e);
            }
        }
        storage.set(TYPE, data);
        return data;
    }
}
