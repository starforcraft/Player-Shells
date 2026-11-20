package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.utils.PositionReference;
import com.ultramega.playershells.utils.ShellPlayer;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.utils.MathUtils.findTargetLevel;

public record TransferPlayerPacket(PositionReference oldShellForgePos, PositionReference newShellForgePos) implements CustomPacketPayload {
    public static final Type<TransferPlayerPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "transfer_player_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TransferPlayerPacket> STREAM_CODEC = StreamCodec.composite(
        PositionReference.STREAM_CODEC, TransferPlayerPacket::oldShellForgePos,
        PositionReference.STREAM_CODEC, TransferPlayerPacket::newShellForgePos,
        TransferPlayerPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final TransferPlayerPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final Level level = context.player().level();
            if (level.getServer() == null) {
                return;
            }
            transfer(level.getServer(), context.player(), data.oldShellForgePos(), data.newShellForgePos());
        }).exceptionally(e -> null);
    }

    public static void transfer(final MinecraftServer server,
                                final Player player,
                                @Nullable final PositionReference oldShellForgePos,
                                final PositionReference newShellForgePos) {
        final ServerLevel oldTargetLevel = findTargetLevel(server, oldShellForgePos);
        final ServerLevel newTargetLevel = findTargetLevel(server, newShellForgePos);

        if (oldShellForgePos == null || oldTargetLevel != player.level() || newTargetLevel == null
            || !(player instanceof ShellPlayer shellPlayer) || player.blockPosition().distSqr(oldShellForgePos.pos()) > 4
            || !(oldTargetLevel.getBlockEntity(oldShellForgePos.pos()) instanceof ShellForgeBlockEntity oldForge)
            || !(newTargetLevel.getChunkAt(newShellForgePos.pos()).getBlockEntity(newShellForgePos.pos()) instanceof ShellForgeBlockEntity newForge)
            || oldForge == newForge || oldForge.getShellState() != ShellStates.CREATE) {
            return;
        }

        final CompoundTag oldBody = shellPlayer.playershells$getData();
        if (newForge.transferPlayerTo(player, () -> { })) {
            oldForge.transferPlayerFrom(player, oldBody);
        }
    }
}
