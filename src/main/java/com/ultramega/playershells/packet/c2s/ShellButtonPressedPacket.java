package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ShellButtonPressedPacket(BlockPos shellForgePos) {
    public static void encode(final ShellButtonPressedPacket data, final FriendlyByteBuf buf) {
        buf.writeBlockPos(data.shellForgePos);
    }

    public static ShellButtonPressedPacket decode(final FriendlyByteBuf buf) {
        return new ShellButtonPressedPacket(buf.readBlockPos());
    }

    public static void handle(final ShellButtonPressedPacket data, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.level().getBlockEntity(data.shellForgePos) instanceof ShellForgeBlockEntity blockEntity) {
                blockEntity.shellButtonPressed();
            }
        });
        context.setPacketHandled(true);
    }
}
