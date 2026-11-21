package com.ultramega.playershells.blockentities;

import com.ultramega.playershells.Config;
import com.ultramega.playershells.blocks.ShellForgeBlock.BoolProperty;
import com.ultramega.playershells.container.ShellForgeContainerMenu;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModBlocks;
import com.ultramega.playershells.registry.ModDataComponentTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.storage.ShellSavedData;
import com.ultramega.playershells.storage.ShellState;
import com.ultramega.playershells.utils.MachineInventory;
import com.ultramega.playershells.utils.ObservableEnergyStorage;
import com.ultramega.playershells.utils.PositionReference;
import com.ultramega.playershells.utils.ShellPlayer;

import java.util.Objects;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import static com.ultramega.playershells.Config.SHELL_FORGE_DNA_AMOUNT;
import static com.ultramega.playershells.blocks.AbstractMultiblockBlock.FACING;
import static com.ultramega.playershells.blocks.ShellForgeBlock.OPEN;
import static com.ultramega.playershells.blocks.ShellForgeBlock.isOpen;
import static com.ultramega.playershells.blocks.ShellForgeBlock.setValue;
import static com.ultramega.playershells.utils.MathUtils.EMPTY_UUID;
import static com.ultramega.playershells.utils.MathUtils.hasPlayerInside;
import static com.ultramega.playershells.utils.MathUtils.smoothstep01;

public class ShellForgeBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    private static final float OPEN_SPEED = 0.12f;
    private static final int TRANSFER_DRAIN_TICKS = 34;
    private static final int MASK_FIT_TICKS = 14;

    public final MachineInventory inventoryHandler = new MachineInventory(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ShellForgeBlockEntity.super.setChanged();
        }
    };
    public final ObservableEnergyStorage energyStorage = new ObservableEnergyStorage(Config.SHELL_FORGE_ENERGY_CAPACITY.get()) {
        @Override
        public void onEnergyChanged() {
            ShellForgeBlockEntity.this.setChanged();
        }
    };

    private UUID playerUuid = EMPTY_UUID;
    private PlayerStates playerState = PlayerStates.NONE;
    private UUID shellUuid = EMPTY_UUID;
    private ShellStates shellState = ShellStates.CREATE;
    private int shellPercentage;
    private int shellPercentageCooldownTick;
    private long lastTransferTick = -1L;

    private float animProgress;
    private float animPrevProgress;
    private float fluidFill = -1f;
    private float previousFluidFill;
    private long observedTransferTick = -1L;
    private int drainTicks = TRANSFER_DRAIN_TICKS;
    private int previousDrainTicks = TRANSFER_DRAIN_TICKS;
    private int maskFitTicks = MASK_FIT_TICKS;
    private int previousMaskFitTicks = MASK_FIT_TICKS;

    public ShellForgeBlockEntity(final BlockPos pos,
                                 final BlockState blockState) {
        super(ModBlockEntityTypes.SHELL_FORGE.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ShellForgeBlockEntity blockEntity) {
        blockEntity.updatePlayerStateAndOpened(state, pos);

        blockEntity.tickShell();
    }

    private void tickShell() {
        if (this.shellState == ShellStates.EXTERMINATE || this.shellState == ShellStates.DECAYING || this.shellState == ShellStates.RECOVERING) {
            this.updateMaintenance();
        }

        switch (this.shellState) {
            case CREATING, RECOVERING -> this.advanceShell(1, Config.SHELL_FORGE_CREATION_COOLDOWN.get());
            case DECAYING -> this.advanceShell(-1, Config.SHELL_FORGE_DECAY_COOLDOWN.get());
            case EXTERMINATING -> this.advanceShell(-1, 1);
            default -> { }
        }
    }

    private void advanceShell(final int step, final int cooldown) {
        if (step > 0 && this.shellPercentage >= 100) {
            this.setShellState(ShellStates.EXTERMINATE);
            this.updateShellCreationProgressOnClient(100);
            return;
        }
        if (++this.shellPercentageCooldownTick < cooldown && (step > 0 || this.shellPercentage > 0)) {
            super.setChanged();
            return;
        }

        this.shellPercentageCooldownTick = 0;
        this.shellPercentage = Mth.clamp(this.shellPercentage + step, 0, 100);
        if (this.shellPercentage == 0) {
            if (this.level instanceof ServerLevel serverLevel) {
                ShellSavedData.getShellData(serverLevel).archive(this.playerUuid, this.shellUuid);
            }
            this.clearShell();
        } else {
            if (this.shellPercentage == 100) {
                this.shellState = ShellStates.EXTERMINATE;
            }
            this.updateShellCreationProgressOnClient(this.shellPercentage);
            this.setChanged();
        }
    }

    public void tickDoorAnimation(final boolean open) {
        this.animPrevProgress = this.animProgress;
        final float target = open ? 1.0f : 0.0f;
        if (this.animProgress < target) {
            this.animProgress = Math.min(target, this.animProgress + OPEN_SPEED);
        } else if (this.animProgress > target) {
            this.animProgress = Math.max(target, this.animProgress - OPEN_SPEED);
        }
    }

    public boolean tickChamberAnimation(final boolean waitingForArrivalCamera) {
        this.previousMaskFitTicks = this.maskFitTicks;
        this.maskFitTicks = Math.min(this.maskFitTicks + 1, MASK_FIT_TICKS);

        final float targetFill = this.targetFluidFill();
        this.previousFluidFill = this.fluidFill < 0f ? targetFill : this.fluidFill;
        this.fluidFill = Math.abs(this.previousFluidFill - targetFill) < 0.001f ? targetFill
            : Mth.lerp(0.2f, this.previousFluidFill, targetFill);

        final boolean newTransfer = this.observedTransferTick != this.lastTransferTick;
        if (newTransfer) {
            this.observedTransferTick = this.lastTransferTick;
            // A client that starts tracking an old transfer should see an empty chamber.
            this.drainTicks = this.lastTransferTick < 0L || this.level == null ? TRANSFER_DRAIN_TICKS
                : (int) Math.clamp(this.level.getGameTime() - this.lastTransferTick, 0L, TRANSFER_DRAIN_TICKS);
        }
        final boolean holdDrain = waitingForArrivalCamera && this.playerState == PlayerStates.TRANSFERRED && this.lastTransferTick >= 0L;
        if (holdDrain) {
            this.drainTicks = 0;
        }
        this.previousDrainTicks = this.drainTicks;
        if (holdDrain || this.drainTicks >= TRANSFER_DRAIN_TICKS) {
            return false;
        }
        final boolean releaseStarted = newTransfer || this.drainTicks == 0;
        this.drainTicks++;
        return releaseStarted;
    }

    private float targetFluidFill() {
        return switch (this.shellState) {
            case CREATING, RECOVERING, DECAYING -> Mth.clamp(this.shellPercentage / 100f, 0f, 1f);
            case EXTERMINATE -> 1f;
            default -> 0f;
        };
    }

    public float getChamberFill(final float partialTick) {
        if (this.shellState == ShellStates.CREATE && this.lastTransferTick >= 0L) {
            return 1f - smoothstep01(this.getDrainProgress(partialTick));
        }
        return this.fluidFill < 0f ? this.targetFluidFill() : Mth.lerp(partialTick, this.previousFluidFill, this.fluidFill);
    }

    public float getDrainProgress(final float partialTick) {
        return this.shellState == ShellStates.CREATE && this.lastTransferTick >= 0L
            ? Mth.lerp(partialTick, (float) this.previousDrainTicks, this.drainTicks) / TRANSFER_DRAIN_TICKS
            : 1f;
    }

    public float getMaskSeparation(final float partialTick) {
        return this.shellState == ShellStates.EXTERMINATE
            ? 1f - Mth.lerp(partialTick, (float) this.previousMaskFitTicks, this.maskFitTicks) / MASK_FIT_TICKS
            : 1f;
    }

    private void updateMaintenance() {
        final int cost = Config.SHELL_FORGE_ENERGY_USAGE_MAINTENANCE.get();
        final boolean powered = cost == 0 || this.energyStorage.extractEnergy(cost, true) == cost;
        if (powered && cost > 0) {
            this.energyStorage.extractEnergy(cost, false);
        }
        final ShellStates state = !powered && Config.SHELL_FORGE_DECAY_ENABLED.get() ? ShellStates.DECAYING
            : this.shellPercentage < 100 ? ShellStates.RECOVERING : ShellStates.EXTERMINATE;
        if (state != this.shellState) {
            this.setShellState(state);
            if (this.shellPercentage == 100) {
                this.updateShellCreationProgressOnClient(state == ShellStates.EXTERMINATE ? 100 : 99);
            }
        }
    }

    private void setShellState(final ShellStates state) {
        this.shellState = state;
        this.shellPercentageCooldownTick = 0;
        this.setChanged();
    }

    private void updateShellCreationProgressOnClient(final int progress) {
        if (this.level instanceof ServerLevel serverLevel) {
            ShellSavedData.getShellData(serverLevel).updateShellCreationProgress(this.playerUuid, this.shellUuid, progress);
        }
    }

    public boolean transferPlayerTo(final Player player, final Runnable afterTeleport) {
        if (this.shellState == ShellStates.EXTERMINATE && this.playerUuid.equals(player.getUUID())
            && this.level instanceof ServerLevel serverLevel && player instanceof ShellPlayer shellPlayer
            && !hasPlayerInside(this.getBlockPos(), serverLevel)) {
            final ShellState shellState = ShellSavedData.getShellData(serverLevel).get(this.playerUuid, this.shellUuid);
            if (shellState != null && shellPlayer.playershells$applyData(shellState.playerData(), shellState.shellForgePos(), afterTeleport)) {
                this.playerState = PlayerStates.TRANSFERRED;
                this.lastTransferTick = serverLevel.getGameTime();
                ShellSavedData.getShellData(serverLevel).consume(this.playerUuid, this.shellUuid);
                this.clearShell();

                setValue(this.getBlockState(), this.level, this.getBlockPos(), new BoolProperty(OPEN, true));
                return true;
            }
        }
        return false;
    }

    public void transferPlayerFrom(final Player player, final CompoundTag playerData) {
        this.setPlayerUuid(player.getUUID());
        this.shellState = ShellStates.EXTERMINATE;
        this.playerState = PlayerStates.NONE;
        this.shellPercentage = 100;
        this.shellPercentageCooldownTick = 0;
        this.lastTransferTick = -1L;
        if (this.level instanceof ServerLevel serverLevel && player instanceof ShellPlayer) {
            this.shellUuid = UUID.randomUUID();
            ShellSavedData.getShellData(serverLevel).add(this.playerUuid, new ShellState(
                this.shellUuid, new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), this.level.dimension().identifier()),
                playerData, 100));
        }
        this.setChanged();
    }

    public void createShell() {
        if (!this.canCreateShell()) {
            return;
        }

        final UUID playerUUID = Objects.requireNonNull(this.inventoryHandler.getItem(0).get(ModDataComponentTypes.OWNER_PLAYER.get())).playerUUID();

        this.energyStorage.extractEnergy(Config.SHELL_FORGE_ENERGY_USAGE_CREATION.get(), false);
        this.inventoryHandler.removeItem(0, SHELL_FORGE_DNA_AMOUNT.get());
        this.lastTransferTick = -1L;
        this.setPlayerUuid(playerUUID);
        this.setShellState(ShellStates.CREATING);
        if (this.level instanceof ServerLevel serverLevel) {
            this.shellUuid = UUID.randomUUID();
            ShellSavedData.getShellData(serverLevel).add(this.playerUuid, new ShellState(
                this.shellUuid, new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), this.level.dimension().identifier()),
                this.createFreshPlayerData(serverLevel), 0));
        }
        this.setChanged();
    }

    private void clearShell() {
        this.setPlayerUuid(EMPTY_UUID);
        this.shellPercentage = 0;
        this.shellState = ShellStates.CREATE;
        this.shellUuid = EMPTY_UUID;
        this.shellPercentageCooldownTick = 0;
        this.setChanged();
    }

    private CompoundTag createFreshPlayerData(final ServerLevel serverLevel) {
        final GameType gameMode = serverLevel.getServer().getDefaultGameType();
        final Player body = new Player(serverLevel, new GameProfile(this.playerUuid, "")) {
            @Override
            public GameType gameMode() {
                return gameMode;
            }
        };
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, serverLevel.registryAccess());
        body.saveWithoutId(output);
        output.discard(Entity.TAG_UUID);
        output.store("playerGameType", GameType.LEGACY_ID_CODEC, gameMode);
        return output.buildResult();
    }

    public boolean canCreateShell() {
        final int energyNeeded = Config.SHELL_FORGE_ENERGY_USAGE_CREATION.get();

        final ItemStack stack = this.inventoryHandler.getItem(0);
        if (stack.isEmpty() || !stack.is(ModItems.DNA) || !stack.has(ModDataComponentTypes.OWNER_PLAYER.get())) {
            return false;
        }

        if (stack.getCount() < SHELL_FORGE_DNA_AMOUNT.get()) {
            return false;
        }

        final int simulatedExtract = this.energyStorage.extractEnergy(energyNeeded, true);
        if (simulatedExtract < energyNeeded) {
            return false;
        }

        return true;
    }

    private void updatePlayerStateAndOpened(final BlockState state, final BlockPos pos) {
        if (this.level == null) {
            return;
        }

        final boolean occupied = hasPlayerInside(pos, this.level);
        if (this.playerState == PlayerStates.GOING_IN) {
            if (!isOpen(state)) {
                setValue(state, this.level, pos, new BoolProperty(OPEN, true));
            } else if (this.level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, false) != null) {
                this.setPlayerState(PlayerStates.INSIDE);
            }
        } else if (occupied && !isOpen(state) && (this.shellState == ShellStates.CREATE || this.playerState == PlayerStates.TRANSFERRED)) {
            setValue(state, this.level, pos, new BoolProperty(OPEN, true));
        } else if (!occupied) {
            this.setPlayerState(PlayerStates.NONE);
            if (isOpen(state)) {
                setValue(state, this.level, pos, new BoolProperty(OPEN, false));
            }
        }
    }

    public void shellButtonPressed(final UUID confirmedShell) {
        if (this.shellState == ShellStates.CREATE && confirmedShell.equals(EMPTY_UUID)) {
            this.createShell();
        } else if (this.shellState == ShellStates.EXTERMINATE && this.shellUuid.equals(confirmedShell)) {
            this.setShellState(ShellStates.EXTERMINATING);
            this.updateShellCreationProgressOnClient(99);
        }
    }

    public boolean restoreShell(final UUID owner, final UUID restoredShellUuid) {
        if (!(this.level instanceof ServerLevel level) || this.shellState != ShellStates.CREATE || this.playerState != PlayerStates.NONE
            || hasPlayerInside(this.getBlockPos(), level)) {
            return false;
        }

        final ShellSavedData data = ShellSavedData.getShellData(level);
        final PositionReference position = new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), level.dimension().identifier());
        if (!data.restore(owner, restoredShellUuid, position)) {
            return false;
        }
        this.playerUuid = owner;
        this.shellUuid = restoredShellUuid;
        this.shellPercentage = 100;
        this.lastTransferTick = -1L;
        this.setShellState(ShellStates.EXTERMINATE);
        return true;
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        final ShellStates oldShellState = this.shellState;
        final UUID oldShellUuid = this.shellUuid;
        this.inventoryHandler.load(input.childOrEmpty("inv"));
        this.energyStorage.set(input.getIntOr("energy", 0));
        this.playerUuid = input.read("playerUuid", UUIDUtil.CODEC).orElse(EMPTY_UUID);
        this.playerState = PlayerStates.values()[Mth.clamp(input.getIntOr("playerState", 0), 0, PlayerStates.values().length - 1)];
        this.shellUuid = input.read("shellUuid", UUIDUtil.CODEC).orElse(EMPTY_UUID);
        this.shellState = ShellStates.values()[Mth.clamp(input.getIntOr("shellState", 0), 0, ShellStates.values().length - 1)];
        this.shellPercentage = input.getIntOr("shellPercentage", 0);
        this.shellPercentageCooldownTick = input.getIntOr("shellPercentageCooldownTick", 0);
        this.lastTransferTick = input.getLongOr("lastTransferTick", -1L);
        if (this.level != null && this.level.isClientSide()) {
            if (this.shellState != ShellStates.EXTERMINATE || !this.shellUuid.equals(oldShellUuid)) {
                this.previousMaskFitTicks = MASK_FIT_TICKS;
                this.maskFitTicks = MASK_FIT_TICKS;
            } else if (oldShellState == ShellStates.CREATING || oldShellState == ShellStates.RECOVERING) {
                this.previousMaskFitTicks = 0;
                this.maskFitTicks = 0;
            }
        }
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        this.inventoryHandler.save(output.child("inv"));
        output.putInt("energy", (int) this.energyStorage.getAmountAsLong());
        output.store("playerUuid", UUIDUtil.CODEC, this.playerUuid);
        output.putInt("playerState", this.playerState.ordinal());
        output.store("shellUuid", UUIDUtil.CODEC, this.shellUuid);
        output.putInt("shellState", this.shellState.ordinal());
        output.putInt("shellPercentage", this.shellPercentage);
        output.putInt("shellPercentageCooldownTick", this.shellPercentageCooldownTick);
        if (this.lastTransferTick >= 0L) {
            output.putLong("lastTransferTick", this.lastTransferTick);
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void setPlayerUuid(final UUID uuid) {
        this.playerUuid = uuid;
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public UUID getShellUuid() {
        return this.shellUuid;
    }

    public boolean canRenameShell(final UUID owner, final UUID shell) {
        return this.shellState != ShellStates.CREATE && this.shellState != ShellStates.EXTERMINATING
            && !shell.equals(EMPTY_UUID) && this.shellUuid.equals(shell) && this.playerUuid.equals(owner);
    }

    public ShellStates getShellState() {
        return this.shellState;
    }

    public void setPlayerState(final PlayerStates playerState) {
        if (this.playerState != playerState) {
            this.playerState = playerState;
            this.setChanged();
        }
    }

    public PlayerStates getPlayerState() {
        return this.playerState;
    }

    public int getShellPercentage() {
        return this.shellPercentage;
    }

    public float getAnimPrevProgress() {
        return this.animPrevProgress;
    }

    public float getAnimProgress() {
        return this.animProgress;
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.SHELL_FORGE.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new ShellForgeContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    public enum ShellStates {
        CREATE,
        CREATING,
        EXTERMINATE,
        EXTERMINATING,
        DECAYING,
        RECOVERING
    }

    public enum PlayerStates {
        NONE,
        GOING_IN,
        INSIDE,
        GOING_OUT,
        TRANSFERRED
    }
}
