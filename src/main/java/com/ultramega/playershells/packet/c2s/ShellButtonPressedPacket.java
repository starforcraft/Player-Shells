package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.container.ShellForgeContainerMenu;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.playershells.PlayerShells.MODID;

public record ShellButtonPressedPacket(BlockPos shellForgePos, UUID confirmedShell) implements CustomPacketPayload {
    public static final Type<ShellButtonPressedPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "shell_button_pressed_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShellButtonPressedPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ShellButtonPressedPacket::shellForgePos,
        UUIDUtil.STREAM_CODEC, ShellButtonPressedPacket::confirmedShell,
        ShellButtonPressedPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ShellButtonPressedPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ShellForgeContainerMenu menu
                && menu.stillValid(player) && menu.getBlockEntity().getBlockPos().equals(data.shellForgePos())
                && player.level().getBlockEntity(data.shellForgePos()) == menu.getBlockEntity()) {
                menu.getBlockEntity().shellButtonPressed(data.confirmedShell());
            }
        }).exceptionally(e -> null);
    }
}
