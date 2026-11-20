package com.ultramega.playershells.utils;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MachineInventory extends SimpleContainer {
    public MachineInventory(final int size) {
        super(size);
    }

    public void load(final ValueInput input) {
        this.clearContent();
        for (final ValueInput entry : input.childrenListOrEmpty("Items")) {
            final int slot = entry.getIntOr("Slot", -1);
            if (slot >= 0 && slot < this.getContainerSize()) {
                entry.read(ItemStack.MAP_CODEC).ifPresent(stack -> this.setItem(slot, stack));
            }
        }
    }

    public void save(final ValueOutput output) {
        final ValueOutput.ValueOutputList list = output.childrenList("Items");
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            final ItemStack stack = this.getItem(slot);
            if (!stack.isEmpty()) {
                final ValueOutput entry = list.addChild();
                entry.putInt("Slot", slot);
                entry.store(ItemStack.MAP_CODEC, stack);
            }
        }
    }
}
