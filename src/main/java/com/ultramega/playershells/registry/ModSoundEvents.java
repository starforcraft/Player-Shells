package com.ultramega.playershells.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER = SOUND_EVENTS.register("flamethrower", () ->
        SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "flamethrower")));

    private ModSoundEvents() {
    }
}
