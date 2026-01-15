package com.ultramega.playershells.blockentities;

import com.ultramega.playershells.Config;
import com.ultramega.playershells.container.CentrifugeContainerMenu;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModBlocks;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.utils.ObservableEnergyStorage;
import com.ultramega.playershells.utils.OwnerData;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class CentrifugeBlockEntity extends BlockEntity implements MenuProvider, Nameable {
    public static final int PROCESSING_TOTAL_TIME = 200;

    public final ItemStackHandler inventoryHandler = new ItemStackHandler(2) {
        @Override
        public void onContentsChanged(final int slot) {
            CentrifugeBlockEntity.super.setChanged();
        }
    };
    public final ObservableEnergyStorage energyStorage = new ObservableEnergyStorage(Config.CENTRIFUGE_ENERGY_CAPACITY.get()) {
        @Override
        public void onEnergyChanged() {
            CentrifugeBlockEntity.this.setChanged();
        }
    };

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> this.energyStorage);
    private LazyOptional<IItemHandler> itemHandlerCapability = LazyOptional.of(() -> this.inventoryHandler);

    private int processingProgress = 0;

    public CentrifugeBlockEntity(final BlockPos pos,
                                 final BlockState blockState) {
        super(ModBlockEntityTypes.CENTRIFUGE.get(), pos, blockState);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final CentrifugeBlockEntity blockEntity) {
        if (blockEntity.cannotOperate() || blockEntity.level == null) {
            return;
        }

        blockEntity.processingProgress++;
        if (blockEntity.processingProgress >= PROCESSING_TOTAL_TIME) {
            blockEntity.processingProgress = 0;

            final ItemStack syringeStack = blockEntity.inventoryHandler.getStackInSlot(0);
            final OwnerData ownerData = OwnerData.getFromStack(syringeStack);
            syringeStack.shrink(1);

            final ItemStack dnaStack = ModItems.DNA.get().getDefaultInstance();
            dnaStack.setCount(blockEntity.level.random.nextInt(1, 6));
            if (ownerData != null) {
                OwnerData.setOnStack(dnaStack, ownerData);
            }
            blockEntity.inventoryHandler.insertItem(1, dnaStack, false);

            blockEntity.energyStorage.extractEnergy(Config.CENTRIFUGE_ENERGY_USAGE.get(), false);
        }

        blockEntity.setChanged();
    }

    private boolean cannotOperate() {
        final ItemStack inputStack = this.inventoryHandler.getStackInSlot(0);
        final ItemStack resultStack = this.inventoryHandler.getStackInSlot(1);
        boolean isNotSameUUID = true;
        if (!inputStack.isEmpty() && !resultStack.isEmpty()) {
            final OwnerData inputOwnerData = OwnerData.getFromStack(inputStack);
            final OwnerData resultOwnerData = OwnerData.getFromStack(resultStack);
            if (inputOwnerData != null && resultOwnerData != null && !inputOwnerData.playerUUID().equals(resultOwnerData.playerUUID())) {
                isNotSameUUID = false;
            }
        }
        return this.energyStorage.getEnergyStored() <= Config.CENTRIFUGE_ENERGY_USAGE.get()
            || inputStack.isEmpty()
            || resultStack.getCount() >= resultStack.getMaxStackSize()
            || !isNotSameUUID;
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
        this.processingProgress = tag.getInt("processingProgress");
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put("inv", this.inventoryHandler.serializeNBT());
        tag.put("energy", this.energyStorage.serializeNBT());
        tag.putInt("processingProgress", this.processingProgress);
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

    public int getProcessingProgress() {
        return this.processingProgress;
    }

    @Override
    public Component getDisplayName() {
        return this.getName();
    }

    @Override
    public Component getName() {
        return Component.translatable(ModBlocks.CENTRIFUGE.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
        return new CentrifugeContainerMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }
}
