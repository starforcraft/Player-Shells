package com.ultramega.playershells.registry;

import com.ultramega.playershells.items.ItemWithOwner;
import com.ultramega.playershells.items.SyringeItem;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<ItemWithOwner> DNA = ITEMS.registerItem("dna", ItemWithOwner::new);
    public static final DeferredItem<SyringeItem> EMPTY_SYRINGE = ITEMS.registerItem("empty_syringe", SyringeItem::new, properties -> properties.stacksTo(1));
    public static final DeferredItem<SyringeItem> BLOOD_SYRINGE = ITEMS.registerItem("blood_syringe", SyringeItem::new);

    public static final DeferredItem<BlockItem> SHELL_FORGE = ITEMS.registerSimpleBlockItem("shell_forge",
        ModBlocks.SHELL_FORGE);
    public static final DeferredItem<BlockItem> CENTRIFUGE = ITEMS.registerSimpleBlockItem("centrifuge",
        ModBlocks.CENTRIFUGE);

    private ModItems() {
    }
}
