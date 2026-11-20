package com.ultramega.playershells.gui;

import com.ultramega.playershells.container.CentrifugeContainerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import static com.ultramega.playershells.PlayerShells.MODID;

public class CentrifugeScreen extends AbstractContainerScreen<CentrifugeContainerMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(MODID, "textures/gui/centrifuge.png");
    private static final Identifier PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int ENERGY_BAR_HEIGHT = 52;

    public CentrifugeScreen(final CentrifugeContainerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Draw energy bar
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT, 256, 256);
        final long energyAmount = this.getMenu().getBlockEntity().energyStorage.getAmountAsLong();
        final long energyMaxStored = this.getMenu().getBlockEntity().energyStorage.getCapacityAsLong();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) energyMaxStored));
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel, 256, 256);

        // Draw progress
        final int width = Mth.ceil((this.menu.getProcessingProgress() * 24.0F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_SPRITE, 24, 16, 0, 0, this.leftPos + 79, this.topPos + 34, width, 16);
        super.extractContents(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (this.isHovering(7, 18, 8, ENERGY_BAR_HEIGHT + 1, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.playershells.energy",
                this.getMenu().getBlockEntity().energyStorage.getAmountAsLong()), mouseX, mouseY);
        }
    }
}
