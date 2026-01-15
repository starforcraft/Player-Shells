package com.ultramega.playershells.container;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.registry.ModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ShellSlotItemHandler extends SlotItemHandler {
    private final ShellForgeBlockEntity shellForge;

    public ShellSlotItemHandler(final IItemHandler itemHandler, final int index, final int x, final int y, final ShellForgeBlockEntity shellForge) {
        super(itemHandler, index, x, y);
        this.shellForge = shellForge;
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        return super.mayPlace(stack) && stack.getItem() == ModItems.DNA.get();
    }

    @Override
    public boolean isActive() {
        return this.shellForge.getShellState() == ShellStates.CREATE;
    }
}
