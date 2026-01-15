package com.ultramega.playershells.storage;

import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class ClientShellData {
    public static final ClientShellData INSTANCE = new ClientShellData();

    private Multimap<UUID, ShellState> entries = ArrayListMultimap.create();
    private int version;

    public void set(final Multimap<UUID, ShellState> entries) {
        this.entries = entries;
        this.version++;
    }

    public int getVersion() {
        return this.version;
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
