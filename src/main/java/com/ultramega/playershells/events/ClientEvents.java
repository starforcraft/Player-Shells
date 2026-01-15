package com.ultramega.playershells.events;

import com.ultramega.playershells.blockentities.renderer.ShellForgeBlockEntityRenderer;
import com.ultramega.playershells.gui.CentrifugeScreen;
import com.ultramega.playershells.gui.ShellForgeScreen;
import com.ultramega.playershells.gui.ShellSelectionOverlay;
import com.ultramega.playershells.gui.layer.BloodLayer;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.registry.ModMenuTypes;
import com.ultramega.playershells.registry.ModRenderTypes;

import java.io.IOException;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.items.SyringeItem.MAX_EXTRACT_DURATION;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    public static BakedModel shellForgeGlassLeft;
    public static BakedModel shellForgeGlassRight;

    private static final ResourceLocation GLASS_LEFT_LOC = new ResourceLocation(MODID, "block/shell_forge_glass_left");
    private static final ResourceLocation GLASS_RIGHT_LOC = new ResourceLocation(MODID, "block/shell_forge_glass_right");

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                ModItems.EMPTY_SYRINGE.get(),
                new ResourceLocation(MODID, "extracting"),
                (stack, level, player, seed) -> player != null && player.isUsingItem() && player.getUseItem() == stack ? 1.0F : 0.0F
            );
            ItemProperties.register(
                ModItems.EMPTY_SYRINGE.get(),
                new ResourceLocation(MODID, "extracting_progress"),
                (stack, level, player, seed) -> {
                    if (player == null) {
                        return 0.0F;
                    }
                    return player.getUseItem() != stack ? 0.0F : (float) (stack.getUseDuration() - player.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
                }
            );

            MenuScreens.register(ModMenuTypes.SHELL_FORGE.get(), ShellForgeScreen::new);
            MenuScreens.register(ModMenuTypes.CENTRIFUGE.get(), CentrifugeScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterAdditional(final ModelEvent.RegisterAdditional event) {
        event.register(GLASS_LEFT_LOC);
        event.register(GLASS_RIGHT_LOC);
    }

    @SubscribeEvent
    public static void onBakingCompleted(final ModelEvent.BakingCompleted event) {
        shellForgeGlassLeft = event.getModels().get(GLASS_LEFT_LOC);
        shellForgeGlassRight = event.getModels().get(GLASS_RIGHT_LOC);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(final RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("shells_selection", ShellSelectionOverlay.INSTANCE);
        event.registerAboveAll("blood", new BloodLayer());
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SHELL_FORGE.get(), ShellForgeBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(final RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), new ResourceLocation(MODID, "create_shader"), DefaultVertexFormat.NEW_ENTITY),
            shaderInstance -> ModRenderTypes.createShaderInstance = shaderInstance
        );
    }
}
