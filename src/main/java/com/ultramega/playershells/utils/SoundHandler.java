package com.ultramega.playershells.utils;

import com.ultramega.playershells.blockentities.ShellForgeBlockEntity;
import com.ultramega.playershells.blockentities.ShellForgeBlockEntity.ShellStates;
import com.ultramega.playershells.registry.ModSoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import static com.ultramega.playershells.utils.MathUtils.smoothstep01;

public final class SoundHandler {
    private static final Map<BlockPos, ExterminationSound> EXTERMINATIONS = new HashMap<>();

    private SoundHandler() {
    }

    public static void playExtermination(final ShellForgeBlockEntity forge, final RandomSource random) {
        final BlockPos pos = forge.getBlockPos();
        final Minecraft client = Minecraft.getInstance();
        if (EXTERMINATIONS.containsKey(pos) || client.gameRenderer.getMainCamera().position().distanceToSqr(pos.getCenter()) > 24 * 24
            || client.options.getFinalSoundSourceVolume(SoundSource.BLOCKS) <= 0f) {
            return;
        }

        final var sound = new ExterminationSound(pos, forge.getShellUuid(), random);
        if (client.getSoundManager().play(sound) != SoundEngine.PlayResult.NOT_STARTED) {
            EXTERMINATIONS.put(pos, sound);
        }
    }

    public static void tick() {
        final var sounds = Minecraft.getInstance().getSoundManager();
        EXTERMINATIONS.values().removeIf(sound -> sound.isStopped() || !sounds.isActive(sound));
    }

    public static void stopAllBlockSounds() {
        final var sounds = Minecraft.getInstance().getSoundManager();
        EXTERMINATIONS.values().forEach(sounds::stop);
        EXTERMINATIONS.clear();
    }

    private static final class ExterminationSound extends AbstractTickableSoundInstance {
        private static final int FADE_IN_TICKS = 10;
        private static final float FADE_OUT_PERCENT = 16f;
        private static final float MAX_VOLUME = 0.9f;

        private final BlockPos forgePos;
        private final UUID shellUuid;
        private int age;

        private ExterminationSound(final BlockPos forgePos, final UUID shellUuid, final RandomSource random) {
            super(ModSoundEvents.FLAMETHROWER.get(), SoundSource.BLOCKS, random);
            this.forgePos = forgePos;
            this.shellUuid = shellUuid;
            this.x = forgePos.getX() + 0.5;
            this.y = forgePos.getY() + 1.0;
            this.z = forgePos.getZ() + 0.5;
            this.looping = true;
            this.volume = 0f;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            final var level = Minecraft.getInstance().level;
            if (level != null && level.getBlockEntity(this.forgePos) instanceof ShellForgeBlockEntity forge
                && forge.getShellState() == ShellStates.EXTERMINATING && forge.getShellUuid().equals(this.shellUuid)) {
                final float envelope = Math.min(++this.age / (float) FADE_IN_TICKS, forge.getShellPercentage() / FADE_OUT_PERCENT);
                this.volume = Mth.approach(this.volume, MAX_VOLUME * smoothstep01(envelope), 0.14f);
            } else {
                // Release gradually even if a chunk unload or block removal cuts the cycle short.
                this.volume = Mth.approach(this.volume, 0f, MAX_VOLUME / FADE_OUT_PERCENT);
                if (this.volume <= 0f) {
                    this.stop();
                }
            }
        }
    }
}
