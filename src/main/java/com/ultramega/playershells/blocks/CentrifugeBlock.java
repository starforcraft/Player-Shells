package com.ultramega.playershells.blocks;

import com.ultramega.playershells.blockentities.CentrifugeBlockEntity;
import com.ultramega.playershells.registry.ModBlockEntityTypes;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import static com.ultramega.playershells.utils.MathUtils.createTickerHelper;

public class CentrifugeBlock extends Block implements EntityBlock {
    public CentrifugeBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof CentrifugeBlockEntity blockEntity) {
            if (!level.isClientSide()) {
                player.openMenu(blockEntity, pos);
            }
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos blockPos, final BlockState state) {
        return new CentrifugeBlockEntity(blockPos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        return !level.isClientSide()
            ? createTickerHelper(blockEntityType, ModBlockEntityTypes.CENTRIFUGE.get(), CentrifugeBlockEntity::serverTick)
            : null;
    }
}
