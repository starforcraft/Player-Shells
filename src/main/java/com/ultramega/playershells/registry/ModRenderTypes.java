package com.ultramega.playershells.registry;

import com.ultramega.playershells.mixin.RenderStateShardAccessor;
import com.ultramega.playershells.shaders.CreateTextureStateShard;
import com.ultramega.playershells.utils.FiveFunction;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.ultramega.playershells.PlayerShells.MODID;

@OnlyIn(Dist.CLIENT)
public class ModRenderTypes {
    public static ShaderInstance createShaderInstance;
    public static final ShaderStateShard CREATE_SHADER = new ShaderStateShard(() -> createShaderInstance);

    public static final FiveFunction<ResourceLocation, LivingEntity, Integer, Integer, Integer, RenderType> CREATE_SHADER_TYPE =
        (location, entity, percentage, guiScale, color) -> RenderType.create(
            MODID + "_create_shader_type",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            true,
            false,
            CompositeState.builder()
                .setShaderState(CREATE_SHADER)
                .setTextureState(new CreateTextureStateShard(location, entity, percentage, guiScale, color))
                .setTransparencyState(RenderStateShardAccessor.playershells$getNoTransparency())
                .setCullState(RenderStateShardAccessor.playershells$getNoCull())
                .setLightmapState(RenderStateShardAccessor.playershells$getLightmap())
                .setOverlayState(RenderStateShardAccessor.playershells$getOverlay())
                .setDepthTestState(RenderStateShardAccessor.playershells$getLequalDepthTest())
                .createCompositeState(true)
        );
}
