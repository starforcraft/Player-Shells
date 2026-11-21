package com.ultramega.playershells.items.extensions;

import com.ultramega.playershells.registry.ModItems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class SyringeItemExtension {
    public static void renderHand(final RenderHandEvent event) {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        final ItemStack syringe = event.getItemStack();
        if (player == null
            || event.getHand() != InteractionHand.MAIN_HAND
            || !syringe.is(ModItems.EMPTY_SYRINGE.get())
            || !player.getOffhandItem().isEmpty()) {
            return;
        }

        final PoseStack pose = event.getPoseStack();
        final SubmitNodeCollector nodes = event.getSubmitNodeCollector();
        final int light = event.getPackedLight();
        final int duration = syringe.getUseDuration(player);
        final boolean extracting = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND;
        final int ticks = extracting ? duration - player.getUseItemRemainingTicks() : 0;
        final float equipped = extracting ? 0 : event.getEquipProgress();

        event.setCanceled(true);
        pose.pushPose();
        if (extracting) {
            renderLeftPlayerArm(mc, player, pose, nodes, light, event.getPartialTick(), ticks);
        }
        renderRightPlayerArm(mc, player, pose, nodes, light, event.getPartialTick(), equipped, ticks, duration);
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, syringe, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, pose, nodes, light);
        pose.popPose();
    }

    private static void renderLeftPlayerArm(final Minecraft mc,
                                     final LocalPlayer localPlayer,
                                     final PoseStack poseStack,
                                     final SubmitNodeCollector buffer,
                                     final int packedLight,
                                     final float partialTicks,
                                     final int useTicks) {
        final float t = Mth.clamp((useTicks + partialTicks) / 6.0F, 0.0F, 1.0F);
        final float moveFactor = Mth.lerp(t, -1.6F, 0.0F);

        poseStack.pushPose();

        poseStack.translate(moveFactor, 0.0F, 0.0F);

        poseStack.translate(0.14F, 0.0F, -0.50F);

        poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        poseStack.translate(-1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-130.0F));
        poseStack.translate(5.8F, 0F, 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(80.0F));
        poseStack.translate(0.1F, -0.1F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(20.0F));

        mc.getEntityRenderDispatcher().getPlayerRenderer(localPlayer).renderLeftHand(poseStack, buffer, packedLight,
            localPlayer.getSkin().body().texturePath(), localPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), localPlayer);

        poseStack.popPose();
    }

    private static void renderRightPlayerArm(final Minecraft mc,
                                      final LocalPlayer localPlayer,
                                      final PoseStack poseStack,
                                      final SubmitNodeCollector buffer,
                                      final int packedLight,
                                      final float partialTicks,
                                      final float equippedProgress,
                                      final int useTicks,
                                      final int useDuration) {
        final float t = Mth.clamp((useTicks + partialTicks) / 6.0F, 0.0F, 1.0F);
        final float moveFactor = (useTicks != useDuration) ? Mth.lerp(t, 0.0F, -1.0F) : 0.0F;

        poseStack.pushPose();

        poseStack.translate(0.0F, 0.04F + equippedProgress * -1.2F, 0.0F);

        poseStack.translate(0.64F, -0.6F, -0.72F);

        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.translate(-1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));
        poseStack.translate(5.6F, 0.0F, 0.0F);

        poseStack.mulPose(Axis.XP.rotationDegrees(75 * moveFactor));
        poseStack.translate(0.0F, -0.2F * moveFactor + 0.2F, 0.0F);

        poseStack.mulPose(Axis.YP.rotationDegrees(10));

        mc.getEntityRenderDispatcher().getPlayerRenderer(localPlayer).renderRightHand(poseStack, buffer, packedLight,
            localPlayer.getSkin().body().texturePath(), localPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), localPlayer);

        poseStack.popPose();

        // Syringe
        poseStack.translate(0.57F, -0.32F + equippedProgress * -0.6F, -1.22F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-162F));
        poseStack.mulPose(Axis.YP.rotationDegrees(10F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(10F));

        poseStack.translate(moveFactor, 0.25F * moveFactor, 0.2F * moveFactor);

        poseStack.mulPose(Axis.YP.rotationDegrees(110F * moveFactor));
        poseStack.mulPose(Axis.XP.rotationDegrees(20F));
    }
}
