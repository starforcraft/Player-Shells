package com.ultramega.playershells.mixin;

import com.ultramega.playershells.entities.renderer.ShellScan;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.math.MatrixUtil;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public abstract class MixinSubmitNodeCollection {
    @ModifyVariable(method = {"submitModel", "submitModelPart", "submitCustomGeometry"}, at = @At("HEAD"), argsOnly = true, require = 3, name = "renderType")
    private RenderType playershells$applyScan(final RenderType renderType) {
        return ShellScan.renderType(renderType);
    }

    @Inject(method = "submitItem", at = @At("HEAD"), cancellable = true)
    private void playershells$submitScannedItem(final PoseStack poseStack,
                                                final ItemDisplayContext displayContext,
                                                final int lightCoords,
                                                final int overlayCoords,
                                                final int outlineColor,
                                                final int[] tintLayers,
                                                final List<BakedQuad> quads,
        final ItemStackRenderState.FoilType foilType, final CallbackInfo callback) {
        if (!ShellScan.isActive()) {
            return;
        }

        final SubmitNodeCollection nodes = (SubmitNodeCollection) (Object) this;
        final QuadInstance colors = new QuadInstance();
        colors.setLightCoords(lightCoords);
        colors.setOverlayCoords(overlayCoords);
        final PoseStack.Pose foilPose;
        if (foilType == ItemStackRenderState.FoilType.SPECIAL) {
            foilPose = poseStack.last().copy();
            if (displayContext == ItemDisplayContext.GUI) {
                MatrixUtil.mulComponentWise(foilPose.pose(), 0.5F);
            } else if (displayContext.firstPerson()) {
                MatrixUtil.mulComponentWise(foilPose.pose(), 0.75F);
            }
        } else {
            foilPose = null;
        }

        for (final BakedQuad quad : quads) {
            final BakedQuad.MaterialInfo material = quad.materialInfo();
            final int index = material.tintIndex();
            final int tint = material.isTinted() && index >= 0 && index < tintLayers.length ? tintLayers[index] : -1;
            nodes.submitCustomGeometry(poseStack, material.itemRenderType(), (itemPose, buffer) -> {
                colors.setColor(tint);
                buffer.putBakedQuad(itemPose, quad, colors);
            });
            if (foilType != ItemStackRenderState.FoilType.NONE) {
                // Vanilla glint uses an equal-depth test, so it follows the clipped base quad
                nodes.submitCustomGeometry(poseStack, ItemFeatureRenderer.getFoilRenderType(material.itemRenderType(), true), (itemPose, buffer) -> {
                    colors.setColor(tint);
                    final var foilBuffer = foilPose == null ? buffer : new SheetedDecalTextureGenerator(buffer, foilPose, 0.0078125F);
                    foilBuffer.putBakedQuad(itemPose, quad, colors);
                });
            }
        }
        callback.cancel();
    }
}
