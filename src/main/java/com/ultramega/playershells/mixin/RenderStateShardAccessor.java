package com.ultramega.playershells.mixin;

import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderStateShard.class)
public interface RenderStateShardAccessor {
    @Accessor("NO_TRANSPARENCY")
    static RenderStateShard.TransparencyStateShard playershells$getNoTransparency() {
        throw new AssertionError();
    }

    @Accessor("NO_CULL")
    static RenderStateShard.CullStateShard playershells$getNoCull() {
        throw new AssertionError();
    }

    @Accessor("LIGHTMAP")
    static RenderStateShard.LightmapStateShard playershells$getLightmap() {
        throw new AssertionError();
    }

    @Accessor("OVERLAY")
    static RenderStateShard.OverlayStateShard playershells$getOverlay() {
        throw new AssertionError();
    }

    @Accessor("LEQUAL_DEPTH_TEST")
    static RenderStateShard.DepthTestStateShard playershells$getLequalDepthTest() {
        throw new AssertionError();
    }
}

