package com.ultramega.playershells.blockentities;

import com.ultramega.playershells.Config;
import com.ultramega.playershells.blocks.AbstractMultiblockBlock;
import com.ultramega.playershells.blocks.ShellForgeBlock.BoolProperty;
import com.ultramega.playershells.container.ShellForgeContainerMenu;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModBlocks;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.registry.ModSoundEvents;
import com.ultramega.playershells.storage.ShellSavedData;
import com.ultramega.playershells.storage.ShellState;
import com.ultramega.playershells.utils.ObservableEnergyStorage;
import com.ultramega.playershells.utils.OwnerData;
import com.ultramega.playershells.utils.PositionReference;
import com.ultramega.playershells.utils.ShellPlayer;
import com.ultramega.playershells.utils.SoundHandler;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import static com.ultramega.playershells.blocks.AbstractMultiblockBlock.FACING;
import static com.ultramega.playershells.blocks.ShellForgeBlock.OPEN;
import static com.ultramega.playershells.blocks.ShellForgeBlock.isOpen;
import static com.ultramega.playershells.blocks.ShellForgeBlock.movePlayerInside;
import static com.ultramega.playershells.blocks.ShellForgeBlock.movePlayerTo;
import static com.ultramega.playershells.blocks.ShellForgeBlock.setValue;
import static com.ultramega.playershells.utils.MathUtils.EMPTY_UUID;
import static com.ultramega.playershells.utils.MathUtils.hasPlayerInside;
import static com.ultramega.playershells.utils.MathUtils.isPlayerInFront;

public class ShellForgeBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    private static final float OPEN_SPEED = 0.12f;

    public final ItemStackHandler inventoryHandler = new ItemStackHandler(1) {
        @Override
        public void onContentsChanged(final int slot) {
            ShellForgeBlockEntity.super.setChanged();
        }
    };
    public final ObservableEnergyStorage energyStorage = new ObservableEnergyStorage(Config.SHELL_FORGE_ENERGY_CAPACITY.get()) {
        @Override
        public void onEnergyChanged() {
            ShellForgeBlockEntity.this.setChanged();
        }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> this.energyStorage);
    private LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this.inventoryHandler);

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

    @OnlyIn(Dist.CLIENT)
    public static void clientTick(final Level level, final BlockPos pos, final BlockState state, final ShellForgeBlockEntity blockEntity) {
        blockEntity.animPrevProgress = blockEntity.animProgress;
        final float target = state.getValue(OPEN) ? 1.0f : 0.0f;

        if (blockEntity.animProgress < target) {
            blockEntity.animProgress = Math.min(target, blockEntity.animProgress + OPEN_SPEED);
        } else if (blockEntity.animProgress > target) {
            blockEntity.animProgress = Math.max(target, blockEntity.animProgress - OPEN_SPEED);
        }

        if (blockEntity.shellState == ShellStates.EXTERMINATING) {
            if (level.random.nextDouble() < 0.1) {
                SoundHandler.startBlockSound(ModSoundEvents.FLAMETHROWER.get(), SoundSource.BLOCKS, 1.5F, 1.0F, level.random, pos);
            }
        } else {
            SoundHandler.stopAllBlockSounds(pos);
        }

        final Player player = Minecraft.getInstance().player;
        if (player == null || !state.getValue(OPEN)) {
            return;
        }

        if (blockEntity.level != null && !isPlayerInFront(pos, blockEntity.level, player.getUUID(), state.getValue(FACING))) {
            return;
        }
        if (blockEntity.playerState == PlayerStates.GOING_IN || blockEntity.playerState == PlayerStates.INSIDE) {
            movePlayerInside(player, pos, state.getValue(FACING));
        } else if (blockEntity.playerState == PlayerStates.GOING_OUT) {
            movePlayerTo(player, pos.relative(state.getValue(FACING)), 0);
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

    public void transferPlayerTo(final Player player) {
        //TODO decrease water level, remove mask on client
        if (this.level instanceof ServerLevel serverLevel && player instanceof ShellPlayer shellPlayer) {
            final ShellState shellState = ShellSavedData.getShellData(serverLevel).get(this.playerUuid, this.shellUuid);
            if (shellState != null) {
                shellPlayer.playershells$applyData(shellState.playerData(), shellState.shellForgePos());
                this.playerState = PlayerStates.TRANSFERRED;
                this.exterminateShell();

                setValue(this.getBlockState(), this.level, this.getBlockPos(), new BoolProperty(OPEN, true));
            }
        }
    }

    public void transferPlayerFrom(final Player player) {
        this.setPlayerUuid(player.getUUID());
        this.shellState = ShellStates.EXTERMINATE;
        this.playerState = PlayerStates.NONE;
        this.shellPercentage = 100;
        if (this.level instanceof ServerLevel serverLevel && player instanceof ShellPlayer shellPlayer) {
            this.shellUuid = UUID.randomUUID();
            ShellSavedData.getShellData(serverLevel).add(this.playerUuid, new ShellState(
                this.shellUuid, new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), this.level.dimension().location()),
                shellPlayer.playershells$getData(), 100));
        }
        this.setChanged();
    }

    public void createShell() {
        if (!this.canCreateShell()) {
            return;
        }

        final OwnerData ownerData = OwnerData.getFromStack(this.inventoryHandler.getStackInSlot(0));
        final UUID playerUUID = Objects.requireNonNull(ownerData, "OwnerData must exist when canCreateShell() is true").playerUUID();

        this.energyStorage.extractEnergy(Config.SHELL_FORGE_ENERGY_USAGE_CREATION.get(), false);
        this.inventoryHandler.extractItem(0, 64, false);
        this.setPlayerUuid(playerUUID);
        this.shellState = ShellStates.CREATING;
        if (this.level instanceof ServerLevel serverLevel) {
            this.shellUuid = UUID.randomUUID();
            ShellSavedData.getShellData(serverLevel).add(this.playerUuid, new ShellState(
                this.shellUuid, new PositionReference(this.getBlockPos(), this.getBlockState().getValue(FACING), this.level.dimension().location()),
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
        return FakePlayerFactory.get(serverLevel, new GameProfile(UUID.randomUUID(), "PlayerShells"))
            .saveWithoutId(new CompoundTag());
    }

    public boolean canCreateShell() {
        final int energyNeeded = Config.SHELL_FORGE_ENERGY_USAGE_CREATION.get();

        final ItemStack stack = this.inventoryHandler.getStackInSlot(0);
        if (stack.isEmpty() || !stack.is(ModItems.DNA.get()) || OwnerData.getFromStack(stack) == null) {
            return false;
        }

        final boolean canExtractStack = this.inventoryHandler.extractItem(0, 64, true).getCount() >= 64;
        if (!canExtractStack) {
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
    public void load(final CompoundTag tag) {
        super.load(tag);

        if (tag.contains("inv")) {
            this.inventoryHandler.deserializeNBT(tag.getCompound("inv"));
        }
        if (tag.contains("energy")) {
            this.energyStorage.deserializeNBT(tag.get("energy"));
        }
        if (tag.contains("playerUuid")) {
            this.playerUuid = tag.getUUID("playerUuid");
        }
        if (tag.contains("playerState")) {
            this.playerState = PlayerStates.values()[tag.getInt("playerState")];
        }
        if (tag.contains("shellUuid")) {
            this.shellUuid = tag.getUUID("shellUuid");
        }
        if (tag.contains("shellState")) {
            this.shellState = ShellStates.values()[tag.getInt("shellState")];
        }
        if (tag.contains("shellPercentage")) {
            this.shellPercentage = tag.getInt("shellPercentage");
        }
        if (tag.contains("shellPercentageCooldownTick")) {
            this.shellPercentageCooldownTick = tag.getInt("shellPercentageCooldownTick");
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put("inv", this.inventoryHandler.serializeNBT());
        tag.put("energy", this.energyStorage.serializeNBT());
        tag.putUUID("playerUuid", this.playerUuid);
        tag.putInt("playerState", this.playerState.ordinal());
        tag.putUUID("shellUuid", this.shellUuid);
        tag.putInt("shellState", this.shellState.ordinal());
        tag.putInt("shellPercentage", this.shellPercentage);
        tag.putInt("shellPercentageCooldownTick", this.shellPercentageCooldownTick);
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos()).expandTowards(0, AbstractMultiblockBlock.isBottomHalf(this.getBlockState()) ? 1 : -1, 0);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.energyCapability.invalidate();
        this.itemHandlerCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.energyCapability = LazyOptional.of(() -> this.energyStorage);
        this.itemHandlerCapability = LazyOptional.of(() -> this.inventoryHandler);
    }

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> cap, @Nullable final Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return this.energyCapability.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return this.itemHandlerCapability.cast();
        }
        return super.getCapability(cap, side);
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
