package com.ultramega.playershells.registry;

import com.ultramega.playershells.mixin.RenderSetupAccessor;
import com.ultramega.playershells.mixin.RenderSetupBuilderAccessor;
import com.ultramega.playershells.mixin.RenderTypeAccessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModRenderTypes {
    private static final Identifier CREATE_SHADER = Identifier.fromNamespaceAndPath(MODID, "core/create_shader");

    public static final RenderPipeline CREATE_SHELL = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(MODID, "pipeline/create_shell"))
        .withVertexShader(CREATE_SHADER)
        .withFragmentShader(CREATE_SHADER)
        .withSampler("Sampler1")
        .withCull(false)
        .build();

    private ModRenderTypes() {
    }

    public static RenderType createShell(final RenderType original, final Matrix4f scanParameters) {
        if (!original.format().equals(CREATE_SHELL.getVertexFormat()) || original.mode() != CREATE_SHELL.getVertexFormatMode()) {
            return original;
        }

        final var setup = (RenderSetupAccessor) (Object) ((RenderTypeAccessor) original).playershells$renderSetup();
        final Object texture = setup.playershells$textures().get("Sampler0");
        if (texture == null) {
            return original;
        }

        final var builder = RenderSetup.builder(CREATE_SHELL)
            .useLightmap()
            .useOverlay()
            .setLayeringTransform(setup.playershells$layeringTransform())
            .setTextureTransform(new TextureTransform("playershells_scan_parameters", () -> scanParameters));
        ((RenderSetupBuilderAccessor) builder).playershells$textures().put("Sampler0", texture);
        return RenderType.create("playershells_create_shell", builder.createRenderSetup());
    }
}
