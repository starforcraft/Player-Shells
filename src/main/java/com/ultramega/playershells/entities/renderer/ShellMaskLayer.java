package com.ultramega.playershells.entities.renderer;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.utils.MathUtils.smoothstep01;

public final class ShellMaskLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final Identifier MODEL_LOCATION = Identifier.fromNamespaceAndPath(MODID, "block/shell_breathing_mask");
    public static final StandaloneModelKey<BlockStateModelPart> MODEL = new StandaloneModelKey<>(MODEL_LOCATION::toString);
    private static final float HEAD_SCALE = 0.9375f;

    public ShellMaskLayer(final ShellRenderer renderer) {
        super(renderer);
    }

    @Override
    public void submit(final PoseStack pose, final SubmitNodeCollector nodes, final int light, final AvatarRenderState state,
                       final float yaw, final float pitch) {
        if (!(state instanceof ShellRenderer.State shell) || shell.maskSeparation >= 1f || shell.isInvisible) {
            return;
        }
        pose.pushPose();
        this.getParentModel().root().translateAndRotate(pose);
        this.getParentModel().translateToHead(pose);
        submitMask(pose, nodes, light, shell.maskSeparation);
        pose.popPose();
    }

    public static void submitReleased(final PoseStack pose, final SubmitNodeCollector nodes, final int light, final float progress) {
        if (progress >= 1f) {
            return;
        }
        pose.pushPose();
        // Match the shell's head transform at the center of the Shell Forge
        pose.translate(0.5, 0.0625 + 1.501 * HEAD_SCALE, 0.5);
        pose.scale(-HEAD_SCALE, -HEAD_SCALE, HEAD_SCALE);
        submitMask(pose, nodes, light, progress);
        pose.popPose();
    }

    private static void submitMask(final PoseStack pose, final SubmitNodeCollector nodes, final int light, final float separation) {
        final var model = Minecraft.getInstance().getModelManager().getStandaloneModel(MODEL);
        if (model == null) {
            return;
        }
        final float rise = smoothstep01(separation);
        final float forward = smoothstep01(separation / 0.5f);
        pose.translate(0f, -0.22f * rise / HEAD_SCALE, -0.025f * forward / HEAD_SCALE);
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        pose.translate(-0.5f, -0.5f, -0.5f);
        final int tint = ARGB.color(Math.round(255f * (1f - smoothstep01((separation - 0.60f) / 0.40f))), 0xFFFFFF);
        final var renderType = separation <= 0f ? Sheets.cutoutBlockSheet() : RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS, false);
        nodes.submitBlockModel(pose, renderType, List.of(model), new int[]{tint}, light, OverlayTexture.NO_OVERLAY, 0);
    }
}
