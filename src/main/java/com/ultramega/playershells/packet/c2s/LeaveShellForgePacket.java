package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.PlayerStates;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.playershells.PlayerShells.MODID;

public record LeaveShellForgePacket(BlockPos shellForgePos) implements CustomPacketPayload {
    public static final Type<LeaveShellForgePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "leave_shell_forge_packet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaveShellForgePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, LeaveShellForgePacket::shellForgePos,
        LeaveShellForgePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final LeaveShellForgePacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(data.shellForgePos()) instanceof ShellForgeBlockEntity blockEntity) {
                blockEntity.setPlayerState(PlayerStates.GOING_OUT);
                blockEntity.setChanged();
            }
        }).exceptionally(e -> null);
    }
}
