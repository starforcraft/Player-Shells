package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.storage.ShellSavedData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.playershells.PlayerShells.MODID;

public record ValidateShellForgePacket() implements CustomPacketPayload {
    public static final Type<ValidateShellForgePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "validate_shell_forge_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ValidateShellForgePacket> STREAM_CODEC = StreamCodec.unit(new ValidateShellForgePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ValidateShellForgePacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level() instanceof ServerLevel serverLevel) {
                ShellSavedData.getShellData(serverLevel).validateShellData(serverLevel);
            }
        }).exceptionally(e -> null);
    }
}
