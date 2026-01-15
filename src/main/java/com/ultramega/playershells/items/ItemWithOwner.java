package com.ultramega.playershells.items;

import com.ultramega.playershells.utils.OwnerData;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemWithOwner extends Item {
    public ItemWithOwner(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level level, final List<Component> components, final TooltipFlag tooltipFlag) {
        final OwnerData owner = OwnerData.getFromStack(stack);
        if (owner != null) {
            components.add(Component.translatable("tooltip.playershells.dna_owner", owner.playerName()).withStyle(ChatFormatting.AQUA));
        }
    }
}
