package com.ultramega.playershells.registry;

import com.ultramega.playershells.container.CentrifugeContainerMenu;
import com.ultramega.playershells.container.ShellForgeContainerMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.ultramega.playershells.PlayerShells.MODID;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<MenuType<ShellForgeContainerMenu>> SHELL_FORGE = MENU_TYPES.register("shell_forge", () ->
        IForgeMenuType.create(ShellForgeContainerMenu::new));
    public static final RegistryObject<MenuType<CentrifugeContainerMenu>> CENTRIFUGE = MENU_TYPES.register("centrifuge", () ->
        IForgeMenuType.create(CentrifugeContainerMenu::new));

    private ModMenuTypes() {
    }
}
