package com.ultramega.playershells.registry;

import com.ultramega.playershells.blockentities.CentrifugeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);

    public static final Supplier<BlockEntityType<ShellForgeBlockEntity>> SHELL_FORGE =
        BLOCK_ENTITY_TYPES.register("shell_forge", () -> BlockEntityType.Builder
            .of(ShellForgeBlockEntity::new, ModBlocks.SHELL_FORGE.get())
            .build(null));
    public static final Supplier<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE =
        BLOCK_ENTITY_TYPES.register("centrifuge", () -> BlockEntityType.Builder
            .of(CentrifugeBlockEntity::new, ModBlocks.CENTRIFUGE.get())
            .build(null));

    private ModBlockEntityTypes() {
    }
}
