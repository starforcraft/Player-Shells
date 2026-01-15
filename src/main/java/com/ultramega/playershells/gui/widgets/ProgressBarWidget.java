package com.ultramega.playershells.gui.widgets;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static com.ultramega.playershells.PlayerShells.MODID;

public class ProgressBarWidget extends AbstractWidget {
    private static final ResourceLocation GREEN_SPRITE = new ResourceLocation(MODID, "textures/gui/sprites/widget/green_progress_bar.png");
    private static final ResourceLocation RED_SPRITE = new ResourceLocation(MODID, "textures/gui/sprites/widget/red_progress_bar.png");
    private static final ResourceLocation SPRITE_BACKGROUND = new ResourceLocation(MODID, "textures/gui/sprites/widget/progress_bar_background.png");

    private final ShellForgeBlockEntity shellForge;
    private final IntSupplier progressSupplier;

    public ProgressBarWidget(final int x, final int y, final int width, final int height, final ShellForgeBlockEntity shellForge, final IntSupplier progressSupplier) {
        super(x, y, width, height, Component.empty());
        this.shellForge = shellForge;
        this.progressSupplier = progressSupplier;
        this.tick();
    }

    @Override
    protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(SPRITE_BACKGROUND, this.getX() - 1, this.getY() - 1, 0, 0, 82, 18, 82, 18);

        final Minecraft mc = Minecraft.getInstance();
        final int progress = this.progressSupplier.getAsInt();

        final int filledWidth = (int) ((progress / 100.0) * this.getWidth());
        graphics.blit(this.shellForge.getShellState() == ShellStates.EXTERMINATING ? RED_SPRITE : GREEN_SPRITE,
            this.getX(), this.getY(), 0, 0, filledWidth, this.getHeight(), 80, 16);

        final String progressText = progress + "%";
        final int x = this.getX() + (this.getWidth() - mc.font.width(progressText)) / 2;
        final int y = this.getY() + (this.getHeight() - mc.font.lineHeight) / 2 + 1;
        graphics.drawString(mc.font, progressText, x, y, 0xFFFFFFFF);
    }

    public void tick() {
        this.visible = this.shellForge.getShellState() == ShellStates.CREATING || this.shellForge.getShellState() == ShellStates.EXTERMINATING;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }

    @Override
    public void playDownSound(final SoundManager handler) {
    }
}
