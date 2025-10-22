package com.ultramega.playershells.utils;

import net.minecraft.nbt.CompoundTag;

public interface ShellPlayer {
    void playershells$applyData(CompoundTag tag, PositionReference posReference);

    CompoundTag playershells$getData();
}
