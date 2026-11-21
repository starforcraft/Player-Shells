package com.ultramega.playershells.blockentities.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ShellForgeFluid {
    private static final SpriteId WATER = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/water_still"));
    private static final float FLOOR = 1.02f / 16f;
    private static final float FULL_LEVEL = 30.5f / 16f;
    // North-facing interior, following the two angled sections of the glass doors.
    private static final float[][] PERIMETER = {
        {1.06f / 16f, 2.7f / 16f}, {5.5f / 16f, 1.1f / 16f}, {10.5f / 16f, 1.1f / 16f},
        {14.94f / 16f, 2.7f / 16f}, {14.94f / 16f, 14.94f / 16f}, {1.06f / 16f, 14.94f / 16f}
    };
    private static final int DEEP_GREEN = 0x7942A859;
    private static final int WATERLINE = 0x845BF082;
    private static final int SURFACE = 0xAF80F6A9;

    private ShellForgeFluid() {
    }

    public static float surfaceY(final float fill) {
        return Mth.lerp(fill, FLOOR, FULL_LEVEL);
    }

    public static void submit(final PoseStack pose, final SubmitNodeCollector nodes, final float fill, final int light) {
        if (fill < 0.001f) {
            return;
        }
        final float top = surfaceY(fill);
        final TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(WATER);
        nodes.submitCustomGeometry(pose, RenderTypes.entityTranslucent(sprite.atlasLocation(), false), (transform, buffer) -> {
            final float topV = sprite.getV(1f - fill);
            for (int i = 0; i < PERIMETER.length; ++i) {
                final float[] from = PERIMETER[i];
                final float[] to = PERIMETER[(i + 1) % PERIMETER.length];
                final float dx = to[0] - from[0];
                final float dz = to[1] - from[1];
                final float length = Mth.sqrt(dx * dx + dz * dz);
                final float nx = dz / length;
                final float nz = -dx / length;
                final float startU = sprite.getU0();
                final float endU = sprite.getU(length);
                final float bottomV = sprite.getV1();
                vertex(transform, buffer, from, FLOOR, startU, bottomV, DEEP_GREEN, light, nx, 0f, nz);
                vertex(transform, buffer, from, top, startU, topV, WATERLINE, light, nx, 0f, nz);
                vertex(transform, buffer, to, top, endU, topV, WATERLINE, light, nx, 0f, nz);
                vertex(transform, buffer, to, FLOOR, endU, bottomV, DEEP_GREEN, light, nx, 0f, nz);
            }
            surfaceQuad(transform, buffer, sprite, top, light, 0, 3, 2, 1);
            surfaceQuad(transform, buffer, sprite, top, light, 0, 5, 4, 3);
        });
    }

    private static void surfaceQuad(final PoseStack.Pose pose, final VertexConsumer buffer, final TextureAtlasSprite sprite,
                                    final float top, final int light, final int a, final int b, final int c, final int d) {
        surfaceVertex(pose, buffer, sprite, top, light, PERIMETER[a]);
        surfaceVertex(pose, buffer, sprite, top, light, PERIMETER[b]);
        surfaceVertex(pose, buffer, sprite, top, light, PERIMETER[c]);
        surfaceVertex(pose, buffer, sprite, top, light, PERIMETER[d]);
    }

    private static void surfaceVertex(final PoseStack.Pose pose, final VertexConsumer buffer, final TextureAtlasSprite sprite,
                                      final float top, final int light, final float[] corner) {
        vertex(pose, buffer, corner, top, sprite.getU(corner[0]), sprite.getV(corner[1]), SURFACE, light, 0f, 1f, 0f);
    }

    private static void vertex(final PoseStack.Pose pose, final VertexConsumer buffer, final float[] corner, final float y,
                               final float u, final float v, final int color, final int light, final float nx, final float ny, final float nz) {
        buffer.addVertex(pose.pose(), corner[0], y, corner[1]).setColor(color).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
