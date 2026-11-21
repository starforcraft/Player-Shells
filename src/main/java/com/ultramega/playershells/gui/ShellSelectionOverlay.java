package com.ultramega.playershells.gui;

import com.ultramega.playershells.entities.ShellEntity;
import com.ultramega.playershells.storage.ClientShellData;
import com.ultramega.playershells.storage.ShellState;
import com.ultramega.playershells.utils.ShellBundle;
import com.ultramega.playershells.utils.ShellBundle.InventoryEntry;
import com.ultramega.playershells.utils.ShellBundle.ShellEntry;
import com.ultramega.playershells.utils.ShellBundle.StatEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import static com.ultramega.playershells.PlayerShells.MODID;
import static com.ultramega.playershells.utils.MathUtils.EMPTY_UUID;

public class ShellSelectionOverlay extends RadialMenuRenderer<ShellBundle.ShellEntry> implements GuiLayer {
    public static final ShellSelectionOverlay INSTANCE = new ShellSelectionOverlay();
    private static final int SHELLS_PER_PAGE = 6;
    private static final int PAGE_ARROW_OFFSET = 26;
    private static final int PAGE_ARROW_HALF_SIZE = 7;
    private static final Identifier ARMOR_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/armor_empty");
    private static final Identifier ARMOR_FULL_SPRITE = Identifier.withDefaultNamespace("hud/armor_full");
    private static final Identifier ARMOR_HALF_SPRITE = Identifier.withDefaultNamespace("hud/armor_half");
    private static final Identifier FOOD_EMPTY_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_empty_hunger");
    private static final Identifier FOOD_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_FULL_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_full_hunger");
    private static final Identifier FOOD_FULL_SPRITE = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier FOOD_HALF_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_half_hunger");
    private static final Identifier FOOD_HALF_SPRITE = Identifier.withDefaultNamespace("hud/food_half");
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private static final Identifier[] EMPTY_ARMOR = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
        InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS};
    private static final Identifier INVENTORY_LOCATION = Identifier.fromNamespaceAndPath(MODID, "textures/gui/clean_inventory.png");

    @Nullable
    private Consumer<ShellEntry> onSelectShell;
    @Nullable
    private Runnable onClose;
    @Nullable
    private ShellBundle displayedShell;
    private int pageIndex;

    @Override
    public void render(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
        // TODO: Add eye view into the top right corner, below coordinates, dimension and gamemode
        //  also add curios compat to the left side of the inventory
        if (!this.isOpened()) {
            return;
        }

        super.render(graphics);
        this.renderPageControls(graphics);

        @Nullable final ShellEntry shellEntry = super.lastIndexUnderMouse != -1 ? this.getEntries().get(super.lastIndexUnderMouse) : null;

        if (shellEntry == null) {
            return;
        }

        final Matrix3x2fStack pose = graphics.pose();
        final float centerX = graphics.guiWidth() / 2f;
        final float centerY = graphics.guiHeight() / 2f;

        final int invWidth = 176;
        final int invHeight = 166;

        // Top left
        this.renderInventory(graphics, pose, centerX, centerY, invWidth, invHeight, shellEntry);

        // Bottom left
        this.renderStats(graphics, pose, centerX, centerY, invWidth, invHeight, shellEntry);
    }

    private void renderPageControls(final GuiGraphicsExtractor graphics) {
        final int pageCount = this.getPageCount();
        if (pageCount <= 1) {
            return;
        }
        final var font = Minecraft.getInstance().font;
        final int centerX = graphics.guiWidth() / 2;
        final int centerY = graphics.guiHeight() / 2;
        final MousePos mouse = getMousePos();
        final int hoveredArrow = pageArrowAt(getDiffFromCenter(mouse.x(), mouse.y()));
        final Component counter = Component.literal((this.pageIndex + 1) + "/" + pageCount);
        final int counterHalfWidth = (font.width(counter) + 1) / 2 + 4;
        graphics.fill(centerX - counterHalfWidth, centerY - PAGE_ARROW_HALF_SIZE,
            centerX + counterHalfWidth, centerY + PAGE_ARROW_HALF_SIZE, 0xCC343434);
        graphics.centeredText(font, counter, centerX, centerY - font.lineHeight / 2, 0xCCFFFFFF);
        this.renderPageArrow(graphics, centerX, centerY, -1, this.pageIndex > 0, hoveredArrow == -1);
        this.renderPageArrow(graphics, centerX, centerY, 1, this.pageIndex + 1 < pageCount, hoveredArrow == 1);
    }

    private void renderPageArrow(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final int direction,
                                 final boolean enabled, final boolean hovered) {
        final int x = centerX + direction * PAGE_ARROW_OFFSET;
        final int size = PAGE_ARROW_HALF_SIZE;
        if (enabled && hovered) {
            graphics.fill(x - size, centerY - size, x + size, centerY + size, 0x994D4D4D);
        }
        final var font = Minecraft.getInstance().font;
        graphics.centeredText(font, Component.literal(direction < 0 ? "‹" : "›"), x, centerY - font.lineHeight / 2,
            enabled ? 0xCCFFFFFF : 0x66888888);
    }

    private static int pageArrowAt(final MousePos mouse) {
        final double x = mouse.x();
        if (Math.abs(mouse.y()) >= PAGE_ARROW_HALF_SIZE || Math.abs(Math.abs(x) - PAGE_ARROW_OFFSET) >= PAGE_ARROW_HALF_SIZE) {
            return 0;
        }
        return x < 0 ? -1 : 1;
    }

    private void renderInventory(final GuiGraphicsExtractor graphics,
                                 final Matrix3x2fStack pose,
                                 final float centerX,
                                 final float centerY,
                                 final int invWidth,
                                 final int invHeight,
                                 final ShellEntry shellEntry) {
        if (shellEntry.inventory().isEmpty()) {
            return;
        }


        pose.pushMatrix();
        pose.translate(centerX - (OUTER + 15f) - invWidth / 2f, centerY - invHeight / 2f / 2f);
        pose.scale(0.5f, 0.5f);

        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, 0, 0, 0, 0, invWidth, invHeight, 256, 256);

        for (int i = 0; i < 4; ++i) {
            final ItemStack armorStack = shellEntry.inventory().get().armor().get(4 - (i + 1));
            if (armorStack.isEmpty()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY_ARMOR[i], 8, 8 + i * 18, 16, 16);
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
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD, 77, 62, 16, 16);
        } else {
            this.renderSlotContent(graphics, offHandStack, 77, 62, invWidth);
        }

        pose.popMatrix();
    }

    private void renderStats(final GuiGraphicsExtractor graphics,
                             final Matrix3x2fStack pose,
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
        final StatEntry stats = shellEntry.stats().get();
        final int currentHealth = Mth.ceil(stats.health());
        //final float maxHealth = Math.max((float) statEntry.health().getAttributeValue(Attributes.MAX_HEALTH), currentHealth)); //TODO

        pose.pushMatrix();
        pose.translate(centerX - (OUTER + 15f) - invWidth / 2f + offsetX, centerY - invHeight / 2f / 2f + invHeight / 2f + 10f);
        final int healthY = stats.armorValue() > 0 ? 10 : 0;
        if (healthY > 0) {
            this.renderArmor(graphics, stats.armorValue(), 0, 0);
        }
        this.renderHearts(graphics, 0, healthY, 5, 20f, currentHealth, 0);
        this.renderFood(graphics, stats, 0, healthY + 10);
        pose.popMatrix();
    }

    @Override
    public Component getTitle(final ShellBundle.ShellEntry entry) {
        return Component.literal(entry.title());
    }

    @Override
    public List<ShellBundle.ShellEntry> getEntries() {
        if (this.displayedShell == null) {
            return List.of();
        }
        final List<ShellEntry> entries = this.displayedShell.getEntries();
        final int first = this.pageIndex * SHELLS_PER_PAGE;
        return entries.subList(first, Math.min(first + SHELLS_PER_PAGE, entries.size()));
    }

    private int getPageCount() {
        return this.displayedShell == null ? 0 : Math.ceilDiv(this.displayedShell.getEntries().size(), SHELLS_PER_PAGE);
    }

    public boolean mouseScroll(final int scroll) {
        if (this.getPageCount() <= 1 || scroll == 0) {
            return false;
        }
        this.changePage(-Integer.signum(scroll));
        return true;
    }

    private void changePage(final int direction) {
        final int next = Mth.clamp(this.pageIndex + direction, 0, this.getPageCount() - 1);
        if (next != this.pageIndex) {
            this.pageIndex = next;
            this.clearState();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
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

        return ClientShellData.INSTANCE.getPreview(mc.level, playerUuid, shellUuid);
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
            return;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        final MousePos diffFromCenter = getDiffFromCenter(mouseX, mouseY);
        final int pageDirection = pageArrowAt(diffFromCenter);
        if (this.getPageCount() > 1 && pageDirection != 0) {
            this.changePage(pageDirection);
            return;
        }
        if (Mth.length(diffFromCenter.x(), diffFromCenter.y()) > OUTER + 30) {
            return;
        }
        final int selectionIndex = this.getElementUnderMouse(diffFromCenter);
        final List<ShellEntry> entries = this.getEntries();
        if (selectionIndex >= 0 && selectionIndex < entries.size()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            if (this.onSelectShell != null) {
                final ShellEntry shellEntry = entries.get(selectionIndex);
                if (this.getPlayerShellCreationProgress(shellEntry) != 100) {
                    return;
                }
                this.onSelectShell.accept(shellEntry);
            }

            this.close(true);
        }
    }

    private void renderSlotContent(final GuiGraphicsExtractor graphics, final ItemStack stack, final int x, final int y, final int imageWidth) {
        final int seed = x + y * imageWidth;
        graphics.item(stack, x, y, seed);
        graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    /**
     * Copied and modified from {@link net.minecraft.client.gui.Gui#renderFood(net.minecraft.client.gui.GuiGraphicsExtractor, net.minecraft.world.entity.player.Player, int, int)}
     */
    private void renderFood(final GuiGraphicsExtractor graphics, final StatEntry stats, final int x, final int y) {
        final int foodLevel = stats.foodLevel();

        final boolean hasHunger = stats.activeEffects().containsKey(MobEffects.HUNGER);
        final Identifier emptySprite = hasHunger ? FOOD_EMPTY_HUNGER_SPRITE : FOOD_EMPTY_SPRITE;
        final Identifier halfSprite = hasHunger ? FOOD_HALF_HUNGER_SPRITE : FOOD_HALF_SPRITE;
        final Identifier fullSprite = hasHunger ? FOOD_FULL_HUNGER_SPRITE : FOOD_FULL_SPRITE;


        for (int i = 0; i < 10; i++) {
            final int foodIndex = i * 2 + 1;
            final int posX = x + i * 8;

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, emptySprite, posX, y, 9, 9);

            if (foodIndex < foodLevel) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fullSprite, posX, y, 9, 9);
            } else if (foodIndex == foodLevel) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, halfSprite, posX, y, 9, 9);
            }
        }

    }

    /**
     * Copied and modified from {@link Gui#renderArmor(GuiGraphics, Player, int, int, int, int)}
     */
    private void renderArmor(final GuiGraphicsExtractor graphics, final int armorValue, final int x, final int y) {
        for (int i = 0; i < 10; i++) {
            final int armorIndex = i * 2 + 1;
            final Identifier sprite = armorIndex < armorValue ? ARMOR_FULL_SPRITE
                : armorIndex == armorValue ? ARMOR_HALF_SPRITE : ARMOR_EMPTY_SPRITE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + i * 8, y, 9, 9);
        }
    }

    /**
     * Copied and modified from {@link Gui#renderHearts(GuiGraphics, Player, int, int, int, int, float, int, int, int, boolean)}
     */
    private void renderHearts(
        final GuiGraphicsExtractor guiGraphics,
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

    private void renderHeart(final GuiGraphicsExtractor guiGraphics,
                             final Gui.HeartType heartType,
                             final int x,
                             final int y,
                             final boolean hardcore,
                             final boolean halfHeart,
                             final boolean blinking) {

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, heartType.getSprite(hardcore, blinking, halfHeart), x, y, 9, 9);

    }

    public void open(final Player player, final Consumer<ShellEntry> onSelectShell, final Runnable onClose) {
        final List<ShellEntry> shellEntries = new ArrayList<>();
        //TODO: rebuild entries if shell creation progress == 0 (can happen if someone exterminates the shell)
        final var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (final ShellState shellState : ClientShellData.INSTANCE.getAll(player.getUUID())) {
            final ShellEntity preview = ClientShellData.INSTANCE.getPreview(level, player.getUUID(), shellState.shellUUID());
            if (preview == null) {
                continue;
            }
            final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
            for (int slot = 0; slot < ARMOR.length; slot++) {
                armor.set(slot, preview.getItemBySlot(ARMOR[slot]));
            }
            final InventoryEntry inventoryEntry = new InventoryEntry(preview.getInventory().getNonEquipmentItems(), armor,
                NonNullList.of(ItemStack.EMPTY, preview.getOffhandItem()));
            final StatEntry statEntry = new StatEntry(preview.getHealth(), preview.getArmorValue(),
                preview.getFoodData().getFoodLevel(), preview.getActiveEffectsMap());

            shellEntries.add(new ShellEntry(player.getName().getString(), Optional.of(shellState.shellForgePos()),
                Optional.of(inventoryEntry), Optional.of(statEntry), Optional.of(shellState.shellUUID())));
        }

        if (shellEntries.isEmpty()) {
            shellEntries.add(new ShellEntry("", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        }
        this.displayedShell = new ShellBundle(player.getUUID(), shellEntries);
        this.pageIndex = 0;
        this.clearState();

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
        this.pageIndex = 0;
        this.onSelectShell = null;
        this.onClose = null;
        this.clearState();
    }
}
