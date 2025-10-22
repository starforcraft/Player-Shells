package com.ultramega.playershells.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLAYER_SHELLS_TAB = CREATIVE_MODE_TABS.register(MODID + "_tab", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID))
            .icon(ModItems.DNA.get()::getDefaultInstance)
            .displayItems((parameters, output) -> {
                for (final DeferredHolder<Item, ?> item : ModItems.ITEMS.getEntries()) {
                    output.accept(item.get());
                }
            }).build());

    private ModCreativeTabs() {
    }
}
