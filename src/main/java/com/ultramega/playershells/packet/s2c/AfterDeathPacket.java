package com.ultramega.playershells.packet.s2c;

import com.ultramega.playershells.utils.CameraHandler;
import com.ultramega.playershells.utils.PositionReference;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record AfterDeathPacket(PositionReference shellForgePos) {
    public static void encode(final AfterDeathPacket data, final FriendlyByteBuf buf) {
        data.shellForgePos.write(buf);
    }

    public static AfterDeathPacket decode(final FriendlyByteBuf buf) {
        return new AfterDeathPacket(PositionReference.read(buf));
    }

    public static void handle(final AfterDeathPacket data, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> CameraHandler.setMovingAnimation(null, null, data.shellForgePos.pos().above(), data.shellForgePos.facing(), () -> { }));
        context.setPacketHandled(true);
    }
}
