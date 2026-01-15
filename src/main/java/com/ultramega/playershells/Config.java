package com.ultramega.playershells;

import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue SHELL_FORGE_ENERGY_CAPACITY = BUILDER
            .comment("Maximum energy storage capacity of the Shell Forge")
            .defineInRange("shellForgeEnergyCapacity", 1_000_000, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue SHELL_FORGE_ENERGY_USAGE_CREATION = BUILDER
        .comment("Energy consumed by the Shell Forge to create a single Shell (must not exceed the energy capacity)")
        .defineInRange("shellForgeEnergyUsageCreation", 1_000_000, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue SHELL_FORGE_ENERGY_USAGE_MAINTENANCE = BUILDER
        .comment("Energy consumed by the Shell Forge per tick to maintain an alive Shell (must not exceed the energy capacity)"
            + "\nIf you want to disable decaying, simply set this value to 0")
        .defineInRange("shellForgeEnergyUsageMaintenance", 1_000, 0, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue SHELL_FORGE_CREATION_COOLDOWN = BUILDER
        .comment("Time required to progress by 1% during Shell creation (in ticks)")
        .defineInRange("shellForgeCreationCooldown", 20, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue SHELL_FORGE_DECAY_COOLDOWN = BUILDER
        .comment("Time required to progress by 1% during Shell decay (in ticks)")
        .defineInRange("shellForgeDecayCooldown", 10, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue CENTRIFUGE_ENERGY_CAPACITY = BUILDER
        .comment("Maximum energy storage capacity of the Centrifuge")
        .defineInRange("centrifugeEnergyCapacity", 100_000, 1, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue CENTRIFUGE_ENERGY_USAGE = BUILDER
        .comment("Energy consumed by the Centrifuge per tick")
        .defineInRange("centrifugeEnergyUsage", 3_000, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue TRANSFER_INTO_SHELL_AFTER_DEATH = BUILDER
        .comment("If enabled, the player will respawn in the nearest shell after death, even in hardcore mode. When disabled, death in hardcore mode remains permanent")
        .define("transferIntoShellAfterDeath", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
