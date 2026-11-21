package com.ultramega.playershells.gui.layer;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.renderer.ShellForgeFluid;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.gui.GuiLayer;

import static com.ultramega.playershells.utils.MathUtils.clamp01;

public final class ShellForgeViewLayer implements GuiLayer {
    @Override
    public void render(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
        final Minecraft client = Minecraft.getInstance();
        final var player = client.player;
        final var camera = client.gameRenderer.getMainCamera();
        if (player == null || camera.isDetached() || client.getCameraEntity() != player) {
            return;
        }

        final var level = player.level();
        final var pos = player.blockPosition();
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ShellForgeBlockEntity)) {
            blockEntity = level.getBlockEntity(pos.below());
        }
        if (!(blockEntity instanceof ShellForgeBlockEntity forge)) {
            return;
        }

        final float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        final float progress = forge.getDrainProgress(partialTick);
        if (progress >= 1f) {
            return;
        }

        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();
        final float relativeEyeHeight = (float) (camera.position().y - forge.getBlockPos().getY());
        final float immersion = clamp01((ShellForgeFluid.surfaceY(forge.getChamberFill(partialTick)) - relativeEyeHeight + 0.12f) / 0.25f);
        if (immersion > 0f) {
            graphics.fillGradient(0, 0, width, height, ARGB.color(Math.round(52f * immersion), 0x237444),
                ARGB.color(Math.round(88f * immersion), 0x13984D));
        }
    }
}
