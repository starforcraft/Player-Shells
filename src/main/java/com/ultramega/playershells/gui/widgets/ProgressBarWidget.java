package com.ultramega.playershells.gui.widgets;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static com.ultramega.playershells.PlayerShells.MODID;

public class ProgressBarWidget extends AbstractWidget {
    private static final Identifier GREEN_SPRITE = Identifier.fromNamespaceAndPath(MODID, "widget/green_progress_bar");
    private static final Identifier RED_SPRITE = Identifier.fromNamespaceAndPath(MODID, "widget/red_progress_bar");
    private static final Identifier SPRITE_BACKGROUND = Identifier.fromNamespaceAndPath(MODID, "widget/progress_bar_background");

    private final ShellForgeBlockEntity shellForge;
    private final IntSupplier progressSupplier;

    public ProgressBarWidget(final int x, final int y, final int width, final int height, final ShellForgeBlockEntity shellForge, final IntSupplier progressSupplier) {
        super(x, y, width, height, Component.empty());
        this.shellForge = shellForge;
        this.progressSupplier = progressSupplier;
        this.tick();
    }

    @Override
    protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE_BACKGROUND, 82, 18, 0, 0, this.getX() - 1, this.getY() - 1, 82, 18);

        final Minecraft mc = Minecraft.getInstance();
        final int progress = this.progressSupplier.getAsInt();

        final int filledWidth = (int) ((progress / 100.0) * this.getWidth());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, (this.shellForge.getShellState() == ShellStates.EXTERMINATING || this.shellForge.getShellState() == ShellStates.DECAYING) ? RED_SPRITE : GREEN_SPRITE, 80, 16, 0, 0,
            this.getX(), this.getY(), filledWidth, this.getHeight());

        final String progressText = progress + "%";
        final int x = this.getX() + (this.getWidth() - mc.font.width(progressText)) / 2;
        final int y = this.getY() + (this.getHeight() - mc.font.lineHeight) / 2 + 1;
        graphics.text(mc.font, progressText, x, y, 0xFFFFFFFF);
    }

    public void tick() {
        final ShellStates state = this.shellForge.getShellState();
        this.visible = state != ShellStates.CREATE && state != ShellStates.EXTERMINATE;
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }

    @Override
    public void playDownSound(final SoundManager handler) {
    }
}
