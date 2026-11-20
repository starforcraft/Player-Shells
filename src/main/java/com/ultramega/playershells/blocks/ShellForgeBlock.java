package com.ultramega.playershells.blocks;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.PlayerStates;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.events.ShellForgeClient;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModBlocks;
import com.ultramega.playershells.storage.ShellSavedData;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static com.ultramega.playershells.utils.MathUtils.createTickerHelper;
import static com.ultramega.playershells.utils.MathUtils.getMinVelocity;

public class ShellForgeBlock extends AbstractMultiblockBlock implements EntityBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final VoxelShape ROOF = Block.box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape NORTH_WALL = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape SOUTH_WALL = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape EAST_WALL = Block.box(15, 0, 0, 16, 16, 16);
    private static final VoxelShape WEST_WALL = Block.box(0, 0, 0, 1, 16, 16);

    private static final int HALF_BOTTOM = 0;
    private static final int HALF_TOP = 1;
    private static final int STATE_CLOSED = 0;
    private static final int STATE_OPEN = 1;

    private static final VoxelShape[][][] SHAPES = new VoxelShape[2 /*half*/][2 /*open/closed*/][4 /*facing*/];

    static {
        final VoxelShape allWalls = Shapes.or(NORTH_WALL, SOUTH_WALL, EAST_WALL, WEST_WALL).optimize();
        final VoxelShape closedBottom = Shapes.or(allWalls, FLOOR).optimize();
        final VoxelShape closedTop = Shapes.or(allWalls, ROOF).optimize();

        final VoxelShape openToNorth = Shapes.or(NORTH_WALL, EAST_WALL, WEST_WALL).optimize();
        final VoxelShape openToSouth = Shapes.or(SOUTH_WALL, EAST_WALL, WEST_WALL).optimize();
        final VoxelShape openToEast = Shapes.or(NORTH_WALL, SOUTH_WALL, EAST_WALL).optimize();
        final VoxelShape openToWest = Shapes.or(NORTH_WALL, SOUTH_WALL, WEST_WALL).optimize();

        final VoxelShape openToNorthBottom = Shapes.or(openToNorth, FLOOR).optimize();
        final VoxelShape openToSouthBottom = Shapes.or(openToSouth, FLOOR).optimize();
        final VoxelShape openToEastBottom = Shapes.or(openToEast, FLOOR).optimize();
        final VoxelShape openToWestBottom = Shapes.or(openToWest, FLOOR).optimize();

        final VoxelShape openToNorthTop = Shapes.or(openToNorth, ROOF).optimize();
        final VoxelShape openToSouthTop = Shapes.or(openToSouth, ROOF).optimize();
        final VoxelShape openToEastTop = Shapes.or(openToEast, ROOF).optimize();
        final VoxelShape openToWestTop = Shapes.or(openToWest, ROOF).optimize();

        for (int i = 0; i < 4; i++) {
            SHAPES[HALF_BOTTOM][STATE_CLOSED][i] = closedBottom;
            SHAPES[HALF_TOP][STATE_CLOSED][i] = closedTop;
        }

        SHAPES[HALF_BOTTOM][STATE_OPEN][dirToIndex(Direction.NORTH)] = openToNorthBottom;
        SHAPES[HALF_BOTTOM][STATE_OPEN][dirToIndex(Direction.SOUTH)] = openToSouthBottom;
        SHAPES[HALF_BOTTOM][STATE_OPEN][dirToIndex(Direction.EAST)] = openToEastBottom;
        SHAPES[HALF_BOTTOM][STATE_OPEN][dirToIndex(Direction.WEST)] = openToWestBottom;

        SHAPES[HALF_TOP][STATE_OPEN][dirToIndex(Direction.NORTH)] = openToNorthTop;
        SHAPES[HALF_TOP][STATE_OPEN][dirToIndex(Direction.SOUTH)] = openToSouthTop;
        SHAPES[HALF_TOP][STATE_OPEN][dirToIndex(Direction.EAST)] = openToEastTop;
        SHAPES[HALF_TOP][STATE_OPEN][dirToIndex(Direction.WEST)] = openToWestTop;
    }


    public ShellForgeBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(OPEN, false));
    }

    @Override
    protected InteractionResult useItemOn(final ItemStack stack,
                                              final BlockState state,
                                              final Level level,
                                              final BlockPos pos,
                                              final Player player,
                                              final InteractionHand hand,
                                              final BlockHitResult hitResult) {
        final boolean handled = this.handleUseCommon(level, state, pos, player);
        return handled ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state,
                                               final Level level,
                                               final BlockPos pos,
                                               final Player player,
                                               final BlockHitResult hitResult) {
        final boolean handled = this.handleUseCommon(level, state, pos, player);
        return handled ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private boolean handleUseCommon(final Level level,
                                    final BlockState state,
                                    final BlockPos pos,
                                    final Player player) {
        final BlockPos shellForgePos = isBottomHalf(state) ? pos : pos.below();
        final BlockState shellForgeState = level.getBlockState(shellForgePos);

        if (!(level.getBlockEntity(shellForgePos) instanceof ShellForgeBlockEntity shellForge)) {
            return false;
        }

        final boolean hasShellInside = shellForge.getShellState() != ShellStates.CREATE;
        final boolean exiting = !hasShellInside
            && (shellForge.getPlayerState() == PlayerStates.TRANSFERRED || shellForge.getPlayerState() == PlayerStates.NONE)
            && (player.blockPosition().equals(shellForgePos) || player.blockPosition().equals(shellForgePos.above()));
        if (exiting) {
            if (!level.isClientSide()) {
                shellForge.setPlayerState(PlayerStates.GOING_OUT);
            }
        } else if (!level.isClientSide()) {
            // Open menu if there's a shell or SHIFT is held; otherwise start going in
            if (hasShellInside || player.isShiftKeyDown()) {
                player.openMenu(shellForge, shellForgePos);
                return true;
            }

            // Shift wasn't pressed, go in or out
            shellForge.setPlayerState(PlayerStates.GOING_IN);
            return true;
        } else if (!hasShellInside && !player.isShiftKeyDown()) {
            ShellForgeClient.onUse(player, level, shellForgePos, shellForgeState);
        }

        return true;
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        final int halfIdx = isBottomHalf(state) ? HALF_BOTTOM : HALF_TOP;
        final int stateIdx = isOpen(state) ? STATE_OPEN : STATE_CLOSED;

        return SHAPES[halfIdx][stateIdx][dirToIndex(state.getValue(FACING).getOpposite())];
    }

    private static int dirToIndex(final Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> throw new IllegalArgumentException("Direction must be horizontal: " + direction);
        };
    }

    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity,
                                final InsideBlockEffectApplier effects, final boolean precise) {
        if (!(entity instanceof Player player) || !isBottomHalf(state) || !(level.getBlockEntity(pos) instanceof ShellForgeBlockEntity shellForge)) {
            return;
        }

        if (level.isClientSide()) {
            ShellForgeClient.entityInside(player, level, pos, state, shellForge);
        }
    }

    public static void movePlayerInside(final Player player, final BlockPos target, final Direction facing) {
        final float yaw = facing.toYRot();

        movePlayerTo(player, target, 0.06250);

        // TODO: The rotation is currently snapping change it to be smooth instead
        player.setXRot(0);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setYBodyRot(yaw);
        player.yRotO = yaw;
        player.yBodyRotO = yaw;
        player.yHeadRotO = yaw;
    }

    public static void movePlayerTo(final Player player, final BlockPos target, final double offsetY) {
        final Vec3 currentPos = player.position();
        final double targetX = target.getX() + 0.5;
        final double targetY = target.getY() + offsetY;
        final double targetZ = target.getZ() + 0.5;

        final double currentX = currentPos.x;
        final double currentY = currentPos.y;
        final double currentZ = currentPos.z;

        final double velocityX = getMinVelocity(targetX - currentX, 0.33);
        final double velocityY = getMinVelocity(targetY - currentY, 0.33);
        final double velocityZ = getMinVelocity(targetZ - currentZ, 0.33);

        player.setDeltaMovement(velocityX, velocityY, velocityZ);
    }

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        ShellSavedData.getShellData(level).validateShellData(level);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos blockPos, final BlockState state) {
        if (!isBottomHalf(state)) {
            return null;
        }
        return new ShellForgeBlockEntity(blockPos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> blockEntityType) {
        if (!isBottomHalf(state)) {
            return null;
        }
        return !level.isClientSide()
            ? createTickerHelper(blockEntityType, ModBlockEntityTypes.SHELL_FORGE.get(), ShellForgeBlockEntity::serverTick)
            : createTickerHelper(blockEntityType, ModBlockEntityTypes.SHELL_FORGE.get(), ShellForgeClient::clientTick);
    }

    public static void setValue(final BlockState state,
                                final Level level,
                                final BlockPos pos,
                                final BoolProperty... properties) {
        BlockState newState = state;
        for (final BoolProperty property : properties) {
            newState = newState.setValue(property.property(), property.value());
        }
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        final BlockPos otherHalfPos = pos.relative(isBottomHalf(newState) ? Direction.UP : Direction.DOWN);
        final BlockState otherHalfState = level.getBlockState(otherHalfPos);

        if (otherHalfState.is(ModBlocks.SHELL_FORGE.get())) {
            BlockState newOther = otherHalfState;
            for (final BoolProperty property : properties) {
                newOther = newOther.setValue(property.property(), property.value());
            }
            level.setBlock(otherHalfPos, newOther, Block.UPDATE_ALL);
        }
    }

    public static boolean isOpen(final BlockState state) {
        return state.getValue(OPEN);
    }

    public record BoolProperty(BooleanProperty property, boolean value) { }
}
