package com.ultramega.playershells.utils;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ObservableEnergyStorage extends SimpleEnergyHandler {
    public ObservableEnergyStorage(final int capacity) {
        super(capacity);
    }

    public int extractEnergy(final int amount, final boolean simulate) {
        try (final Transaction transaction = Transaction.openRoot()) {
            final int extracted = this.extract(amount, transaction);
            if (!simulate) {
                transaction.commit();
            }
            return extracted;
        }
    }

    @Override
    protected void onEnergyChanged(final int previousAmount) {
        this.onEnergyChanged();
    }

    public void onEnergyChanged() {
    }
}
