package com.ultramega.playershells.network;

import com.ultramega.playershells.PlayerShells;
import com.ultramega.playershells.packet.c2s.LeaveShellForgePacket;
import com.ultramega.playershells.packet.c2s.ShellButtonPressedPacket;
import com.ultramega.playershells.packet.c2s.TransferPlayerPacket;
import com.ultramega.playershells.packet.c2s.ValidateShellForgePacket;
import com.ultramega.playershells.packet.s2c.AfterDeathPacket;
import com.ultramega.playershells.packet.s2c.FinishedSyncPacket;
import com.ultramega.playershells.packet.s2c.SyncShellDataPacket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(PlayerShells.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private ModNetworking() {
    }

    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworking::registerMessages);
    }

    private static void registerMessages() {
        int id = 0;

        CHANNEL.registerMessage(id++, ShellButtonPressedPacket.class, ShellButtonPressedPacket::encode, ShellButtonPressedPacket::decode, ShellButtonPressedPacket::handle);
        CHANNEL.registerMessage(id++, TransferPlayerPacket.class, TransferPlayerPacket::encode, TransferPlayerPacket::decode, TransferPlayerPacket::handle);
        CHANNEL.registerMessage(id++, LeaveShellForgePacket.class, LeaveShellForgePacket::encode, LeaveShellForgePacket::decode, LeaveShellForgePacket::handle);
        CHANNEL.registerMessage(id++, ValidateShellForgePacket.class, ValidateShellForgePacket::encode, ValidateShellForgePacket::decode, ValidateShellForgePacket::handle);

        CHANNEL.registerMessage(id++, SyncShellDataPacket.class, SyncShellDataPacket::encode, SyncShellDataPacket::decode, SyncShellDataPacket::handle);
        CHANNEL.registerMessage(id++, FinishedSyncPacket.class, FinishedSyncPacket::encode, FinishedSyncPacket::decode, FinishedSyncPacket::handle);
        CHANNEL.registerMessage(id++, AfterDeathPacket.class, AfterDeathPacket::encode, AfterDeathPacket::decode, AfterDeathPacket::handle);
    }

    public static <T> void sendToServer(final T message) {
        CHANNEL.sendToServer(message);
    }

    public static <T> void sendToPlayer(final ServerPlayer player, final T message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <T> void sendToAll(final T message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
}
