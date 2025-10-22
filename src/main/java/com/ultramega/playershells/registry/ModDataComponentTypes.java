package com.ultramega.playershells.registry;

import com.ultramega.playershells.utils.OwnerData;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModDataComponentTypes {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPE = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final Supplier<DataComponentType<OwnerData>> OWNER_PLAYER =
        DATA_COMPONENT_TYPE.registerComponentType("owner_player", builder -> builder
            .persistent(OwnerData.CODEC)
            .networkSynchronized(OwnerData.STREAM_CODEC));
}
