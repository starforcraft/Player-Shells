package com.ultramega.playershells.packet.s2c;

import com.ultramega.playershells.storage.ClientShellData;
import com.ultramega.playershells.storage.ShellState;

import java.util.UUID;
import java.util.function.Supplier;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncShellDataPacket(Multimap<UUID, ShellState> entries) {
    public static void encode(final SyncShellDataPacket data, final FriendlyByteBuf buf) {
        buf.writeVarInt(data.entries.size());
        data.entries.entries().forEach(entry -> {
            buf.writeUUID(entry.getKey());
            entry.getValue().write(buf);
        });
    }

    public static SyncShellDataPacket decode(final FriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        final Multimap<UUID, ShellState> entries = ArrayListMultimap.create();
        for (int i = 0; i < size; i++) {
            final UUID playerUuid = buf.readUUID();
            final ShellState state = ShellState.read(buf);
            entries.put(playerUuid, state);
        }
        return new SyncShellDataPacket(entries);
    }

    public static void handle(final SyncShellDataPacket data, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientShellData.INSTANCE.set(data.entries));
        context.setPacketHandled(true);
    }
}
