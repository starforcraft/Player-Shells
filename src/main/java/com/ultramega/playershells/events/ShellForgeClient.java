package com.ultramega.playershells.events;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.PlayerStates;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.gui.ShellSelectionOverlay;
import com.ultramega.playershells.packet.c2s.LeaveShellForgePacket;
import com.ultramega.playershells.packet.c2s.TransferPlayerPacket;
import com.ultramega.playershells.packet.c2s.ValidateShellForgePacket;
import com.ultramega.playershells.registry.ModSoundEvents;
import com.ultramega.playershells.utils.CameraHandler;
import com.ultramega.playershells.utils.PositionReference;
import com.ultramega.playershells.utils.SoundHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import static com.ultramega.playershells.blocks.AbstractMultiblockBlock.FACING;
import static com.ultramega.playershells.blocks.AbstractMultiblockBlock.HALF;
import static com.ultramega.playershells.blocks.ShellForgeBlock.OPEN;
import static com.ultramega.playershells.blocks.ShellForgeBlock.movePlayerInside;
import static com.ultramega.playershells.blocks.ShellForgeBlock.movePlayerTo;
import static com.ultramega.playershells.utils.MathUtils.isPlayerInFront;
import static com.ultramega.playershells.utils.MathUtils.overlapsShellForge;

public final class ShellForgeClient {
    private static boolean blockShellSelection;

    private ShellForgeClient() {
    }

    public static void onUse(final Player player, final Level level, final BlockPos pos, final BlockState state) {
        final Minecraft mc = Minecraft.getInstance();
        if (player == mc.player) {
            openShellSelection(player, level, pos, state);
            mc.options.keyUse.setDown(false);
        }
    }

    public static void clearSelection() {
        blockShellSelection = false;
        ShellSelectionOverlay.INSTANCE.close(false);
    }

    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final ShellForgeBlockEntity blockEntity) {
        blockEntity.tickDoorAnimation(state.getValue(OPEN));
        if (blockEntity.getShellState() == ShellStates.EXTERMINATING) {
            if (level.getRandom().nextDouble() < 0.1) {
                SoundHandler.startBlockSound(ModSoundEvents.FLAMETHROWER.get(), SoundSource.BLOCKS, 1.5F, 1.0F, level.getRandom(), pos);
            }
        } else {
            SoundHandler.stopAllBlockSounds(pos);
        }

        final Player player = Minecraft.getInstance().player;
        if (player == null || !state.getValue(OPEN)) {
            return;
        }

        final PlayerStates playerState = blockEntity.getPlayerState();
        if (playerState == PlayerStates.GOING_OUT && overlapsShellForge(player, pos)) {
            movePlayerTo(player, pos.relative(state.getValue(FACING)), 0);
        } else if ((playerState == PlayerStates.GOING_IN || playerState == PlayerStates.INSIDE)
            && isPlayerInFront(pos, level, player.getUUID(), state.getValue(FACING))) {
            movePlayerInside(player, pos, state.getValue(FACING));
        }
    }

    public static void entityInside(final Player player, final Level level, final BlockPos pos,
                                    final BlockState state, final ShellForgeBlockEntity shellForge) {
        if (player != Minecraft.getInstance().player) {
            return;
        }
        final PlayerStates playerState = shellForge.getPlayerState();
        if (playerState == PlayerStates.NONE) {
            blockShellSelection = false;
        }
        if (!ShellSelectionOverlay.INSTANCE.isOpened() && playerState == PlayerStates.INSIDE && !blockShellSelection) {
            openShellSelection(player, level, pos, state);
        } else if (ShellSelectionOverlay.INSTANCE.isOpened()
            && (playerState == PlayerStates.TRANSFERRED || playerState == PlayerStates.GOING_OUT)) {
            ShellSelectionOverlay.INSTANCE.close(true);
        }
    }

    private static void openShellSelection(final Player player, final Level level, final BlockPos pos, final BlockState state) {
        final BlockPos thisShellForgePos = (state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos).immutable();
        final Direction thisFacing = level.getBlockState(thisShellForgePos).getValue(FACING);
        final PositionReference thisShellForgePosReference = new PositionReference(thisShellForgePos, thisFacing, level.dimension().identifier());

        ShellSelectionOverlay.INSTANCE.open(player, (shellEntry) -> {
            // TODO: slowly build up green water from below and add mask before playing the animation
            if (shellEntry.shellForgePos().isEmpty()) {
                return;
            }

            ClientPacketDistributor.sendToServer(new ValidateShellForgePacket());

            // Play camera animation
            final BlockPos newShellForgePos = shellEntry.shellForgePos().get().pos().immutable();
            final Direction newShellForgeFacing = shellEntry.shellForgePos().get().facing();
            CameraHandler.setMovingAnimation(thisShellForgePos.above(), thisFacing, newShellForgePos.above(), newShellForgeFacing, () -> {
                ClientPacketDistributor.sendToServer(new TransferPlayerPacket(thisShellForgePosReference, shellEntry.shellForgePos().get()));
            });
        }, () -> {
            blockShellSelection = true;
            ClientPacketDistributor.sendToServer(new LeaveShellForgePacket(thisShellForgePos));
        });
    }

}
