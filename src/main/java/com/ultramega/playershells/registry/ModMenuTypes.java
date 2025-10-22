package com.ultramega.playershells.registry;

import com.ultramega.playershells.container.CentrifugeContainerMenu;
import com.ultramega.playershells.container.ShellForgeContainerMenu;

import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, MODID);

    public static final Supplier<MenuType<ShellForgeContainerMenu>> SHELL_FORGE = MENU_TYPES.register("shell_forge", () ->
        IMenuTypeExtension.create(ShellForgeContainerMenu::new));
    public static final Supplier<MenuType<CentrifugeContainerMenu>> CENTRIFUGE = MENU_TYPES.register("centrifuge", () ->
        IMenuTypeExtension.create(CentrifugeContainerMenu::new));

    private ModMenuTypes() {
    }
}
