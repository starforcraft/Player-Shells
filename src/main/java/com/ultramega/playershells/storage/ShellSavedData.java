package com.ultramega.playershells.storage;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.network.ModNetworking;
import com.ultramega.playershells.packet.s2c.SyncShellDataPacket;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

import static com.ultramega.playershells.utils.MathUtils.findTargetLevel;
import static java.util.Objects.requireNonNull;

public class ShellSavedData extends SavedData {
    private static final String ENTRIES_TAG = "entries";
    private static final String PLAYER_UUID_TAG = "player_uuid";
    private static final String SHELL_STATE_TAG = "shell_state";

    private final Multimap<UUID, ShellState> entries;

    public ShellSavedData() {
        this.entries = ArrayListMultimap.create();
    }

    public ShellSavedData(final CompoundTag tag) {
        this.entries = ArrayListMultimap.create();
        if (tag.contains(ENTRIES_TAG, CompoundTag.TAG_LIST)) {
            final ListTag list = tag.getList(ENTRIES_TAG, CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                final CompoundTag entryTag = list.getCompound(i);
                if (!entryTag.hasUUID(PLAYER_UUID_TAG) || !entryTag.contains(SHELL_STATE_TAG, CompoundTag.TAG_COMPOUND)) {
                    continue;
                }
                final UUID playerUuid = entryTag.getUUID(PLAYER_UUID_TAG);
                final ShellState shellState = ShellState.fromNbt(entryTag.getCompound(SHELL_STATE_TAG));
                this.entries.put(playerUuid, shellState);
            }
        }
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
    public ShellState getNearestActive(final UUID playerUuid, final ResourceLocation location, final BlockPos playerPos) {
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
    public CompoundTag save(final CompoundTag tag) {
        final ListTag list = new ListTag();
        for (final Entry<UUID, ShellState> entry : this.entries.entries()) {
            final ShellState shellState = entry.getValue();
            if (shellState == null) {
                continue;
            }
            final CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(PLAYER_UUID_TAG, entry.getKey());
            entryTag.put(SHELL_STATE_TAG, shellState.toNbt());
            list.add(entryTag);
        }
        tag.put(ENTRIES_TAG, list);
        return tag;
    }

    @Override
    public void setDirty() {
        super.setDirty();
        this.syncToClient();
    }

    public void validateShellData(final Level level) {
        final Iterator<Entry<UUID, ShellState>> iterator = this.entries.entries().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, ShellState> entry = iterator.next();
            final ShellState shellState = entry.getValue();

            if (shellState == null || level.getServer() == null) {
                iterator.remove();
                continue;
            }

            final ServerLevel targetLevel = findTargetLevel(level.getServer(), shellState.shellForgePos());
            if (targetLevel == null) {
                iterator.remove();
                continue;
            }

            final BlockPos pos = shellState.shellForgePos().pos();
            if (!targetLevel.hasChunkAt(pos)) {
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
        ModNetworking.sendToAll(new SyncShellDataPacket(ImmutableListMultimap.copyOf(this.entries)));
    }

    public static ShellSavedData getShellData(final ServerLevel level) {
        final ServerLevel serverLevel = requireNonNull(level.getServer().getLevel(Level.OVERWORLD));
        return serverLevel.getDataStorage().computeIfAbsent(ShellSavedData::new, ShellSavedData::new, "player_shells");
    }
}
