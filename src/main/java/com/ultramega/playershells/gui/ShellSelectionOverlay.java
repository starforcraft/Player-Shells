package com.ultramega.playershells.gui;

import com.ultramega.playershells.entities.ShellEntity;
import com.ultramega.playershells.storage.ClientShellData;
import com.ultramega.playershells.storage.ShellState;
import com.ultramega.playershells.utils.ShellBundle;
import com.ultramega.playershells.utils.ShellBundle.InventoryEntry;
import com.ultramega.playershells.utils.ShellBundle.ShellEntry;
import com.ultramega.playershells.utils.ShellBundle.StatEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.lwjgl.glfw.GLFW;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.blockentities.renderer.ShellForgeBlockEntityRenderer.getPlayerShellFromCache;
import static com.ultramega.playershells.utils.MathUtils.EMPTY_UUID;

public class ShellSelectionOverlay extends RadialMenuRenderer<ShellBundle.ShellEntry> implements IGuiOverlay {
    public static final ShellSelectionOverlay INSTANCE = new ShellSelectionOverlay();
    private static final Map<UUID, ShellEntity> SHELL_CACHE = new HashMap<>();
    private static final ResourceLocation INVENTORY_LOCATION = new ResourceLocation(MODID, "textures/gui/clean_inventory.png");

    @Nullable
    private Consumer<ShellEntry> onSelectShell;
    @Nullable
    private Runnable onClose;
    @Nullable
    private ShellBundle displayedShell;

    @Override
    public void render(final ForgeGui gui, final GuiGraphics graphics, final float partialTick, final int screenWidth, final int screenHeight) {
        // TODO: Add eye view into the top right corner, below coordinates, dimension and gamemode
        //  also add curios compat to the left side of the inventory
        //  also implement pagination
        if (!this.isOpened()) {
            return;
        }

        super.render(graphics);

        @Nullable final ShellEntry shellEntry = super.lastIndexUnderMouse != -1 ? this.getEntries().get(super.lastIndexUnderMouse) : null;

        if (shellEntry == null) {
            return;
        }

        final PoseStack pose = graphics.pose();
        final float centerX = graphics.guiWidth() / 2f;
        final float centerY = graphics.guiHeight() / 2f;

        final int invWidth = 176;
        final int invHeight = 166;

        // Top left
        this.renderInventory(graphics, pose, centerX, centerY, invWidth, invHeight, shellEntry);
    }

    private void renderInventory(final GuiGraphics graphics,
                                 final PoseStack pose,
                                 final float centerX,
                                 final float centerY,
                                 final int invWidth,
                                 final int invHeight,
                                 final ShellEntry shellEntry) {
        if (shellEntry.inventory().isEmpty()) {
            return;
        }

        pose.pushPose();
        pose.translate(centerX - (OUTER + 15f) - invWidth / 2f, centerY - invHeight / 2f / 2f, 0f);
        pose.scale(0.5f, 0.5f, 0.5f);

        graphics.blit(INVENTORY_LOCATION, 0, 0, 0, 0, invWidth, invHeight, 256, 256);

        for (int i = 0; i < 4; ++i) {
            final ItemStack armorStack = shellEntry.inventory().get().armor().get(4 - (i + 1));
            if (!armorStack.isEmpty()) {
                this.renderSlotContent(graphics, armorStack, 8, 8 + i * 18, invWidth);
            }
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                final ItemStack stack = shellEntry.inventory().get().items().get(j + (i + 1) * 9);
                this.renderSlotContent(graphics, stack, 8 + j * 18, 84 + i * 18, invWidth);
            }
        }

        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = shellEntry.inventory().get().items().get(i);
            this.renderSlotContent(graphics, stack, 8 + i * 18, 142, invWidth);
        }

        final ItemStack offHandStack = shellEntry.inventory().get().offhand().isEmpty() ? ItemStack.EMPTY : shellEntry.inventory().get().offhand().get(0);
        if (!offHandStack.isEmpty()) {
            this.renderSlotContent(graphics, offHandStack, 77, 62, invWidth);
        }

        pose.popPose();
    }

    private void renderStats(final GuiGraphics graphics,
                             final PoseStack pose,
                             final float centerX,
                             final float centerY,
                             final int invWidth,
                             final int invHeight,
                             final ShellEntry shellEntry) {
    }

    @Override
    public Component getTitle(final ShellBundle.ShellEntry entry) {
        return Component.literal(entry.title());
    }

    @Override
    public List<ShellBundle.ShellEntry> getEntries() {
        return this.displayedShell == null ? List.of() : this.displayedShell.getEntries();
    }

    @Nullable
    public ShellBundle getDisplayedShell() {
        return this.displayedShell;
    }

    @Nullable
    @Override
    public ShellEntity getPlayerShell(final ShellEntry entry) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || entry.shellUuid().isEmpty()) {
            return null;
        }

        final UUID playerUuid = mc.player.getUUID();
        final UUID shellUuid = entry.shellUuid().get();
        if (playerUuid.equals(EMPTY_UUID) || shellUuid.equals(EMPTY_UUID)) {
            return null;
        }

        return getPlayerShellFromCache(mc.level, playerUuid, shellUuid, SHELL_CACHE);
    }

    @Override
    public int getPlayerShellCreationProgress(final ShellEntry entry) {
        final Player player = Minecraft.getInstance().player;
        if (player == null || entry.shellUuid().isEmpty()) {
            return 0;
        }
        final ShellState shellState = ClientShellData.INSTANCE.get(player.getUUID(), entry.shellUuid().get());
        if (shellState == null) {
            return 0;
        }
        return shellState.shellCreationProgress();
    }

    public void keyPressed(final int key) {
        if (key == Minecraft.getInstance().options.keyUp.getKey().getValue()) {
            this.cancel();
        }
    }

    public void mouseClick(final double mouseX, final double mouseY, final int button, final int action) {
        if (action == GLFW.GLFW_RELEASE) {
            return;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            this.cancel();
        }

        final MousePos diffFromCenter = getDiffFromCenter(mouseX, mouseY);
        final double distanceFromCenter = Mth.length(diffFromCenter.x(), diffFromCenter.y());
        if (distanceFromCenter < 30 || distanceFromCenter > RadialMenuRenderer.OUTER + 30) {
            return;
        }

        final int selectionIndex = this.getElementUnderMouse();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (this.onSelectShell != null) {
                final ShellEntry shellEntry = this.getEntries().get(selectionIndex);
                if (this.getPlayerShellCreationProgress(shellEntry) != 100) {
                    return;
                }
                this.onSelectShell.accept(shellEntry);
            }

            this.close(true);
        }
    }

    private void renderSlotContent(final GuiGraphics graphics, final ItemStack stack, final int x, final int y, final int imageWidth) {
        final int seed = x + y * imageWidth;
        graphics.renderItem(stack, x, y, seed);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    public void open(final Player player, final Consumer<ShellEntry> onSelectShell, final Runnable onClose) {
        final List<ShellEntry> shellEntries = new ArrayList<>();
        //TODO: rebuild entries if shell creation progress == 0 (can happen if someone exterminates the shell)
        for (final ShellState shellState : ClientShellData.INSTANCE.getAll(player.getUUID())) {
            final Inventory inventory = new Inventory(player);
            inventory.load(shellState.playerData().getList("Inventory", ListTag.TAG_COMPOUND));
            final InventoryEntry inventoryEntry = new InventoryEntry(inventory.items, inventory.armor, inventory.offhand);

            final FoodData foodData = new FoodData();
            foodData.readAdditionalSaveData(shellState.playerData());
            final AttributeMap attributes = new AttributeMap(DefaultAttributes.getSupplier(EntityType.PLAYER));
            attributes.load(shellState.playerData().getList("attributes", ListTag.TAG_COMPOUND));
            final Map<MobEffect, MobEffectInstance> activeEffects = Maps.newHashMap();
            final ListTag effectsTag = shellState.playerData().getList("active_effects", ListTag.TAG_COMPOUND);
            for (int i = 0; i < effectsTag.size(); ++i) {
                final MobEffectInstance effectInstance = MobEffectInstance.load(effectsTag.getCompound(i));
                if (effectInstance != null) {
                    activeEffects.put(effectInstance.getEffect(), effectInstance);
                }
            }
            final StatEntry statEntry = new StatEntry(shellState.playerData().getFloat("Health"),
                Mth.floor(attributes.getValue(Attributes.ARMOR)),
                foodData.getFoodLevel(),
                activeEffects);

            shellEntries.add(new ShellEntry(player.getName().getString(), Optional.of(shellState.shellForgePos()),
                Optional.of(inventoryEntry), Optional.of(statEntry), Optional.of(shellState.shellUUID())));
        }

        if (shellEntries.isEmpty()) {
            shellEntries.add(new ShellEntry("", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        }
        this.displayedShell = new ShellBundle(player.getUUID(), shellEntries);

        this.onSelectShell = onSelectShell;
        this.onClose = onClose;
    }

    public boolean isOpened() {
        return this.displayedShell != null;
    }

    private void cancel() {
        if (this.onClose != null) {
            this.onClose.run();
        }

        this.close(true);
    }

    public void close(final boolean grabMouse) {
        if (grabMouse) {
            Minecraft.getInstance().mouseHandler.grabMouse();
        }
        this.displayedShell = null;
        this.clearState();
    }
}
