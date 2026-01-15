package com.ultramega.playershells.mixin;

import com.ultramega.playershells.entities.ShellEntity;
import com.ultramega.playershells.entities.renderer.ShellRenderer;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Unique
    private static final Map<String, EntityRendererProvider<ShellEntity>> SHELL_PROVIDERS = Map.of(
        "default", (EntityRendererProvider) (context) -> new ShellRenderer(context, false),
        "slim", (EntityRendererProvider) (context) -> new ShellRenderer(context, true));

    @Shadow
    @Final
    private ItemRenderer itemRenderer;
    @Shadow
    @Final
    private BlockRenderDispatcher blockRenderDispatcher;
    @Shadow
    @Final
    private ItemInHandRenderer itemInHandRenderer;
    @Shadow
    @Final
    private EntityModelSet entityModels;
    @Shadow
    @Final
    private Font font;

    @Unique
    private Map<String, EntityRenderer<? extends ShellEntity>> playershells$shellRenderers = Map.of();

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void playerShells$getRenderer(final T entity, final CallbackInfoReturnable<EntityRenderer<? super T>> cir) {
        if (entity instanceof ShellEntity shell) {
            final String modelName = shell.getModelName();
            final EntityRenderer<? extends ShellEntity> entityrenderer = this.playershells$shellRenderers.get(modelName);
            cir.setReturnValue(entityrenderer != null ? (EntityRenderer) entityrenderer : (EntityRenderer) this.playershells$shellRenderers.get("default"));
        }
    }

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void playershells$reload(final ResourceManager resourceManager, final CallbackInfo ci) {
        final EntityRendererProvider.Context context = new EntityRendererProvider.Context((EntityRenderDispatcher) (Object) this,
            this.itemRenderer, this.blockRenderDispatcher, this.itemInHandRenderer, resourceManager, this.entityModels, this.font);
        this.playershells$shellRenderers = playershells$createShellRenderers(context);
    }

    @Unique
    private static Map<String, EntityRenderer<? extends ShellEntity>> playershells$createShellRenderers(final EntityRendererProvider.Context context) {
        final ImmutableMap.Builder<String, EntityRenderer<? extends ShellEntity>> builder = ImmutableMap.builder();
        SHELL_PROVIDERS.forEach((model, provider) -> {
            try {
                builder.put(model, provider.create(context));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to create shell model for " + model, exception);
            }
        });
        return builder.build();
    }
}
