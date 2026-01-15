package com.ultramega.playershells;

import com.ultramega.playershells.network.ModNetworking;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModBlocks;
import com.ultramega.playershells.registry.ModCreativeTabs;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.registry.ModMenuTypes;
import com.ultramega.playershells.registry.ModSoundEvents;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PlayerShells.MODID)
public final class PlayerShells {
    public static final String MODID = "playershells";

    public PlayerShells() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(ModNetworking::onCommonSetup);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);
    }
}
