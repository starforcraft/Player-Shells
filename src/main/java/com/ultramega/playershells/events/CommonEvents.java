package com.ultramega.playershells.events;

import com.ultramega.playershells.gui.ShellSelectionOverlay;
import com.ultramega.playershells.storage.ShellSavedData;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public final class CommonEvents {
    @SubscribeEvent
    public static void onPlayerJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ShellSavedData.getShellData(serverLevel).syncToClient();
        }
    }

    @SubscribeEvent
    public static void onPlayerLeaveLevel(final EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Player && ShellSelectionOverlay.INSTANCE.isOpened()) {
            ShellSelectionOverlay.INSTANCE.close(false);
        }
    }
}
