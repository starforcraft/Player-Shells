package com.ultramega.playershells.container;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.registry.ModItems;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class ShellSlotItemHandler extends ResourceHandlerSlot {
    private final ShellForgeBlockEntity shellForge;

    public ShellSlotItemHandler(final int index, final int x, final int y, final ShellForgeBlockEntity shellForge) {
        super(shellForge.inventoryHandler, shellForge.inventoryHandler::set, index, x, y);
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
