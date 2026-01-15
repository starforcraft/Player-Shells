package com.ultramega.playershells.storage;

import com.ultramega.playershells.utils.PositionReference;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record ShellState(UUID shellUUID, PositionReference shellForgePos, CompoundTag playerData, int shellCreationProgress) {
    private static final String SHELL_UUID_TAG = "shell_uuid";
    private static final String SHELL_FORGE_POS_TAG = "shell_forge_pos";
    private static final String PLAYER_DATA_TAG = "player_data";
    private static final String CREATION_PROGRESS_TAG = "creation_progress";

    public void write(final FriendlyByteBuf buf) {
        buf.writeUUID(this.shellUUID);
        this.shellForgePos.write(buf);
        buf.writeNbt(this.playerData);
        buf.writeVarInt(this.shellCreationProgress);
    }

    public static ShellState read(final FriendlyByteBuf buf) {
        final UUID shellUuid = buf.readUUID();
        final PositionReference pos = PositionReference.read(buf);
        final CompoundTag playerData = buf.readNbt();
        final int progress = buf.readVarInt();
        return new ShellState(shellUuid, pos, playerData == null ? new CompoundTag() : playerData, progress);
    }

    public CompoundTag toNbt() {
        final CompoundTag tag = new CompoundTag();
        tag.putUUID(SHELL_UUID_TAG, this.shellUUID);
        tag.put(SHELL_FORGE_POS_TAG, this.shellForgePos.toNbt());
        tag.put(PLAYER_DATA_TAG, this.playerData);
        tag.putInt(CREATION_PROGRESS_TAG, this.shellCreationProgress);
        return tag;
    }

    public static ShellState fromNbt(final CompoundTag tag) {
        final UUID uuid = tag.getUUID(SHELL_UUID_TAG);
        final PositionReference pos = PositionReference.fromNbt(tag.getCompound(SHELL_FORGE_POS_TAG));
        final CompoundTag playerData = tag.getCompound(PLAYER_DATA_TAG);
        final int progress = tag.getInt(CREATION_PROGRESS_TAG);
        return new ShellState(uuid, pos, playerData, progress);
    }
}
