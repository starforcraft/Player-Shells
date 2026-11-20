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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.storage.TagValueInput;

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
            entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            entity.setPose(Pose.STANDING);
            entity.setHealth(Math.max(entity.getHealth(), 0.01f));
            return entity;
        });
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
