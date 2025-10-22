package com.ultramega.playershells.registry;

import com.ultramega.playershells.blocks.CentrifugeBlock;
import com.ultramega.playershells.blocks.ShellForgeBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);

    public static final DeferredBlock<Block> SHELL_FORGE = BLOCKS.register("shell_forge", () ->
        new ShellForgeBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()));
    public static final DeferredBlock<Block> CENTRIFUGE = BLOCKS.register("centrifuge", () ->
        new CentrifugeBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()));

    private ModBlocks() {
    }
}
