package com.ultramega.playershells.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<SoundEvent> FLAMETHROWER = SOUND_EVENTS.register("flamethrower", () ->
        SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "flamethrower")));

    private ModSoundEvents() {
    }
}
