package com.ultramega.playershells.events;

import com.ultramega.playershells.commands.ShellRecoveryCommands;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.storage.ShellSavedData;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public final class CommonEvents {
    @SubscribeEvent
    public static void registerCommands(final RegisterCommandsEvent event) {
        ShellRecoveryCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntityTypes.SHELL_FORGE.get(),
            (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntityTypes.CENTRIFUGE.get(),
            (blockEntity, side) -> blockEntity.energyStorage
        );
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            ModBlockEntityTypes.CENTRIFUGE.get(),
            (blockEntity, side) -> blockEntity.inventoryHandler
        );
    }

    @SubscribeEvent
    public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShellSavedData.getShellData(player.level()).syncToClient(player);
        }
    }
}
