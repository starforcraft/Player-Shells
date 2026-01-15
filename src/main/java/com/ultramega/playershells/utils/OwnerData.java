package com.ultramega.playershells.utils;

import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public record OwnerData(UUID playerUUID, String playerName) {
    private static final String ROOT_TAG = "playershells_owner";
    private static final String UUID_TAG = "uuid";
    private static final String NAME_TAG = "name";

    public static void setOnStack(final ItemStack stack, final OwnerData ownerData) {
        final CompoundTag root = stack.getOrCreateTag();
        final CompoundTag ownerTag = new CompoundTag();
        ownerTag.putUUID(UUID_TAG, ownerData.playerUUID());
        ownerTag.putString(NAME_TAG, ownerData.playerName());
        root.put(ROOT_TAG, ownerTag);
    }

    public static boolean hasOnStack(final ItemStack stack) {
        return getFromStack(stack) != null;
    }

    @Nullable
    public static OwnerData getFromStack(final ItemStack stack) {
        final CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_TAG, CompoundTag.TAG_COMPOUND)) {
            return null;
        }

        final CompoundTag ownerTag = root.getCompound(ROOT_TAG);
        if (!ownerTag.hasUUID(UUID_TAG) || !ownerTag.contains(NAME_TAG, CompoundTag.TAG_STRING)) {
            return null;
        }

        return new OwnerData(ownerTag.getUUID(UUID_TAG), ownerTag.getString(NAME_TAG));
    }
}
