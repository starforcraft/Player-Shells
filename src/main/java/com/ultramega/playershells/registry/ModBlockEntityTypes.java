package com.ultramega.playershells.registry;

import com.ultramega.playershells.blockentities.CentrifugeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<BlockEntityType<ShellForgeBlockEntity>> SHELL_FORGE = BLOCK_ENTITY_TYPES.register("shell_forge", () -> BlockEntityType.Builder
        .of(ShellForgeBlockEntity::new, ModBlocks.SHELL_FORGE.get())
        .build(null));
    public static final RegistryObject<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE = BLOCK_ENTITY_TYPES.register("centrifuge", () -> BlockEntityType.Builder
        .of(CentrifugeBlockEntity::new, ModBlocks.CENTRIFUGE.get())
        .build(null));

    private ModBlockEntityTypes() {
    }
}
