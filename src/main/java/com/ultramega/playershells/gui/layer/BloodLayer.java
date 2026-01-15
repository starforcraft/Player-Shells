package com.ultramega.playershells.gui.layer;

import com.ultramega.playershells.registry.ModItems;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.items.SyringeItem.MAX_EXTRACT_DURATION;
import static com.ultramega.playershells.items.SyringeItem.PULSE_POINTS;
import static com.ultramega.playershells.utils.MathUtils.smoothstep01;

public class BloodLayer implements IGuiOverlay {
    private static final ResourceLocation UNDERWATER_LOCATION = new ResourceLocation(MODID, "textures/gui/blood_overlay.png");

    private static final float PULSE_WIDTH = 0.02f;
    private static final float MAX_ALPHA = 0.4f;

    @Override
    public void render(final ForgeGui gui, final GuiGraphics graphics, final float partialTick, final int screenWidth, final int screenHeight) {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;

        if (player == null) {
            return;
        }

        if (!player.isHolding(ModItems.EMPTY_SYRINGE.get())) {
            return;
        }

        final float usePos = (float) (player.getMainHandItem().getUseDuration() - player.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
        final float alpha = pulseAlpha(usePos);
        if (alpha > 0.01f) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            graphics.blit(UNDERWATER_LOCATION, 0, 0, 0, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static float pulseAlpha(final float usePos) {
        float alphaFactor = 0f;
        for (final float point : PULSE_POINTS) {
            final float d = Math.abs(usePos - point);
            if (d < PULSE_WIDTH) {
                final float t = 1f - (d / PULSE_WIDTH);
                final float smooth = smoothstep01(t);
                alphaFactor = Math.max(alphaFactor, smooth);
            }
        }
        return alphaFactor * MAX_ALPHA;
    }
}
