package com.ultramega.playershells.gui.layer;

import com.ultramega.playershells.registry.ModItems;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.items.SyringeItem.MAX_EXTRACT_DURATION;
import static com.ultramega.playershells.items.SyringeItem.PULSE_POINTS;
import static com.ultramega.playershells.utils.MathUtils.smoothstep01;

public class BloodLayer implements GuiLayer {
    private static final Identifier UNDERWATER_LOCATION = Identifier.fromNamespaceAndPath(MODID, "textures/gui/blood_overlay.png");

    private static final float PULSE_WIDTH = 0.02f;
    private static final float MAX_ALPHA = 0.4f;

    @Override
    public void render(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;

        if (player == null) {
            return;
        }

        if (!player.isHolding(ModItems.EMPTY_SYRINGE.get())) {
            return;
        }

        final float usePos = (float) (player.getMainHandItem().getUseDuration(player) - player.getUseItemRemainingTicks()) / MAX_EXTRACT_DURATION;
        final float alpha = pulseAlpha(usePos);
        if (alpha > 0.01f) {
            final int tint = (Math.round(alpha * 255) << 24) | 0xffffff;
            graphics.blit(RenderPipelines.GUI_TEXTURED, UNDERWATER_LOCATION, 0, 0, 0, 0, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight(), tint);
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
