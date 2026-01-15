package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.PlayerStates;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record LeaveShellForgePacket(BlockPos shellForgePos) {
    public static void encode(final LeaveShellForgePacket data, final FriendlyByteBuf buf) {
        buf.writeBlockPos(data.shellForgePos);
    }

    public static LeaveShellForgePacket decode(final FriendlyByteBuf buf) {
        return new LeaveShellForgePacket(buf.readBlockPos());
    }

    public static void handle(final LeaveShellForgePacket data, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.level().getBlockEntity(data.shellForgePos) instanceof ShellForgeBlockEntity blockEntity) {
                blockEntity.setPlayerState(PlayerStates.GOING_OUT);
                blockEntity.setChanged();
            }
        });
        context.setPacketHandled(true);
    }
}
