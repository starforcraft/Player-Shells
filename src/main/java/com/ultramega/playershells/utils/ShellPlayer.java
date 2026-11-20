package com.ultramega.playershells.utils;

import net.minecraft.nbt.CompoundTag;

public interface ShellPlayer {
    boolean playershells$applyData(CompoundTag tag, PositionReference posReference, Runnable afterTeleport);

    CompoundTag playershells$getData();
}
