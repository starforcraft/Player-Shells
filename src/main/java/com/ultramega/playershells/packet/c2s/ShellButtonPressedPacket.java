package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.playershells.PlayerShells.MODID;

public record ShellButtonPressedPacket(BlockPos shellForgePos) implements CustomPacketPayload {
    public static final Type<ShellButtonPressedPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "shell_button_pressed_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShellButtonPressedPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ShellButtonPressedPacket::shellForgePos,
        ShellButtonPressedPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ShellButtonPressedPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(data.shellForgePos()) instanceof ShellForgeBlockEntity blockEntity) {
                blockEntity.shellButtonPressed();
            }
        }).exceptionally(e -> null);
    }
}
