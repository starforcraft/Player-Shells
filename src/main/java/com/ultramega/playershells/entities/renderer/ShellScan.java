package com.ultramega.playershells.entities.renderer;

import com.ultramega.playershells.registry.ModRenderTypes;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class ShellScan {
    private static final ThreadLocal<ShellScan> ACTIVE = new ThreadLocal<>();
    private static final float EDGE_WIDTH = 0.05F;

    private final Map<RenderType, RenderType> renderTypes = new IdentityHashMap<>();
    private final Matrix4f parameters;

    private ShellScan(final Matrix4fc entityPose, final float height, final int progress, final int color) {
        // Position is already transformed by the submit pose. Express the clip plane
        // in those coordinates, so the same shader also works in the flipped GUI pose.
        final Vector4f plane = new Matrix4f(entityPose).invertAffine().getRow(1, new Vector4f());
        plane.w -= height * (progress / 100.0F);

        // RenderType supplies TextureMat per draw. This pipeline uses it for scan parameters.
        final Vector4f edge = new Vector4f((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, EDGE_WIDTH);
        this.parameters = new Matrix4f().setColumn(0, plane).setColumn(1, edge);
    }

    public static void submit(final Matrix4fc entityPose, final float height, final int progress, final int color, final Runnable submit) {
        final ShellScan previous = ACTIVE.get();
        ACTIVE.set(new ShellScan(entityPose, height, progress, color));
        try {
            submit.run();
        } finally {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }

    public static RenderType renderType(final RenderType original) {
        final ShellScan scan = ACTIVE.get();
        return scan == null ? original : scan.renderTypes.computeIfAbsent(original, type -> ModRenderTypes.createShell(type, scan.parameters));
    }

    public static boolean isActive() {
        return ACTIVE.get() != null;
    }
}
