package com.ultramega.playershells.mixin;

import com.ultramega.playershells.utils.CameraAccess;
import com.ultramega.playershells.utils.CameraHandler;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera implements CameraAccess {
    @Override
    @Accessor("detached")
    public abstract void playershells$setDetached(boolean detached);

    @Override
    @Invoker("setPosition")
    public abstract void playershells$setPosition(Vec3 position);

    @Override
    @Invoker("setRotation")
    public abstract void playershells$setRotation(float yaw, float pitch, float roll);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;alignWithEntity(F)V", shift = At.Shift.AFTER))
    private void playershells$update(final DeltaTracker deltaTracker, final CallbackInfo ci) {
        CameraHandler.cameraTick((Camera) (Object) this);
    }
}
