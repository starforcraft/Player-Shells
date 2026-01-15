package com.ultramega.playershells.events;

import com.ultramega.playershells.gui.RadialMenuRenderer;
import com.ultramega.playershells.gui.ShellSelectionOverlay;
import com.ultramega.playershells.utils.CameraHandler;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static com.ultramega.playershells.PlayerShells.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.FORGE)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onMouseInput(final InputEvent.MouseButton.Pre event) {
        final Minecraft mc = Minecraft.getInstance();
        if (ShellSelectionOverlay.INSTANCE.getDisplayedShell() != null && mc.screen == null) {
            final RadialMenuRenderer.MousePos mousePos = RadialMenuRenderer.getMousePos();

            ShellSelectionOverlay.INSTANCE.mouseClick(mousePos.x(), mousePos.y(), event.getButton(), event.getAction());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(final InputEvent.Key event) {
        final Minecraft mc = Minecraft.getInstance();
        if (ShellSelectionOverlay.INSTANCE.getDisplayedShell() != null && mc.screen == null) {
            ShellSelectionOverlay.INSTANCE.keyPressed(event.getKey());
        }
    }

    @SubscribeEvent
    public static void cancelRenderingGuiOverlays(final RenderGuiOverlayEvent.Pre event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cancelRenderingHand(final RenderHandEvent event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }
}

