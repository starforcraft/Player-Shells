package com.ultramega.playershells.packet.c2s;

import com.ultramega.playershells.container.ShellForgeContainerMenu;
import com.ultramega.playershells.storage.ShellSavedData;
import com.ultramega.playershells.storage.ShellState;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.ultramega.playershells.PlayerShells.MODID;

public record RenameShellPacket(int containerId, UUID shellUuid, String name) implements CustomPacketPayload {
    public static final Type<RenameShellPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "rename_shell"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenameShellPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RenameShellPacket::containerId,
        UUIDUtil.STREAM_CODEC, RenameShellPacket::shellUuid,
        ByteBufCodecs.stringUtf8(ShellState.MAX_NAME_LENGTH), RenameShellPacket::name,
        RenameShellPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final RenameShellPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof ShellForgeContainerMenu menu)
                || menu.containerId != data.containerId() || !menu.stillValid(player)) {
                return;
            }
            final var forge = menu.getBlockEntity();
            final var level = player.level();
            if (level.getBlockEntity(forge.getBlockPos()) == forge && forge.canRenameShell(player.getUUID(), data.shellUuid())) {
                ShellSavedData.getShellData(level).renameShell(player.getUUID(), data.shellUuid(), data.name());
            }
        }).exceptionally(e -> null);
    }
}
