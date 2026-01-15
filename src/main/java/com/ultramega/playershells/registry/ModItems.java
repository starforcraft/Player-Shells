package com.ultramega.playershells.registry;

import com.ultramega.playershells.items.ItemWithOwner;
import com.ultramega.playershells.items.SyringeItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<ItemWithOwner> DNA = ITEMS.register("dna", () -> new ItemWithOwner(new Item.Properties()));
    public static final RegistryObject<SyringeItem> EMPTY_SYRINGE = ITEMS.register("empty_syringe", () -> new SyringeItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<SyringeItem> BLOOD_SYRINGE = ITEMS.register("blood_syringe", () -> new SyringeItem(new Item.Properties()));

    public static final RegistryObject<BlockItem> SHELL_FORGE = ITEMS.register("shell_forge",
        () -> new BlockItem(ModBlocks.SHELL_FORGE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> CENTRIFUGE = ITEMS.register("centrifuge",
        () -> new BlockItem(ModBlocks.CENTRIFUGE.get(), new Item.Properties()));

    private ModItems() {
    }
}
