package com.ultramega.playershells.gui.widgets;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ShellButton extends Button {
    private final ShellForgeBlockEntity shellForge;

    private int dotTick = 0;
    private int dotCount = 0;

    public ShellButton(final int x, final int y, final int width, final int height, final OnPress onPress, final ShellForgeBlockEntity shellForge) {
        super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.shellForge = shellForge;
        this.tick();
    }

    @Override
    protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
        // TODO: improve visual
        final Minecraft mc = Minecraft.getInstance();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()),
            this.getX(), this.getY(), this.getWidth(), this.getHeight(), (Mth.ceil(this.alpha * 255.0F) << 24) | 0xff0000);

        // Render label
        final String text = this.getMessage().getString();
        final int color = this.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24;

        if (this.shellForge.getShellState() == ShellStates.CREATING || this.shellForge.getShellState() == ShellStates.EXTERMINATING) {
            final int x = this.getX() + (this.getWidth() - mc.font.width(text.replace(".", ""))) / 2;
            final int y = this.getY() + (this.getHeight() - mc.font.lineHeight) / 2 + 1;
            graphics.text(mc.font, text, x, y, color, true);
        } else {
            graphics.centeredText(mc.font, this.getMessage(), this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - mc.font.lineHeight) / 2 + 1, color);
        }

    }

    public void setMessage(final ShellStates state) {
        final String additional = (state == ShellStates.CREATING || state == ShellStates.EXTERMINATING) ? ".".repeat(this.dotCount) : "";
        super.setMessage(Component.translatable("gui.playershells.shell_forge." + state.name().toLowerCase(Locale.ROOT)).append(additional));
    }

    public void tick() {
        final ShellStates state = this.shellForge.getShellState();
        if (state == ShellStates.CREATE) {
            this.active = this.shellForge.canCreateShell();
            this.resetDots();
        } else if (state == ShellStates.CREATING || state == ShellStates.EXTERMINATING) {
            this.active = false;
            // Tick dots
            if (++this.dotTick >= 10) {
                this.dotTick = 0;
                this.dotCount = (this.dotCount + 1) % 4;
            }
        } else {
            this.active = state == ShellStates.EXTERMINATE;
            this.resetDots();
        }
        this.setMessage(state);
    }

    private void resetDots() {
        this.dotTick = 0;
        this.dotCount = 0;
    }
}
