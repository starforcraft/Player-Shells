package com.ultramega.playershells.events;

import com.ultramega.playershells.blockentities.renderer.ShellForgeBlockEntityRenderer;
import com.ultramega.playershells.gui.CentrifugeScreen;
import com.ultramega.playershells.gui.RadialMenuRenderer;
import com.ultramega.playershells.gui.ShellForgeScreen;
import com.ultramega.playershells.gui.ShellSelectionOverlay;
import com.ultramega.playershells.items.extensions.SyringeItemExtension;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.registry.ModMenuTypes;
import com.ultramega.playershells.registry.ModRenderTypes;
import com.ultramega.playershells.utils.CameraHandler;

import java.io.IOException;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.items.SyringeItem.MAX_EXTRACT_DURATION;
import static net.minecraft.client.resources.model.ModelResourceLocation.STANDALONE_VARIANT;

@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientEvents {
    public static BakedModel shellForgeGlassLeft;
    public static BakedModel shellForgeGlassRight;

    private static final ModelResourceLocation GLASS_LEFT_LOC =
        new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(MODID, "block/shell_forge_glass_left"), STANDALONE_VARIANT);
    private static final ModelResourceLocation GLASS_RIGHT_LOC =
        new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(MODID, "block/shell_forge_glass_right"), STANDALONE_VARIANT);

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
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                ModItems.EMPTY_SYRINGE.get(),
                ResourceLocation.fromNamespaceAndPath(MODID, "extracting"),
                (stack, level, player, seed) -> player != null && player.isUsingItem() && player.getUseItem() == stack ? 1.0F : 0.0F
            );
            ItemProperties.register(
                ModItems.EMPTY_SYRINGE.get(),
                ResourceLocation.fromNamespaceAndPath(MODID, "extracting_progress"),
                (stack, level, player, seed) -> {
                    if (player == null) {
                        return 0.0F;
                    } else {
                        return player.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration(player) - player.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
                    }
                }
            );
        });
    }

    @SubscribeEvent
    public static void onRegisterAdditional(final ModelEvent.RegisterAdditional event) {
        event.register(GLASS_LEFT_LOC);
        event.register(GLASS_RIGHT_LOC);
    }

    @SubscribeEvent
    public static void onBakingCompleted(final ModelEvent.BakingCompleted event) {
        ClientEvents.shellForgeGlassLeft = event.getModels().get(GLASS_LEFT_LOC);
        ClientEvents.shellForgeGlassRight = event.getModels().get(GLASS_RIGHT_LOC);
    }

    @SubscribeEvent
    public static void registerClientExtensions(final RegisterClientExtensionsEvent event) {
        event.registerItem(new SyringeItemExtension(), ModItems.EMPTY_SYRINGE.get());
    }

    @SubscribeEvent
    public static void registerScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.SHELL_FORGE.get(), ShellForgeScreen::new);
        event.register(ModMenuTypes.CENTRIFUGE.get(), CentrifugeScreen::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(final RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MODID, "shells_selection"), ShellSelectionOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SHELL_FORGE.get(), ShellForgeBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(MODID, "create_shader"),
                        DefaultVertexFormat.NEW_ENTITY),
                shaderInstance -> ModRenderTypes.createShaderInstance = shaderInstance);
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
        }
    }
}
