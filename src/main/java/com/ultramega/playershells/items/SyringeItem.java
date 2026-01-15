package com.ultramega.playershells.items;

import com.ultramega.playershells.items.extensions.SyringeItemExtension;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.utils.OwnerData;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import static com.ultramega.playershells.utils.MathUtils.near;

public class SyringeItem extends ItemWithOwner {
    public static final int MAX_EXTRACT_DURATION = 100;
    public static final float[] PULSE_POINTS = {0.1f, 0.3f, 0.5f, 0.7f, 0.95f};

    public SyringeItem(final Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(final Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new SyringeItemExtension());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand usedHand) {
        final boolean isOffhand = usedHand == InteractionHand.OFF_HAND;
        final boolean isOffhandEmpty = player.getItemInHand(InteractionHand.OFF_HAND).isEmpty();
        if (isOffhand || !isOffhandEmpty) {
            final MutableComponent component = Component.translatable(isOffhand
                ? "gui.playershells.syringe.cannot_use_offhand"
                : "gui.playershells.syringe.offhand_empty");
            player.displayClientMessage(component.withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }

        final ItemStack stack = player.getItemInHand(usedHand);
        if (stack.is(ModItems.EMPTY_SYRINGE.get())) {
            player.startUsingItem(usedHand);
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
        }
        return InteractionResultHolder.fail(player.getItemInHand(usedHand));
    }

    @Override
    public void onUseTick(final Level level, final LivingEntity entity, final ItemStack stack, final int remainingUseDuration) {
        if (level.isClientSide()) {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }

        if (!level.isClientSide()) {
            final float usePos = (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
            for (final float point : PULSE_POINTS) {
                if (near(usePos, point)) {
                    entity.hurt(entity.damageSources().magic(), 2.0F);
                }
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(final ItemStack stack, final Level level, final LivingEntity entity) {
        final ItemStack filledSyringe = ModItems.BLOOD_SYRINGE.get().getDefaultInstance();
        if (entity instanceof Player player) {
            OwnerData.setOnStack(filledSyringe, new OwnerData(player.getUUID(), player.getDisplayName().getString()));
        }
        return filledSyringe;
    }

    @Override
    public int getUseDuration(final ItemStack stack) {
        return MAX_EXTRACT_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
    }
}
