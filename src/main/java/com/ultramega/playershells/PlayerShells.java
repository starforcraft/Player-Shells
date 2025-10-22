package com.ultramega.playershells;

import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModBlocks;
import com.ultramega.playershells.registry.ModCreativeTabs;
import com.ultramega.playershells.registry.ModDataComponentTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.registry.ModMenuTypes;
import com.ultramega.playershells.registry.ModSoundEvents;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(PlayerShells.MODID)
public final class PlayerShells {
    public static final String MODID = "playershells";

    public PlayerShells(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModDataComponentTypes.DATA_COMPONENT_TYPE.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);
    }
}
