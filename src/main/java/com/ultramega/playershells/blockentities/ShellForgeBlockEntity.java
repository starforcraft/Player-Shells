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

public class ShellForgeBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    private static final float OPEN_SPEED = 0.12f;

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

    private float animProgress;
    private float animPrevProgress;

    public ShellForgeBlockEntity(final BlockPos pos,
                                 final BlockState blockState) {
        super(ModBlockEntityTypes.SHELL_FORGE.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final ShellForgeBlockEntity blockEntity) {
        blockEntity.updatePlayerStateAndOpened(state, pos);

        blockEntity.drainEnergyPassive();

        // Decay/Exterminating shell stuff
        if (blockEntity.shellState == ShellStates.DECAYING || blockEntity.shellState == ShellStates.EXTERMINATING) {
            if (blockEntity.shellPercentage <= 0) {
                blockEntity.exterminateShell();
                return;
            }
            if (blockEntity.shellState == ShellStates.DECAYING && blockEntity.shellPercentageCooldownTick++ <= Config.SHELL_FORGE_DECAY_COOLDOWN.get()) {
                blockEntity.setChanged();
                return;
            }
            blockEntity.shellPercentageCooldownTick = 0;
            blockEntity.shellPercentage -= 1;
            blockEntity.updateShellCreationProgressOnClient();
            blockEntity.setChanged();
        }

        // Create shell stuff
        if (blockEntity.shellState != ShellStates.CREATING) {
            return;
        }
        if (blockEntity.shellPercentage == 100) {
            blockEntity.shellState = ShellStates.EXTERMINATE;
            blockEntity.setChanged();
            return;
        }

        if (blockEntity.shellPercentageCooldownTick++ <= Config.SHELL_FORGE_CREATION_COOLDOWN.get()) {
            blockEntity.setChanged();
            return;
        }

        blockEntity.shellPercentageCooldownTick = 0;
        blockEntity.shellPercentage += 1;
        blockEntity.updateShellCreationProgressOnClient();
        blockEntity.setChanged();
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

    private void drainEnergyPassive() { //TODO: decying doesn't stop
        if (this.shellState == ShellStates.EXTERMINATE) {
            final int simulateExtract = this.energyStorage.extractEnergy(Config.SHELL_FORGE_ENERGY_USAGE_MAINTENANCE.get(), true);
            if (simulateExtract != Config.SHELL_FORGE_ENERGY_USAGE_MAINTENANCE.get()) {
                this.shellState = ShellStates.DECAYING;
                this.setChanged();
            } else {
                this.energyStorage.extractEnergy(Config.SHELL_FORGE_ENERGY_USAGE_MAINTENANCE.get(), false);
                this.setChanged();
            }
        }
    }

    private void updateShellCreationProgressOnClient() {
        if (this.level instanceof ServerLevel serverLevel) {
            ShellSavedData.getShellData(serverLevel).updateShellCreationProgress(this.playerUuid, this.shellUuid, this.shellPercentage);
        }
    }

    public boolean transferPlayerTo(final Player player, final Runnable afterTeleport) {
        //TODO decrease water level, remove mask on client
        if (this.shellState == ShellStates.EXTERMINATE && this.playerUuid.equals(player.getUUID())
            && this.level instanceof ServerLevel serverLevel && player instanceof ShellPlayer shellPlayer) {
            final ShellState shellState = ShellSavedData.getShellData(serverLevel).get(this.playerUuid, this.shellUuid);
            if (shellState != null && shellPlayer.playershells$applyData(shellState.playerData(), shellState.shellForgePos(), afterTeleport)) {
                this.playerState = PlayerStates.TRANSFERRED;
                this.exterminateShell();

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
        this.setPlayerUuid(playerUUID);
        this.shellState = ShellStates.CREATING;
        if (this.level instanceof ServerLevel serverLevel) {
            this.shellUuid = UUID.randomUUID();
            ShellSavedData.getShellData(serverLevel).add(this.playerUuid, new ShellState(
                this.shellUuid, new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), this.level.dimension().identifier()),
                this.createFreshPlayerData(serverLevel), 0));
        }
        this.setChanged();
    }

    public void exterminateShell() {
        this.setPlayerUuid(EMPTY_UUID);
        this.shellPercentage = 0;
        this.shellState = ShellStates.CREATE;
        this.shellUuid = EMPTY_UUID;
        if (this.level instanceof ServerLevel serverLevel) {
            ShellSavedData.getShellData(serverLevel).validateShellData(serverLevel);
        }
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

        if (!isOpen(state) && (this.playerState == PlayerStates.GOING_IN || this.playerState == PlayerStates.TRANSFERRED)) {
            setValue(state, this.level, pos, new BoolProperty(OPEN, true));
        } else if (this.playerState == PlayerStates.GOING_IN) {
            if (hasPlayerInside(pos, this.level)) {
                this.playerState = PlayerStates.INSIDE;
                this.setChanged();
            }
        } else if (!hasPlayerInside(pos, this.level)) {
            this.playerState = PlayerStates.NONE;
            this.setChanged();
            setValue(state, this.level, pos, new BoolProperty(OPEN, false));
        }
    }

    public void shellButtonPressed() {
        if (this.shellState == ShellStates.CREATE) {
            this.createShell();
        } else if (this.shellState == ShellStates.EXTERMINATE) {
            this.shellState = ShellStates.EXTERMINATING;
            this.setChanged();
        }
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.inventoryHandler.load(input.childOrEmpty("inv"));
        this.energyStorage.set(input.getIntOr("energy", 0));
        this.playerUuid = input.read("playerUuid", UUIDUtil.CODEC).orElse(EMPTY_UUID);
        this.playerState = PlayerStates.values()[Mth.clamp(input.getIntOr("playerState", 0), 0, PlayerStates.values().length - 1)];
        this.shellUuid = input.read("shellUuid", UUIDUtil.CODEC).orElse(EMPTY_UUID);
        this.shellState = ShellStates.values()[Mth.clamp(input.getIntOr("shellState", 0), 0, ShellStates.values().length - 1)];
        this.shellPercentage = input.getIntOr("shellPercentage", 0);
        this.shellPercentageCooldownTick = input.getIntOr("shellPercentageCooldownTick", 0);
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

    public ShellStates getShellState() {
        return this.shellState;
    }

    public void setPlayerState(final PlayerStates playerState) {
        this.playerState = playerState;
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
        DECAYING
    }

    public enum PlayerStates {
        NONE,
        GOING_IN,
        INSIDE,
        GOING_OUT,
        TRANSFERRED
    }
}
