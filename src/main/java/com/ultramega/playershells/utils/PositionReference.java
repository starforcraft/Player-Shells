package com.ultramega.playershells.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PositionReference(BlockPos pos, Direction facing, ResourceLocation dimension) {
    private static final String POS_TAG = "pos";
    private static final String FACING_TAG = "facing";
    private static final String DIMENSION_TAG = "dimension";

    public void write(final FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.facing);
        buf.writeResourceLocation(this.dimension);
    }

    public static PositionReference read(final FriendlyByteBuf buf) {
        return new PositionReference(
            buf.readBlockPos(),
            buf.readEnum(Direction.class),
            buf.readResourceLocation()
        );
    }

    public CompoundTag toNbt() {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(POS_TAG, this.pos.asLong());
        tag.putInt(FACING_TAG, this.facing.get3DDataValue());
        tag.putString(DIMENSION_TAG, this.dimension.toString());
        return tag;
    }

    public static PositionReference fromNbt(final CompoundTag tag) {
        final BlockPos pos = BlockPos.of(tag.getLong(POS_TAG));
        final Direction facing = Direction.from3DDataValue(tag.getInt(FACING_TAG));
        final ResourceLocation dimension = new ResourceLocation(tag.getString(DIMENSION_TAG));
        return new PositionReference(pos, facing, dimension);
    }
}
