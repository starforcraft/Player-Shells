package com.ultramega.playershells.mixin;

import com.ultramega.playershells.utils.CameraHandler;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public final class MixinCamera {
    @Inject(method = "setup", at = @At("RETURN"))
    public void setup(final BlockGetter level,
                      final Entity entity,
                      final boolean detached,
                      final boolean thirdPersonReverse,
                      final float partialTick,
                      final CallbackInfo ci) {
        CameraHandler.cameraTick((Camera) (Object) this);
    }
}
