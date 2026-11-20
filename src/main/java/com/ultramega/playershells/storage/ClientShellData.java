package com.ultramega.playershells.storage;

import com.ultramega.playershells.entities.ShellEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

public class ClientShellData {
    public static final ClientShellData INSTANCE = new ClientShellData();

    private final Map<UUID, ShellEntity> previews = new HashMap<>();

    private Multimap<UUID, ShellState> entries = ArrayListMultimap.create();

    public void set(final Multimap<UUID, ShellState> entries) {
        this.previews.keySet().retainAll(entries.values().stream().map(ShellState::shellUUID).collect(Collectors.toSet()));
        this.entries = entries;
    }

    public void clearPreviews() {
        this.previews.clear();
    }

    public void clear() {
        this.clearPreviews();
        this.entries = ArrayListMultimap.create();
    }

    @Nullable
    public ShellEntity getPreview(final ClientLevel level, final UUID owner, final UUID shellUuid) {
        final ShellState shell = this.get(owner, shellUuid);
        if (shell == null) {
            return null;
        }

        return this.previews.compute(shellUuid, (ignored, cached) -> {
            if (cached != null && cached.level() == level) {
                return cached;
            }

            final ShellEntity entity = new ShellEntity(level, new GameProfile(owner, ""));
            final var tag = shell.playerData().copy();
            tag.remove(Entity.TAG_UUID);
            final var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
            entity.load(input);
            loadPreviewArmor(entity, input);
            entity.setPose(Pose.STANDING);
            entity.setHealth(Math.max(entity.getHealth(), 0.01f));
            return entity;
        });
    }

    private static void loadPreviewArmor(final ShellEntity preview, final ValueInput input) {
        final AttributeInstance armor = preview.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }

        // Client previews skip the server's attribute load and equipment tick.
        input.read(LivingEntity.TAG_ATTRIBUTES, AttributeInstance.Packed.LIST_CODEC).ifPresent(attributes ->
            attributes.stream().filter(attribute -> attribute.attribute().equals(Attributes.ARMOR)).findFirst().ifPresent(armor::apply));
        for (final EquipmentSlot slot : EquipmentSlot.VALUES) {
            final var stack = preview.getItemBySlot(slot);
            if (!stack.isEmpty() && !stack.isBroken()) {
                stack.forEachModifier(slot, (attribute, modifier) -> {
                    if (attribute.equals(Attributes.ARMOR)) {
                        armor.removeModifier(modifier.id());
                        armor.addTransientModifier(modifier);
                    }
                });
            }
        }
    }

    public Collection<ShellState> getAll(final UUID uuid) {
        return this.entries.get(uuid);
    }

    @Nullable
    public ShellState get(final UUID playerUuid, final UUID shellUuid) {
        for (final ShellState state : this.entries.get(playerUuid)) {
            if (state != null && state.shellUUID().equals(shellUuid)) {
                return state;
            }
        }

        return null;
    }
}
