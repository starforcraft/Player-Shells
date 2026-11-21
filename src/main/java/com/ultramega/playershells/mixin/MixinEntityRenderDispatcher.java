package com.ultramega.playershells.mixin;

import com.ultramega.playershells.entities.renderer.ShellRenderer;
import com.ultramega.playershells.entities.renderer.ShellRenderer.State;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @SuppressWarnings("unchecked")
    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
        at = @At("HEAD"), cancellable = true)
    private <S extends EntityRenderState> void playershells$getRenderer(final S state,
        final CallbackInfoReturnable<EntityRenderer<?, ? super S>> callback) {
        if (state instanceof State shell) {
            callback.setReturnValue((EntityRenderer<?, ? super S>) ShellRenderer.get(shell.skin.model()));
        }
    }
}
