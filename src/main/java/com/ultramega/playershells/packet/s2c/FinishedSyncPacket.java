package com.ultramega.playershells.packet.s2c;

import com.ultramega.playershells.utils.CameraHandler;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record FinishedSyncPacket() {
    public static void encode(final FinishedSyncPacket data, final FriendlyByteBuf buf) {
    }

    public static FinishedSyncPacket decode(final FriendlyByteBuf buf) {
        return new FinishedSyncPacket();
    }

    public static void handle(final FinishedSyncPacket data, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof DeathScreen) {
                mc.setScreen(null);
            }
            CameraHandler.resetPosition();
        });
        context.setPacketHandled(true);
    }
}
