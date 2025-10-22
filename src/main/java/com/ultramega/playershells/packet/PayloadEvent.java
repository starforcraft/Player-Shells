package com.ultramega.playershells.packet;

import com.ultramega.playershells.PlayerShells;
import com.ultramega.playershells.packet.c2s.LeaveShellForgePacket;
import com.ultramega.playershells.packet.c2s.ShellButtonPressedPacket;
import com.ultramega.playershells.packet.c2s.TransferPlayerPacket;
import com.ultramega.playershells.packet.c2s.ValidateShellForgePacket;
import com.ultramega.playershells.packet.s2c.AfterDeathPacket;
import com.ultramega.playershells.packet.s2c.FinishedSyncPacket;
import com.ultramega.playershells.packet.s2c.SyncShellDataPacket;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = PlayerShells.MODID)
public final class PayloadEvent {
    private PayloadEvent() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PlayerShells.MODID).versioned("1.0");
        registrar.playToServer(
            ShellButtonPressedPacket.TYPE,
            ShellButtonPressedPacket.STREAM_CODEC,
            ShellButtonPressedPacket::handle
        );
        registrar.playToServer(
            TransferPlayerPacket.TYPE,
            TransferPlayerPacket.STREAM_CODEC,
            TransferPlayerPacket::handle
        );
        registrar.playToServer(
            LeaveShellForgePacket.TYPE,
            LeaveShellForgePacket.STREAM_CODEC,
            LeaveShellForgePacket::handle
        );
        registrar.playToServer(
            ValidateShellForgePacket.TYPE,
            ValidateShellForgePacket.STREAM_CODEC,
            ValidateShellForgePacket::handle
        );

        registrar.playToClient(
            SyncShellDataPacket.TYPE,
            SyncShellDataPacket.STREAM_CODEC,
            SyncShellDataPacket::handle
        );
        registrar.playToClient(
            FinishedSyncPacket.TYPE,
            FinishedSyncPacket.STREAM_CODEC,
            FinishedSyncPacket::handle
        );
        registrar.playToClient(
            AfterDeathPacket.TYPE,
            AfterDeathPacket.STREAM_CODEC,
            AfterDeathPacket::handle
        );
    }
}
