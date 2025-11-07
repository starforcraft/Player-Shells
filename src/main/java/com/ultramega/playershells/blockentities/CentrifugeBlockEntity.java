package com.ultramega.playershells.blockentities;

import com.ultramega.playershells.Config;
import com.ultramega.playershells.container.CentrifugeContainerMenu;
import com.ultramega.playershells.registry.ModBlockEntityTypes;
import com.ultramega.playershells.registry.ModBlocks;
import com.ultramega.playershells.registry.ModDataComponentTypes;
import com.ultramega.playershells.registry.ModItems;
import com.ultramega.playershells.utils.ObservableEnergyStorage;
import com.ultramega.playershells.utils.OwnerData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.neoforged.neoforge.items.ItemStackHandler;

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
            final OwnerData ownerData = syringeStack.get(ModDataComponentTypes.OWNER_PLAYER.get());
            syringeStack.shrink(1);

            final ItemStack dnaStack = ModItems.DNA.get().getDefaultInstance();
            dnaStack.setCount(blockEntity.level.random.nextInt(1, 6));
            dnaStack.set(ModDataComponentTypes.OWNER_PLAYER.get(), ownerData);
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
            final OwnerData inputOwnerData = inputStack.get(ModDataComponentTypes.OWNER_PLAYER.get());
            final OwnerData resultOwnerData = resultStack.get(ModDataComponentTypes.OWNER_PLAYER.get());
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
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("inv")) {
            this.inventoryHandler.deserializeNBT(registries, tag.getCompound("inv"));
        }
        if (tag.contains("energy")) {
            this.energyStorage.deserializeNBT(registries, tag.get("energy"));
        }
        this.processingProgress = tag.getInt("processingProgress");
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("inv", this.inventoryHandler.serializeNBT(registries));
        tag.put("energy", this.energyStorage.serializeNBT(registries));
        tag.putInt("processingProgress", this.processingProgress);
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
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
