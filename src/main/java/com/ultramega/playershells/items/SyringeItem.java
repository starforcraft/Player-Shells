package com.ultramega.playershells.items;

import com.ultramega.playershells.registry.ModDataComponentTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.utils.OwnerData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

import static com.ultramega.playershells.utils.MathUtils.near;

public class SyringeItem extends ItemWithOwner {
    public static final int MAX_EXTRACT_DURATION = 100;
    public static final float[] PULSE_POINTS = {0.1f, 0.3f, 0.5f, 0.7f, 0.95f};

    public SyringeItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand usedHand) {
        final boolean isOffhand = usedHand == InteractionHand.OFF_HAND;
        final boolean isOffhandEmpty = player.getItemInHand(InteractionHand.OFF_HAND).isEmpty();
        if (isOffhand || !isOffhandEmpty) {
            final MutableComponent component = Component.translatable(isOffhand
                ? "gui.playershells.syringe.cannot_use_offhand"
                : "gui.playershells.syringe.offhand_empty");
            player.sendOverlayMessage(component.withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        final ItemStack stack = player.getItemInHand(usedHand);
        if (stack.is(ModItems.EMPTY_SYRINGE.get())) {
            player.startUsingItem(usedHand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity entity, final ItemStack stack, final int remainingUseDuration) {
        if (!level.isClientSide()) {
            final float usePos = (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
            for (final float point : PULSE_POINTS) {
                if (near(usePos, point)) {
                    entity.hurtOrSimulate(entity.damageSources().magic(), 2.0F);
                }
            }
        }
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
    public ItemUseAnimation getUseAnimation(final ItemStack stack) {
        return ItemUseAnimation.NONE;
    }
}
