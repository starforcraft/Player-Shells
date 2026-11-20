package com.ultramega.playershells.entities.renderer;

import com.ultramega.playershells.entities.ShellEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.PlayerModelType;

public class ShellRenderer extends AvatarRenderer<ShellEntity> {
    private static ShellRenderer wide;
    private static ShellRenderer slim;

    public ShellRenderer(final Context context, final boolean useSlimModel) {
        super(context, useSlimModel);
    }

    public static void reload(final Context context) {
        wide = new ShellRenderer(context, false);
        slim = new ShellRenderer(context, true);
    }

    public static ShellRenderer get(final PlayerModelType model) {
        return model == PlayerModelType.SLIM ? slim : wide;
    }

    public static State extract(final ShellEntity shell, final float yaw, final int progress, final int edgeColor, final int light,
                                final boolean scanActive) {
        shell.setYRot(yaw);
        shell.yBodyRot = yaw;
        shell.yBodyRotO = yaw;
        shell.setYHeadRot(yaw);
        shell.yHeadRotO = yaw;
        shell.setXRot(0);
        final State state = (State) get(shell.getSkin().model()).createRenderState(shell, 1.0F);
        state.progress = progress;
        state.edgeColor = edgeColor;
        state.scanActive = scanActive;
        state.lightCoords = light;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.displayFireAnimation = false;
        return state;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void submit(final AvatarRenderState state, final PoseStack poseStack, final SubmitNodeCollector nodes, final CameraRenderState camera) {
        final State shell = (State) state;
        if (shell.scanActive) {
            ShellScan.submit(poseStack.last().pose(), shell.boundingBoxHeight, shell.progress, shell.edgeColor,
                () -> super.submit(state, poseStack, nodes, camera));
        } else {
            super.submit(state, poseStack, nodes, camera);
        }
    }

    @Override
    protected void submitNameDisplay(final AvatarRenderState state, final PoseStack poseStack,
        final SubmitNodeCollector nodes, final CameraRenderState camera) {
    }

    public static class State extends AvatarRenderState {
        private int progress;
        private int edgeColor;
        private boolean scanActive;
    }
}
