package com.ultramega.playershells.registry;

import com.ultramega.playershells.items.ItemWithOwner;
import com.ultramega.playershells.items.SyringeItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<ItemWithOwner> DNA = ITEMS.registerItem("dna", ItemWithOwner::new, new Item.Properties());
    public static final DeferredItem<SyringeItem> EMPTY_SYRINGE = ITEMS.registerItem("empty_syringe", SyringeItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<SyringeItem> BLOOD_SYRINGE = ITEMS.registerItem("blood_syringe", SyringeItem::new, new Item.Properties());

    public static final DeferredItem<BlockItem> SHELL_FORGE = ITEMS.registerSimpleBlockItem("shell_forge",
        ModBlocks.SHELL_FORGE, new Item.Properties());
    public static final DeferredItem<BlockItem> CENTRIFUGE = ITEMS.registerSimpleBlockItem("centrifuge",
        ModBlocks.CENTRIFUGE, new Item.Properties());

    private ModItems() {
    }
}
