package com.ultramega.playershells.utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.NonNullList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

public class ShellBundle {
    private final UUID id;
    private final List<ShellEntry> entries;

    public ShellBundle(final UUID id, final List<ShellEntry> entries) {
        this.id = id;
        this.entries = List.copyOf(entries);
    }

    public List<ShellEntry> getEntries() {
        return this.entries;
    }

    public record ShellEntry(String title,
                             Optional<PositionReference> shellForgePos,
                             Optional<InventoryEntry> inventory,
                             Optional<StatEntry> stats,
                             Optional<UUID> shellUuid) {
    }

    public record InventoryEntry(NonNullList<ItemStack> items, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) {
    }

    public record StatEntry(float health, int armorValue, int foodLevel, Map<MobEffect, MobEffectInstance> activeEffects) {
    }
}
