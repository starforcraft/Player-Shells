package com.ultramega.playershells.utils;

import net.minecraftforge.energy.EnergyStorage;

public class ObservableEnergyStorage extends EnergyStorage {
    public ObservableEnergyStorage(final int capacity) {
        super(capacity);
    }

    public ObservableEnergyStorage(final int capacity, final int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public ObservableEnergyStorage(final int capacity, final int maxReceive, final int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public ObservableEnergyStorage(final int capacity, final int maxReceive, final int maxExtract, final int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        final int received = super.receiveEnergy(toReceive, simulate);
        if (!simulate && received > 0) {
            this.onEnergyChanged();
        }
        return received;
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        final int extracted = super.extractEnergy(toExtract, simulate);
        if (!simulate && extracted > 0) {
            this.onEnergyChanged();
        }
        return extracted;
    }

    public void onEnergyChanged() {
    }
}
