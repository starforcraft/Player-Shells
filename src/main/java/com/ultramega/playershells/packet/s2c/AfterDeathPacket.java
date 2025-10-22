package com.ultramega.playershells.packet.s2c;

import com.ultramega.playershells.utils.CameraHandler;
import com.ultramega.playershells.utils.PositionReference;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.playershells.PlayerShells.MODID;

public record AfterDeathPacket(PositionReference shellForgePos) implements CustomPacketPayload {
    public static final Type<AfterDeathPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "after_death"));
    public static final StreamCodec<FriendlyByteBuf, AfterDeathPacket> STREAM_CODEC = StreamCodec.composite(
        PositionReference.STREAM_CODEC, AfterDeathPacket::shellForgePos,
        AfterDeathPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final AfterDeathPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            CameraHandler.setMovingAnimation(null, null, data.shellForgePos().pos().above(), data.shellForgePos().facing(), () -> { });
        }).exceptionally(e -> null);
    }
}
