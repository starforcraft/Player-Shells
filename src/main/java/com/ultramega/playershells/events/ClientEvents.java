package com.ultramega.playershells.events;

import com.ultramega.playershells.blockentities.renderer.ShellForgeBlockEntityRenderer;
import com.ultramega.playershells.entities.renderer.ShellRenderer;
import com.ultramega.playershells.gui.CentrifugeScreen;
import com.ultramega.playershells.gui.RadialMenuRenderer;
import com.ultramega.playershells.gui.ShellForgeScreen;
import com.ultramega.playershells.gui.ShellSelectionOverlay;
import com.ultramega.playershells.gui.layer.BloodLayer;
import com.ultramega.playershells.items.extensions.SyringeItemExtension;
import com.ultramega.playershells.packet.s2c.AfterDeathPacket;
import com.ultramega.playershells.packet.s2c.FinishedSyncPacket;
import com.ultramega.playershells.packet.s2c.SyncShellDataPacket;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.registry.ModMenuTypes;
import com.ultramega.playershells.registry.ModRenderTypes;
import com.ultramega.playershells.storage.ClientShellData;
import com.ultramega.playershells.utils.CameraHandler;
import com.ultramega.playershells.utils.SoundHandler;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import static com.ultramega.playershells.PlayerShells.MODID;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientEvents {
    private static final Identifier GLASS_LEFT_LOC = Identifier.fromNamespaceAndPath(MODID, "block/shell_forge_glass_left");
    private static final Identifier GLASS_RIGHT_LOC = Identifier.fromNamespaceAndPath(MODID, "block/shell_forge_glass_right");

    public static final StandaloneModelKey<BlockStateModelPart> GLASS_LEFT = new StandaloneModelKey<>(GLASS_LEFT_LOC::toString);
    public static final StandaloneModelKey<BlockStateModelPart> GLASS_RIGHT = new StandaloneModelKey<>(GLASS_RIGHT_LOC::toString);

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
    public static void onRegisterStandalone(final ModelEvent.RegisterStandalone event) {
        event.register(GLASS_LEFT, SimpleUnbakedStandaloneModel.simpleModelWrapper(GLASS_LEFT_LOC));
        event.register(GLASS_RIGHT, SimpleUnbakedStandaloneModel.simpleModelWrapper(GLASS_RIGHT_LOC));
    }

    @SubscribeEvent
    public static void reloadShellRenderers(final EntityRenderersEvent.AddLayers event) {
        ShellRenderer.reload(event.getContext());
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            ClientShellData.INSTANCE.clearPreviews();
            SoundHandler.stopAllBlockSounds();
            ShellForgeClient.clearSelection();
        }
    }

    @SubscribeEvent
    public static void onLogout(final ClientPlayerNetworkEvent.LoggingOut event) {
        ClientShellData.INSTANCE.clear();
        CameraHandler.resetPosition();
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isUsingItem() && mc.player.getUseItem().is(ModItems.EMPTY_SYRINGE.get())
            && mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    @SubscribeEvent
    public static void registerPipelines(final RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ModRenderTypes.CREATE_SHELL);
    }

    @SubscribeEvent
    public static void registerClientPayloads(final RegisterClientPayloadHandlersEvent event) {
        event.register(SyncShellDataPacket.TYPE, SyncShellDataPacket::handle);
        event.register(FinishedSyncPacket.TYPE, FinishedSyncPacket::handle);
        event.register(AfterDeathPacket.TYPE, AfterDeathPacket::handle);
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHELL_FORGE.get(), ShellForgeScreen::new);
        event.register(ModMenuTypes.CENTRIFUGE.get(), CentrifugeScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(MODID, "shells_selection"), ShellSelectionOverlay.INSTANCE);
        event.registerAboveAll(Identifier.fromNamespaceAndPath(MODID, "blood"), new BloodLayer());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SHELL_FORGE.get(), ShellForgeBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void cancelRenderingGuiLayers(final RenderGuiLayerEvent.Pre event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cancelRenderingHand(final RenderHandEvent event) {
        if (CameraHandler.isCameraOutsideOfPlayer()) {
            event.setCanceled(true);
        } else {
            SyringeItemExtension.renderHand(event);
        }
    }
}
