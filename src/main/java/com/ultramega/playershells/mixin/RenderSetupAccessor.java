package com.ultramega.playershells.mixin;

import java.util.Map;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    @Accessor("textures")
    Map<String, Object> playershells$textures();

    @Accessor("layeringTransform")
    LayeringTransform playershells$layeringTransform();
}
