package com.ultramega.playershells.gui;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.container.ShellForgeContainerMenu;
import com.ultramega.playershells.gui.widgets.ProgressBarWidget;
import com.ultramega.playershells.gui.widgets.ShellButton;
import com.ultramega.playershells.packet.c2s.ShellButtonPressedPacket;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2fStack;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.utils.MathUtils.EMPTY_UUID;

public class ShellForgeScreen extends AbstractContainerScreen<ShellForgeContainerMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(MODID, "textures/gui/shell_forge.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final float STATUS_TEXT_SCALE = 0.5F;
    private static final long EXTERMINATION_CONFIRMATION_DELAY_MS = 500L;

    private final Set<Integer> heldSelectionKeys = new HashSet<>();
    @Nullable
    private UUID pendingExtermination;
    private long confirmationReadyAt;

    @Nullable
    private ShellButton shellButton;
    @Nullable
    private ProgressBarWidget progressBar;

    @Nullable
    private Component randomStatusSubText;
    @Nullable
    private ShellStates lastShellState;

    public ShellForgeScreen(final ShellForgeContainerMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        this.randomStatusSubText = this.getRandomStatusSubText();
    }

    @Override
    protected void init() {
        super.init();

        this.shellButton = new ShellButton(this.leftPos + (this.imageWidth - 80) / 2, this.topPos + 55, 80, 16,
            button -> this.pressShellButton(), this.getMenu().getBlockEntity());
        this.addRenderableWidget(this.shellButton);

        this.progressBar = new ProgressBarWidget(this.leftPos + (this.imageWidth - 80) / 2, this.topPos + 25, 80, 16, this.getMenu().getBlockEntity(),
            () -> this.getMenu().getBlockEntity().getShellPercentage());
        this.addRenderableWidget(this.progressBar);

        this.containerTick();
    }

    private void pressShellButton() {
        final var forge = this.menu.getBlockEntity();
        final BlockPos position = forge.getBlockPos();
        if (forge.getShellState() == ShellStates.CREATE) {
            ClientPacketDistributor.sendToServer(new ShellButtonPressedPacket(position, EMPTY_UUID));
        } else if (forge.getShellState() == ShellStates.EXTERMINATE) {
            final UUID shellUuid = forge.getShellUuid();
            final long now = Util.getMillis();
            if (!shellUuid.equals(this.pendingExtermination)) {
                this.pendingExtermination = shellUuid;
                this.confirmationReadyAt = now + EXTERMINATION_CONFIRMATION_DELAY_MS;
                this.updateConfirmationButton();
            } else if (now >= this.confirmationReadyAt) {
                this.pendingExtermination = null;
                ClientPacketDistributor.sendToServer(new ShellButtonPressedPacket(position, shellUuid));
            }
        }
    }

    private void updateConfirmationButton() {
        if (this.shellButton != null && this.pendingExtermination != null) {
            this.shellButton.active = Util.getMillis() >= this.confirmationReadyAt;
            this.shellButton.setMessage(Component.translatable("gui.playershells.shell_forge.confirm_exterminate"));
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isSelection() && !this.heldSelectionKeys.add(event.key()) && this.getFocused() == this.shellButton) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        this.heldSelectionKeys.remove(event.key());
        return super.keyReleased(event);
    }

    @Override
    protected void containerTick() {
        final var forge = this.menu.getBlockEntity();
        final ShellStates state = forge.getShellState();
        if (state != ShellStates.EXTERMINATE || !forge.getShellUuid().equals(this.pendingExtermination)) {
            this.pendingExtermination = null;
        }
        if (this.shellButton != null) {
            this.shellButton.tick();
            this.updateConfirmationButton();
        }
        if (this.progressBar != null) {
            this.progressBar.tick();
        }
        // Check if shell state has updated
        if (this.lastShellState != state) {
            this.randomStatusSubText = this.getRandomStatusSubText();
        }
        this.lastShellState = state;
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        if (this.getMenu().getBlockEntity().getShellState() == ShellStates.CREATE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, this.leftPos + 79, this.topPos + 16, 18, 18);
        }

        final Matrix3x2fStack poseStack = graphics.pose();

        if (this.pendingExtermination != null) {
            poseStack.pushMatrix();
            poseStack.translate(this.leftPos + this.imageWidth / 2f, this.topPos + 30);
            poseStack.scale(1.25f);
            graphics.centeredText(this.font, Component.translatable("gui.playershells.shell_forge.exterminate_warning"), 0, 0, 0xFFFF4040);
            poseStack.popMatrix();
        }

        if (this.getMenu().getBlockEntity().getShellState() != ShellStates.CREATE) {
            if (this.randomStatusSubText != null) {
                final Component status = Component.translatable("gui.playershells.shell_forge.status", this.randomStatusSubText);
                final int x = (int) (this.leftPos + (this.imageWidth - this.font.width(status.getString()) * STATUS_TEXT_SCALE) / 2);
                final int y = this.topPos + 47;

                poseStack.pushMatrix();
                poseStack.translate(x, y);
                poseStack.scale(STATUS_TEXT_SCALE, STATUS_TEXT_SCALE);
                graphics.text(this.font, status.getString(), 0, 0, 0xFFFFFFFF);
                poseStack.popMatrix();
            }
        }

        // Draw energy bar
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 176, 0, 6, ENERGY_BAR_HEIGHT, 256, 256);
        final long energyAmount = this.getMenu().getBlockEntity().energyStorage.getAmountAsLong();
        final long energyMaxStored = this.getMenu().getBlockEntity().energyStorage.getCapacityAsLong();
        final int energyLevel = (int) (energyAmount * (ENERGY_BAR_HEIGHT / (float) energyMaxStored));
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos + 8, this.topPos + 18, 8, 18, 6, ENERGY_BAR_HEIGHT - energyLevel, 256, 256);
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

    @Nullable
    private Component getRandomStatusSubText() {
        final Level level = this.getMenu().getBlockEntity().getLevel();
        if (level == null) {
            return null;
        }
        final RandomSource random = level.getRandom();

        // 70% chance to show the main/correct status
        final boolean useMainText = random.nextDouble() < 0.7;
        return switch (this.getMenu().getBlockEntity().getShellState()) {
            case CREATE -> null;
            case CREATING -> useMainText
                ? Component.translatable("gui.playershells.shell_forge.status.creating")
                : Component.translatable("gui.playershells.shell_forge.status.creating_" + random.nextInt(0, 5));
            case RECOVERING -> Component.translatable("gui.playershells.shell_forge.status.recovering");
            case EXTERMINATE -> useMainText
                ? Component.translatable("gui.playershells.shell_forge.status.alive")
                : Component.translatable("gui.playershells.shell_forge.status.alive_" + random.nextInt(0, 7));
            case EXTERMINATING -> useMainText
                ? Component.translatable("gui.playershells.shell_forge.status.exterminating")
                : Component.translatable("gui.playershells.shell_forge.status.exterminating_" + random.nextInt(0, 2));
            case DECAYING -> useMainText
                ? Component.translatable("gui.playershells.shell_forge.status.decaying")
                : Component.translatable("gui.playershells.shell_forge.status.decaying_" + random.nextInt(0, 3));
        };
    }
}
