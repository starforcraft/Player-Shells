package com.ultramega.playershells.entities;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

public class ShellEntity extends RemotePlayer {
    private int lastClientShellDataVersion = Integer.MIN_VALUE;

    public ShellEntity(final ClientLevel clientLevel, final GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    public int getLastClientShellDataVersion() {
        return this.lastClientShellDataVersion;
    }

    public void setLastClientShellDataVersion(final int lastClientShellDataVersion) {
        this.lastClientShellDataVersion = lastClientShellDataVersion;
    }

    @Override
    public boolean isCreative() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}
