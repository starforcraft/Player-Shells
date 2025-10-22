package com.ultramega.playershells.items;

import com.ultramega.playershells.registry.ModDataComponentTypes;
import com.ultramega.playershells.utils.OwnerData;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ItemWithOwner extends Item {
    public ItemWithOwner(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> components, final TooltipFlag tooltipFlag) {
        if (stack.has(ModDataComponentTypes.OWNER_PLAYER.get())) {
            final OwnerData owner = stack.get(ModDataComponentTypes.OWNER_PLAYER.get());
            if (owner != null) {
                components.add(Component.translatable("tooltip.playershells.dna_owner", owner.playerName()).withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
