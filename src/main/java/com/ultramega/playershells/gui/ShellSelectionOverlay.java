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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.blockentities.renderer.ShellForgeBlockEntityRenderer.getPlayerShellFromCache;
import static com.ultramega.playershells.utils.MathUtils.EMPTY_UUID;
import static net.minecraft.client.gui.Gui.ARMOR_EMPTY_SPRITE;
import static net.minecraft.client.gui.Gui.ARMOR_FULL_SPRITE;
import static net.minecraft.client.gui.Gui.ARMOR_HALF_SPRITE;
import static net.minecraft.client.gui.Gui.FOOD_EMPTY_HUNGER_SPRITE;
import static net.minecraft.client.gui.Gui.FOOD_EMPTY_SPRITE;
import static net.minecraft.client.gui.Gui.FOOD_FULL_HUNGER_SPRITE;
import static net.minecraft.client.gui.Gui.FOOD_FULL_SPRITE;
import static net.minecraft.client.gui.Gui.FOOD_HALF_HUNGER_SPRITE;
import static net.minecraft.client.gui.Gui.FOOD_HALF_SPRITE;
import static net.minecraft.world.inventory.InventoryMenu.SLOT_IDS;
import static net.minecraft.world.inventory.InventoryMenu.TEXTURE_EMPTY_SLOTS;

public class ShellSelectionOverlay extends RadialMenuRenderer<ShellBundle.ShellEntry> implements LayeredDraw.Layer {
    public static final ShellSelectionOverlay INSTANCE = new ShellSelectionOverlay();
    private static final Map<UUID, ShellEntity> SHELL_CACHE = new HashMap<>();
    private static final ResourceLocation INVENTORY_LOCATION = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/clean_inventory.png");

    @Nullable
    private Consumer<ShellEntry> onSelectShell;
    @Nullable
    private Runnable onClose;
    @Nullable
    private ShellBundle displayedShell;

    @Override
    public void render(final GuiGraphics graphics, final DeltaTracker deltaTracker) {
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

        // Bottom left
        this.renderStats(graphics, pose, centerX, centerY, invWidth, invHeight, shellEntry);
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

        final var textureAtlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);

        pose.pushPose();
        pose.translate(centerX - (OUTER + 15f) - invWidth / 2f, centerY - invHeight / 2f / 2f, 0f);
        pose.scale(0.5f, 0.5f, 0.5f);

        graphics.blit(INVENTORY_LOCATION, 0, 0, 0, 0, invWidth, invHeight, 256, 256);

        for (int i = 0; i < 4; ++i) {
            final ItemStack armorStack = shellEntry.inventory().get().armor().get(4 - (i + 1));
            if (armorStack.isEmpty()) {
                final TextureAtlasSprite sprite = textureAtlas.apply(TEXTURE_EMPTY_SLOTS.get(SLOT_IDS[i]));
                graphics.blit(8, 8 + i * 18, 0, 16, 16, sprite);
            } else {
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

        final ItemStack offHandStack = shellEntry.inventory().get().offhand().getFirst();
        if (offHandStack.isEmpty()) {
            final TextureAtlasSprite sprite = textureAtlas.apply(InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            graphics.blit(77, 62, 0, 16, 16, sprite);
        } else {
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
        if (shellEntry.stats().isEmpty()) {
            return;
        }

        // TODO: show active effects too

        final float bottomWidth = 82f;
        final float offsetX = (invWidth / 2f - bottomWidth) / 2f;
        final int currentHealth = Mth.ceil(shellEntry.stats().get().health());
        //final float maxHealth = Math.max((float) statEntry.health().getAttributeValue(Attributes.MAX_HEALTH), currentHealth)); //TODO

        pose.pushPose();
        pose.translate(centerX - (OUTER + 15f) - invWidth / 2f + offsetX, centerY - invHeight / 2f / 2f + invHeight / 2f + 10f, 0f);
        this.renderArmor(graphics, shellEntry.stats().get(), 0, 0);
        this.renderHearts(graphics, 0, 10, 5, 20f, currentHealth, 0);
        this.renderFood(graphics, shellEntry.stats().get(), 0, 20);
        pose.popPose();
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

    /**
     * Copied and modified from {@link net.minecraft.client.gui.Gui#renderFood(net.minecraft.client.gui.GuiGraphics, net.minecraft.world.entity.player.Player, int, int)}
     */
    private void renderFood(final GuiGraphics graphics, final StatEntry stats, final int x, final int y) {
        final int foodLevel = stats.foodLevel();

        final boolean hasHunger = stats.activeEffects().containsKey(MobEffects.HUNGER);
        final ResourceLocation emptySprite = hasHunger ? FOOD_EMPTY_HUNGER_SPRITE : FOOD_EMPTY_SPRITE;
        final ResourceLocation halfSprite = hasHunger ? FOOD_HALF_HUNGER_SPRITE : FOOD_HALF_SPRITE;
        final ResourceLocation fullSprite = hasHunger ? FOOD_FULL_HUNGER_SPRITE : FOOD_FULL_SPRITE;

        RenderSystem.enableBlend();

        for (int i = 0; i < 10; i++) {
            final int foodIndex = i * 2 + 1;
            final int posX = x + i * 8;

            graphics.blitSprite(emptySprite, posX, y, 9, 9);

            if (foodIndex < foodLevel) {
                graphics.blitSprite(fullSprite, posX, y, 9, 9);
            } else if (foodIndex == foodLevel) {
                graphics.blitSprite(halfSprite, posX, y, 9, 9);
            }
        }

        RenderSystem.disableBlend();
    }

    /**
     * Copied and modified from {@link Gui#renderArmor(GuiGraphics, Player, int, int, int, int)}
     */
    private void renderArmor(final GuiGraphics guiGraphics, final StatEntry stats, final int x, final int y) {
        final int armorValue = stats.armorValue();
        if (armorValue > 0) {
            RenderSystem.enableBlend();

            for (int i = 0; i < 10; ++i) {
                final int armorIndex = i * 2 + 1;
                final int posX = x + i * 8;

                if (armorIndex < armorValue) {
                    guiGraphics.blitSprite(ARMOR_FULL_SPRITE, posX, y, 9, 9);
                }

                if (armorIndex == armorValue) {
                    guiGraphics.blitSprite(ARMOR_HALF_SPRITE, posX, y, 9, 9);
                }

                if (armorIndex > armorValue) {
                    guiGraphics.blitSprite(ARMOR_EMPTY_SPRITE, posX, y, 9, 9);
                }
            }

            RenderSystem.disableBlend();
        }
    }

    /**
     * Copied and modified from {@link Gui#renderHearts(GuiGraphics, Player, int, int, int, int, float, int, int, int, boolean)}
     */
    private void renderHearts(
        final GuiGraphics guiGraphics,
        final int x,
        final int y,
        final int height,
        final float maxHealth,
        final int currentHealth,
        final int absorptionAmount) {
        final Gui.HeartType heartType = Gui.HeartType.NORMAL; //Gui.HeartType.forPlayer(player);
        final boolean hardcore = false;

        final int maxHealthHalves = Mth.ceil((double) maxHealth / 2.0);
        final int absorptionHearts = Mth.ceil((double) absorptionAmount / 2.0);
        final int totalHearts = maxHealthHalves + absorptionHearts;
        final int maxHealthFull = maxHealthHalves * 2;

        for (int heartIndex = totalHearts - 1; heartIndex >= 0; heartIndex--) {
            final int row = heartIndex / 10;
            final int column = heartIndex % 10;

            final int posX = x + column * 8;
            final int posY = y - row * height;

            this.renderHeart(guiGraphics, Gui.HeartType.CONTAINER, posX, posY, hardcore, false, false);

            final int halfIndex = heartIndex * 2;
            final boolean isAbsorptionHeart = heartIndex >= maxHealthHalves;
            if (isAbsorptionHeart) {
                final int absorptionHalfIndex = halfIndex - maxHealthFull;
                if (absorptionHalfIndex < absorptionAmount) {
                    final boolean isHalf = absorptionHalfIndex + 1 == absorptionAmount;
                    this.renderHeart(guiGraphics, heartType == Gui.HeartType.WITHERED ? heartType : Gui.HeartType.ABSORBING,
                        posX, posY, hardcore, false, isHalf);
                }
            }

            if (halfIndex < currentHealth) {
                final boolean isHalf = halfIndex + 1 == currentHealth;
                this.renderHeart(guiGraphics, heartType, posX, posY, hardcore, false, isHalf);
            }
        }
    }

    private void renderHeart(final GuiGraphics guiGraphics,
                             final Gui.HeartType heartType,
                             final int x,
                             final int y,
                             final boolean hardcore,
                             final boolean halfHeart,
                             final boolean blinking) {
        RenderSystem.enableBlend();
        guiGraphics.blitSprite(heartType.getSprite(hardcore, blinking, halfHeart), x, y, 9, 9);
        RenderSystem.disableBlend();
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
            final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
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
