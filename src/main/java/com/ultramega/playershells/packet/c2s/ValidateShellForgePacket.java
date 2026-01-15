package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.storage.ShellSavedData;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

public record ValidateShellForgePacket() {
    public static void encode(final ValidateShellForgePacket data, final FriendlyByteBuf buf) {
    }

    public static ValidateShellForgePacket decode(final FriendlyByteBuf buf) {
        return new ValidateShellForgePacket();
    }

    public static void handle(final ValidateShellForgePacket data, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null && context.getSender().level() instanceof ServerLevel serverLevel) {
                ShellSavedData.getShellData(serverLevel).validateShellData(serverLevel);
            }
        });
        context.setPacketHandled(true);
    }
}
