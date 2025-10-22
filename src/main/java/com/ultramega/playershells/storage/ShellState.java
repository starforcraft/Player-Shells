package com.ultramega.playershells.storage;

import com.ultramega.playershells.utils.MathUtils;
import com.ultramega.playershells.utils.PositionReference;

import java.util.UUID;

import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ShellState(UUID shellUUID, PositionReference shellForgePos, CompoundTag playerData, int shellCreationProgress) {
    public static final StreamCodec<ByteBuf, ShellState> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC, ShellState::shellUUID,
        PositionReference.STREAM_CODEC, ShellState::shellForgePos,
        ByteBufCodecs.COMPOUND_TAG, ShellState::playerData,
        ByteBufCodecs.INT, ShellState::shellCreationProgress,
        ShellState::new
    );

    public static final Codec<ShellState> CODEC = RecordCodecBuilder.create(in -> in.group(
        UUIDUtil.CODEC.fieldOf("shellUUID").forGetter(ShellState::shellUUID),
        PositionReference.CODEC.fieldOf("oldShellForgePos").forGetter(ShellState::shellForgePos),
        CompoundTag.CODEC.fieldOf("playerData").forGetter(ShellState::playerData),
        Codec.INT.fieldOf("shellCreationProgress").forGetter(ShellState::shellCreationProgress)
    ).apply(in, ShellState::new));

    public static final Codec<Multimap<UUID, ShellState>> MULTIMAP_CODEC = MathUtils.multiMapCodec(Codec.STRING.xmap(UUID::fromString, UUID::toString), ShellState.CODEC);
}
