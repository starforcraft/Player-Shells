package com.ultramega.playershells.items;

import com.ultramega.playershells.registry.ModDataComponentTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.utils.OwnerData;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class SyringeItem extends ItemWithOwner {
    public static final int MAX_EXTRACT_DURATION = 100;

    public SyringeItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand usedHand) {
        final ItemStack stack = player.getItemInHand(usedHand);
        if (stack.is(ModItems.EMPTY_SYRINGE.get())) {
            player.startUsingItem(usedHand);
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
        }
        return InteractionResultHolder.fail(player.getItemInHand(usedHand));
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity livingEntity, final ItemStack stack, final int remainingUseDuration) {
        // TODO: damage entity with 4 hearts
    }

    @Override
    public ItemStack finishUsingItem(final ItemStack stack, final Level level, final LivingEntity entity) {
        final ItemStack filledSyringe = ModItems.BLOOD_SYRINGE.get().getDefaultInstance();
        if (entity instanceof Player player) {
            filledSyringe.set(ModDataComponentTypes.OWNER_PLAYER.get(), new OwnerData(player.getUUID(), player.getDisplayName().getString()));
        }
        return filledSyringe;
    }

    @Override
    public int getUseDuration(final ItemStack stack, final LivingEntity entity) {
        return MAX_EXTRACT_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
    }
}
