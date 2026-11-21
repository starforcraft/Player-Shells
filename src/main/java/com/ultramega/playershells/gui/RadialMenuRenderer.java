package com.ultramega.playershells.gui;

import com.ultramega.playershells.entities.ShellEntity;
import com.ultramega.playershells.entities.renderer.ShellRenderer;
import com.ultramega.playershells.mixin.GuiGraphicsAccessor;
import com.ultramega.playershells.utils.MathUtils;

import java.util.List;
import javax.annotation.Nullable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * This is heavily inspired by <a href="https://github.com/MatyrobbrtMods/KeyBindBundles/blob/main/src/main/java/com/matyrobbrt/keybindbundles/render/RadialMenuRenderer.java">KeybindBundles</a>
 */
public abstract class RadialMenuRenderer<T> {
    public static final float INNER = 40;
    public static final float OUTER = 100;
    public static final float MIDDLE_DISTANCE = (INNER + OUTER) / 2F;

    private static final float DRAWS = 300;
    private static final int MAX_LABEL_WIDTH = 72;

    protected int lastIndexUnderMouse = -1;

    private int[] hoverGrows = new int[0];
    private long lastUpdate = System.currentTimeMillis();

    public abstract List<T> getEntries();

    public abstract Component getTitle(T entry);

    @Nullable
    public abstract ShellEntity getPlayerShell(T entry);

    public abstract int getPlayerShellCreationProgress(T entry);

    public void render(final GuiGraphicsExtractor graphics) {
        final List<T> entries = this.getEntries();
        if (entries.isEmpty()) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.releaseMouse();
        if (this.hoverGrows.length != entries.size()) {
            this.hoverGrows = new int[entries.size()];
        }
        if (!mc.isPaused() && !(mc.screen instanceof ChatScreen)) {
            this.lastIndexUnderMouse = this.getElementUnderMouse();
        }

        final int count = entries.size();
        final float angleSize = 360f / count;
        final int centerX = graphics.guiWidth() / 2;
        final int centerY = graphics.guiHeight() / 2;
        final long now = System.currentTimeMillis();
        final boolean advanceHover = now - this.lastUpdate >= 25;
        if (advanceHover) {
            this.lastUpdate = now;
        }

        final var guiState = ((GuiGraphicsAccessor) graphics).playershells$renderState();
        for (int i = 0; i < count; i++) {
            final boolean highlight = this.lastIndexUnderMouse == i;
            if (advanceHover) {
                this.hoverGrows[i] = Mth.clamp(this.hoverGrows[i] + (highlight ? 1 : -1), 0, 10);
            }
            final float start = -90f + angleSize * (i - 0.5f);
            final float radius = OUTER + this.hoverGrows[i];
            guiState.addGuiElement(new RingSegment(new Matrix3x2f().translation(centerX, centerY), start, angleSize, radius,
                highlight ? 0xd94d4d4d : 0x994d4d4d, new ScreenRectangle(centerX - 110, centerY - 110, 220, 220)));
        }

        graphics.nextStratum();
        Component hoveredTitle = null;
        for (int i = 0; i < count; i++) {
            final T entry = entries.get(i);
            final ShellEntity entity = this.getPlayerShell(entry);
            if (entity == null) {
                continue;
            }
            final float angle = (-90f + i * angleSize) * Mth.DEG_TO_RAD;
            final int x = centerX + Math.round(Mth.cos(angle) * MIDDLE_DISTANCE);
            final int y = centerY + Math.round(Mth.sin(angle) * MIDDLE_DISTANCE);
            final int progress = this.getPlayerShellCreationProgress(entry);
            final var state = ShellRenderer.extract(entity, 205, progress, 0x36bcb8, 15728880, progress != 100, 1f);
            graphics.entity(state, 20, new Vector3f(0, state.boundingBoxHeight / 2f, 0),
                new Quaternionf().rotateZ((float) Math.PI), null, x - 14, y - 20, x + 14, y + 20);
            if (progress != 100) {
                graphics.centeredText(mc.font, Component.literal(progress + "%"), x, y - 20, 0xccffffff);
            }
            final Component title = this.getTitle(entry);
            if (mc.font.width(title) > MAX_LABEL_WIDTH) {
                final String shortened = mc.font.plainSubstrByWidth(title.getString(), MAX_LABEL_WIDTH - mc.font.width("…")) + "…";
                graphics.centeredText(mc.font, shortened, x, y + 14, 0xccffffff);
                if (i == this.lastIndexUnderMouse) {
                    hoveredTitle = title;
                }
            } else {
                graphics.centeredText(mc.font, title, x, y + 14, 0xccffffff);
            }
        }
        if (hoveredTitle != null) {
            final MousePos mouse = getMousePos();
            final var lines = mc.font.split(hoveredTitle, 120).stream().map(ClientTooltipComponent::create).toList();
            graphics.nextStratum();
            graphics.tooltip(mc.font, lines, (int) mouse.x(), (int) mouse.y(), DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    public static MousePos getMousePos() {
        final Minecraft mc = Minecraft.getInstance();
        final MouseHandler mouse = mc.mouseHandler;
        final double mouseX = mouse.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        final double mouseY = mouse.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        return new MousePos(mouseX, mouseY);
    }

    public static MousePos getDiffFromCenter(final double mouseX, final double mouseY) {
        final Window window = Minecraft.getInstance().getWindow();
        final int centerX = window.getGuiScaledWidth() / 2;
        final int centerY = window.getGuiScaledHeight() / 2;

        final double xDiff = mouseX - centerX;
        final double yDiff = mouseY - centerY;
        return new MousePos(xDiff, yDiff);
    }

    public int getElementUnderMouse() {
        final MousePos mouse = getMousePos();
        return this.getElementUnderMouse(getDiffFromCenter(mouse.x(), mouse.y()));
    }

    protected int getElementUnderMouse(final MousePos diffFromCenter) {
        final int count = this.getEntries().size();
        final double distanceFromCenter = Mth.length(diffFromCenter.x(), diffFromCenter.y());
        if (count > 0 && distanceFromCenter >= INNER) {
            // draw mouse selection highlight
            final float angle = (float) (Mth.RAD_TO_DEG * Mth.atan2(diffFromCenter.y(), diffFromCenter.x()));
            final float modeSize = 180F / count;

            final float selectionAngle = MathUtils.wrapDegrees(angle + modeSize + 90F);
            return (int) (selectionAngle * (count / 360F));
        }
        return -1;
    }

    public void clearState() {
        this.hoverGrows = new int[0];
        this.lastIndexUnderMouse = -1;
    }

    private record RingSegment(Matrix3x2f pose, float start, float angle, float outer, int color,
                               ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        @Nullable
        public ScreenRectangle scissorArea() {
            return null;
        }

        @Override
        public void buildVertices(final VertexConsumer vertices) {
            final int segments = Math.max(1, Mth.ceil(angle * DRAWS / 360f));
            for (int i = 0; i < segments; i++) {
                final float from = (start + angle * i / segments) * Mth.DEG_TO_RAD;
                final float to = (start + angle * (i + 1) / segments) * Mth.DEG_TO_RAD;
                vertices.addVertexWith2DPose(pose, INNER * Mth.cos(from), INNER * Mth.sin(from)).setColor(color);
                vertices.addVertexWith2DPose(pose, INNER * Mth.cos(to), INNER * Mth.sin(to)).setColor(color);
                vertices.addVertexWith2DPose(pose, outer * Mth.cos(to), outer * Mth.sin(to)).setColor(color);
                vertices.addVertexWith2DPose(pose, outer * Mth.cos(from), outer * Mth.sin(from)).setColor(color);
            }
        }
    }

    public record MousePos(double x, double y) {
    }
}
