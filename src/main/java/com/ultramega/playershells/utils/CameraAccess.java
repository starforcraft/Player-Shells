package com.ultramega.playershells.utils;

import net.minecraft.world.phys.Vec3;

public interface CameraAccess {
    void playershells$setDetached(boolean detached);

    void playershells$setPosition(Vec3 position);

    void playershells$setRotation(float yaw, float pitch, float roll);
}
