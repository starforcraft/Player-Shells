package com.ultramega.playershells.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> PLAYER_SHELLS_TAB = CREATIVE_MODE_TABS.register(MODID + "_tab", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .icon(ModItems.DNA.get()::getDefaultInstance)
            .displayItems((parameters, output) -> {
                ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
            }).build());

    private ModCreativeTabs() {
    }
}
