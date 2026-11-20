package com.ultramega.playershells.blockentities.renderer;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.entities.renderer.ShellRenderer;
import com.ultramega.playershells.events.ClientEvents;
import com.ultramega.playershells.storage.ClientShellData;

import java.util.List;
import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.ultramega.playershells.blocks.AbstractMultiblockBlock.FACING;
import static com.ultramega.playershells.blocks.AbstractMultiblockBlock.isBottomHalf;
import static com.ultramega.playershells.utils.MathUtils.interpolateGradient;
import static com.ultramega.playershells.utils.MathUtils.yawForDirection;

public class ShellForgeBlockEntityRenderer implements BlockEntityRenderer<ShellForgeBlockEntity, ShellForgeBlockEntityRenderer.State> {
    private static final int FLAME_STRAY_COUNT = 50;
    private static final float FLAME_STRAY_WIDTH = 0.03f;
    private static final float FLAME_RADIUS = 0.25f;

    private static final int[] YELLOW = {247, 223, 37};
    private static final int[] ORANGE = {255, 140, 0};
    private static final int[] DARK_ORANGE = {153, 84, 1};

    public ShellForgeBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final ShellForgeBlockEntity forge, final State state, final float partialTick,
        final Vec3 cameraPosition, final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(forge, state, partialTick, cameraPosition, breakProgress);
        state.bottom = isBottomHalf(forge.getBlockState());
        state.facing = forge.getBlockState().getValue(FACING);
        state.doorAngle = Mth.lerp(partialTick, forge.getAnimPrevProgress(), forge.getAnimProgress()) * 60.0f;
        state.shell = null;
        final ShellStates shellState = forge.getShellState();
        state.exterminating = shellState == ShellStates.EXTERMINATING;
        if (state.bottom && forge.getLevel() instanceof ClientLevel level) {
            final var entity = ClientShellData.INSTANCE.getPreview(level, forge.getPlayerUuid(), forge.getShellUuid());
            if (entity != null) {
                entity.setPos(forge.getBlockPos().getCenter().add(0, -0.4375, 0));
                final boolean creating = shellState == ShellStates.CREATING;
                final boolean scanActive = creating || state.exterminating || shellState == ShellStates.DECAYING;
                final int edgeColor = creating ? 0x36bcb8 : 0x353535;
                state.shell = ShellRenderer.extract(entity, yawForDirection(state.facing), forge.getShellPercentage(), edgeColor,
                    state.lightCoords, scanActive);
            }
            state.flameSeed = level.getRandom().nextLong();
        }
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector nodes, final CameraRenderState camera) {
        if (!state.bottom) {
            return;
        }

        final Minecraft client = Minecraft.getInstance();
        final var models = client.getModelManager();
        final float yaw = switch (state.facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
        poseStack.pushPose();
        poseStack.rotateAround(Axis.YP.rotationDegrees(yaw), 0.5f, 0, 0.5f);
        this.submitDoor(poseStack, nodes, state.lightCoords, models.getStandaloneModel(ClientEvents.GLASS_RIGHT), 14.8f / 16f, -state.doorAngle);
        this.submitDoor(poseStack, nodes, state.lightCoords, models.getStandaloneModel(ClientEvents.GLASS_LEFT), 1.1f / 16f, state.doorAngle);
        poseStack.popPose();

        if (state.exterminating) {
            final long seed = state.flameSeed;
            nodes.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, buffer) -> this.renderSmallFlame(pose, buffer, RandomSource.create(seed)));
        }
        if (state.shell != null) {
            client.getEntityRenderDispatcher().submit(state.shell, camera, 0.5, 0.0625, 0.5, poseStack, nodes);
        }
    }

    private void submitDoor(final PoseStack pose, final SubmitNodeCollector nodes, final int light,
        final BlockStateModelPart part, final float pivot, final float angle) {
        pose.pushPose();
        pose.rotateAround(Axis.YP.rotationDegrees(angle), pivot, 0, 1.6f / 16f);
        nodes.submitBlockModel(pose, Sheets.cutoutBlockSheet(), List.of(part), new int[0], light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    private void renderSmallFlame(final PoseStack.Pose pose, final VertexConsumer vertexConsumer, final RandomSource random) {

        final PoseStack poseStack = new PoseStack();
        poseStack.mulPose(pose.pose());
        for (int i = 0; i < FLAME_STRAY_COUNT; i++) {
            final float height = 1.6f + (random.nextFloat() - 0.5f) * 0.7f;
            final float startY = -height + 2.0F;

            final float angleRad = (float) (((float) i / FLAME_STRAY_COUNT) * (2 * Math.PI));
            final float baseX = 0.5f + (float) Math.cos(angleRad) * FLAME_RADIUS;
            final float baseZ = 0.5f + (float) Math.sin(angleRad) * FLAME_RADIUS;

            final float angle = random.nextFloat() * 360f;

            poseStack.pushPose();

            // Rotate around center of quad
            poseStack.translate(baseX, startY + height / 2f, baseZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(-baseX, -(startY + height / 2f), -baseZ);

            final Matrix4f matrix = poseStack.last().pose();

            final int steps = 10;
            for (int j = 0; j < steps; j++) {
                final float y1 = startY + (j / (float) steps) * height;
                final float y2 = startY + ((j + 1) / (float) steps) * height;

                final float t1 = (y1 - startY) / height;
                final float t2 = (y2 - startY) / height;

                final float widthFactor1 = (float) Math.sin(t1 * Math.PI);
                final float widthFactor2 = (float) Math.sin(t2 * Math.PI);

                final float w1 = FLAME_STRAY_WIDTH + 0.25f * widthFactor1;
                final float w2 = FLAME_STRAY_WIDTH + 0.25f * widthFactor2;

                // Calculate colors
                final float midT = j / (float) steps;
                final float midT2 = (j + 1) / (float) steps;

                final int[] color1 = interpolateGradient(midT, DARK_ORANGE, ORANGE, YELLOW);
                final int[] color2 = interpolateGradient(midT2, DARK_ORANGE, ORANGE, YELLOW);

                // Build vertices
                vertexConsumer.addVertex(matrix, baseX - w1 / 2, y1, baseZ)
                    .setColor(color1[0], color1[1], color1[2], 200);
                vertexConsumer.addVertex(matrix, baseX - w2 / 2, y2, baseZ)
                    .setColor(color2[0], color2[1], color2[2], 200);
                vertexConsumer.addVertex(matrix, baseX + w2 / 2, y2, baseZ)
                    .setColor(color2[0], color2[1], color2[2], 200);
                vertexConsumer.addVertex(matrix, baseX + w1 / 2, y1, baseZ)
                    .setColor(color1[0], color1[1], color1[2], 200);
            }

            poseStack.popPose();
        }
    }


    @Override
    public AABB getRenderBoundingBox(final ShellForgeBlockEntity forge) {
        return new AABB(forge.getBlockPos()).expandTowards(0, isBottomHalf(forge.getBlockState()) ? 1 : -1, 0);
    }

    public static class State extends BlockEntityRenderState {
        private boolean bottom;
        private Direction facing;
        private float doorAngle;
        private boolean exterminating;
        private long flameSeed;
        @Nullable
        private ShellRenderer.State shell;
    }
}
