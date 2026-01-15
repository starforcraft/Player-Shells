package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.utils.PositionReference;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import static com.ultramega.playershells.utils.MathUtils.findTargetLevel;

public record TransferPlayerPacket(PositionReference oldShellForgePos, PositionReference newShellForgePos) {
    public static void encode(final TransferPlayerPacket data, final FriendlyByteBuf buf) {
        data.oldShellForgePos.write(buf);
        data.newShellForgePos.write(buf);
    }

    public static TransferPlayerPacket decode(final FriendlyByteBuf buf) {
        return new TransferPlayerPacket(PositionReference.read(buf), PositionReference.read(buf));
    }

    public static void handle(final TransferPlayerPacket data, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final Player player = context.getSender();
            if (player == null) {
                return;
            }
            final Level level = player.level();
            if (level.getServer() == null) {
                return;
            }
            transfer(level.getServer(), player, data.oldShellForgePos, data.newShellForgePos);
        });
        context.setPacketHandled(true);
    }

    public static void transfer(final MinecraftServer server,
                                final Player player,
                                @Nullable final PositionReference oldShellForgePos,
                                final PositionReference newShellForgePos) {
        // Prevent vehicle/passenger desync during teleport/body transfer.
        player.stopRiding();
        player.ejectPassengers();

        final ServerLevel oldTargetLevel = findTargetLevel(server, oldShellForgePos);
        final ServerLevel newTargetLevel = findTargetLevel(server, newShellForgePos);

        if (oldShellForgePos != null && oldTargetLevel != null) {
            oldTargetLevel.getChunkAt(oldShellForgePos.pos());
        }
        if (newTargetLevel != null) {
            newTargetLevel.getChunkAt(newShellForgePos.pos());
        }

        if (oldShellForgePos != null && oldTargetLevel != null && oldTargetLevel.getBlockEntity(oldShellForgePos.pos()) instanceof ShellForgeBlockEntity shellForge) {
            shellForge.transferPlayerFrom(player);
        }
        if (newTargetLevel != null && newTargetLevel.getBlockEntity(newShellForgePos.pos()) instanceof ShellForgeBlockEntity shellForge
            && shellForge.getShellState() == ShellStates.EXTERMINATE) {
            shellForge.transferPlayerTo(player);
        }
    }
}
