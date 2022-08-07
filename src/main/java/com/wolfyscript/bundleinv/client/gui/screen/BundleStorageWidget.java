package com.wolfyscript.bundleinv.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wolfyscript.bundleinv.BundleStorageHolder;
import com.wolfyscript.bundleinv.PlayerBundleStorage;
import com.wolfyscript.bundleinv.client.BundleInvClient;
import com.wolfyscript.bundleinv.client.gui.widget.BundleItemContainer;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public class BundleStorageWidget extends DrawableHelper implements Drawable, Element, Selectable {

    public static final Identifier TEXTURE = new Identifier("bundleinv", "textures/gui/container/bundle_storage.png");
    public static final int WIDTH = 110;
    public static final int OFFSET = 89;
    public static final int HEIGHT = 166;

    protected MinecraftClient client;
    private PlayerScreenHandler handler;
    private PlayerBundleStorage storage;

    private int leftOffset;
    private int parentWidth;
    private int parentHeight;
    private boolean open;
    private boolean narrow;

    private boolean canScroll;
    private float scrollPosition;
    private boolean scrolling;

    private int cachedInvChangeCount;

    private static final int COLOR_CURSOR_HOVER;

    static {
        Color hoverColor = Color.DARK_GRAY;
        COLOR_CURSOR_HOVER = ColorHelper.Argb.getArgb(190, hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue());
    }

    private List<BundleItemContainer> itemContainers;

    private BundleItemContainer hoveredBundleItemContainer = null;

    public BundleStorageWidget() {
        itemContainers = new ArrayList<>();
    }

    public void init(int parentWidth, int parentHeight, MinecraftClient client, boolean narrow, PlayerScreenHandler playerScreenHandler) {
        this.client = client;
        this.handler = playerScreenHandler;
        this.storage = ((BundleStorageHolder) client.player.getInventory()).getBundleStorage();
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        this.narrow = narrow;
        this.cachedInvChangeCount = client.player.getInventory().getChangeCount();

        if (open) {

            reset();
        }
    }

    public void update() {
        if (this.cachedInvChangeCount != this.client.player.getInventory().getChangeCount()) {
            for (BundleItemContainer container : itemContainers) {
                container.checkVisibility();
            }
            this.cachedInvChangeCount = this.client.player.getInventory().getChangeCount();
        }
    }

    private void reset() {
        this.leftOffset = narrow ? 0 : OFFSET;
        itemContainers = new ArrayList<>();
        int x = (this.parentWidth - WIDTH) / 2 + this.leftOffset;
        int y = (this.parentHeight - HEIGHT) / 2;

        int id = 0;
        for (int i = 0; i < 5 * BundleItemContainer.HEIGHT; i += BundleItemContainer.HEIGHT + 1) {
            for (int j = 0; j < 3 * BundleItemContainer.WIDTH; j += BundleItemContainer.WIDTH + 1) {
                var container = new BundleItemContainer(handler, x + 10 + j, y + 28 + i, id++);
                itemContainers.add(container);
                container.visible = false;
            }
        }
        updateSlots();
    }

    public void refresh() {
        if (isOpen()) {
            updateSlots();
        }
    }

    public void updateSlots() {
        canScroll = storage.size() > 15;
        scrollContainers(scrollPosition);
    }

    private void setOpen(boolean open) {
        this.open = open;
        if (client.currentScreen instanceof AbstractInventoryScreenInterface screenInterface1) {
            screenInterface1.setLeftWidth(open ? WIDTH + 1 : 0);
        }
        if (open) {
            reset();
        }
    }

    public void toggleOpen() {
        setOpen(!isOpen());
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Gets the new left edge of the inventory to make place for this widget.
     *
     * @param x The current x position of the inventory.
     * @param width The width of the inventory.
     * @param backgroundWidth The background width of the inventory.
     * @param recipeBookOpen If the recipe book is currently open.
     * @return The new left postion of the inventory.
     */
    public int findLeftEdge(int x, int width, int backgroundWidth, boolean recipeBookOpen) {
        if (!isOpen()) return recipeBookOpen ? x : (width - backgroundWidth) / 2;
        return this.isOpen() && !this.narrow ? (width - backgroundWidth - WIDTH) / 2 : (width - backgroundWidth) / 2;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.isOpen()) {
            int x = (this.parentWidth - WIDTH) / 2 + this.leftOffset;
            int y = (this.parentHeight - HEIGHT) / 2;
            matrices.push();
            matrices.translate(0.0, 0.0, 100.0);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            this.drawTexture(matrices, x, y, 0, 0, WIDTH, HEIGHT);

            int scrollbarTopX = x + WIDTH - 24;
            int scrollbarTopY = y + 29;
            int scrollbarBottom = scrollbarTopY + 126;
            this.drawTexture(matrices, scrollbarTopX, scrollbarTopY + (int)((float)(scrollbarBottom - scrollbarTopY - 17) * this.scrollPosition), 26 + (canScroll ? 0 : 12), 167, 12, 15);

            hoveredBundleItemContainer = null;
            for (BundleItemContainer itemContainer : itemContainers) {
                itemContainer.render(matrices, mouseX, mouseY, delta);
                if (itemContainer.isHovered()) {
                    hoveredBundleItemContainer = itemContainer;
                }
            }

            renderCursorHoverOverlay(matrices, x, y, delta, mouseX, mouseY);
            matrices.pop();
        }
    }

    private void renderCursorHoverOverlay(MatrixStack matrices, int x, int y, float delta, int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY) && !handler.getCursorStack().isEmpty()) {

            fill(matrices, x + 7, y + 7, x + WIDTH - 7, y + HEIGHT - 7, COLOR_CURSOR_HOVER); //-1072689136, -804253680
        }
    }

    public void drawTooltip(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        if (this.isOpen() && handler.getCursorStack().isEmpty()) {
            if (hoveredBundleItemContainer != null) {
                List<Text> text = client.currentScreen.getTooltipFromItem(hoveredBundleItemContainer.getItemStack());
                if (this.client.currentScreen != null) {
                    this.client.currentScreen.renderTooltip(matrices, text, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrolling && canScroll) {
            int x = (this.parentWidth - WIDTH) / 2 + this.leftOffset;
            int y = (this.parentHeight - HEIGHT) / 2;

            int scrollBarTopY = y + 29;
            int scrollBarBottomY = scrollBarTopY + 124;
            this.scrollPosition = ((float) mouseY - (float) scrollBarTopY - 7.5f) / ((float) (scrollBarBottomY - scrollBarTopY) - 15f);
            this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0f, 1f);

            scrollContainers(this.scrollPosition);

            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!canScroll) return false;
        int stepPerPage = (storage.size() + 2) / 4;
        float value = (float) (amount / stepPerPage);
        this.scrollPosition = MathHelper.clamp(this.scrollPosition - value, 0.0f, 1f);
        scrollContainers(scrollPosition);
        return true;
    }

    private void scrollContainers(float position) {
        int stepPerPage = (storage.size() + 2) / 4;
        int itemPos = (int) ((double) (position * (float) stepPerPage) + 0.5);

        if (itemPos < 0) {
            itemPos = 0;
        }

        for (int row = 0; row < 5; ++row) {
            for (int column = 0; column < 3; ++column) {
                int itemIndex = column + (row + itemPos) * 3;
                BundleItemContainer container = itemContainers.get(column + row * 3);
                if (itemIndex >= 0 && itemIndex < storage.size()) {
                    container.setItemStack(storage.get(itemIndex));
                } else {
                    container.setItemStack(ItemStack.EMPTY);
                }
                container.checkVisibility();
            }
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isOpen() && !this.client.player.isSpectator()) {
            if (!isMouseOver(mouseX, mouseY)) return false;
            for (BundleItemContainer container : itemContainers) {
                if (container.mouseClicked(mouseX, mouseY, button)) return true;
            }
            if (isClickInScrollbar(mouseX, mouseY)) {
                this.scrolling = true;
                return true;
            }

            if (!client.player.currentScreenHandler.getCursorStack().isEmpty()) {
                ItemStack stack = client.player.currentScreenHandler.getCursorStack();

                BundleInvClient.sendBundleStorageStoreItem(true, stack);
            }
        }
        return false;
    }

    protected boolean isClickInScrollbar(double mouseX, double mouseY) {
        int x = (this.parentWidth - WIDTH) / 2 + this.leftOffset;
        int y = (this.parentHeight - HEIGHT) / 2;
        int topX = x + WIDTH - 24;
        int topY = y + 29;
        int bottomX = topX + 14;
        int bottomY = topY + 126;
        boolean valid = mouseX >= topX && mouseY >= topY && mouseX < bottomX && mouseY < bottomY;
        return valid;
    }

    public boolean isClickOutsideBounds(double mouseX, double mouseY, int invX, int invY, int backgroundWidth, int backgroundHeight, int button) {
        if (!this.isOpen()) {
            return true;
        } else {
            boolean inventory = mouseX < invX || mouseY < invY || mouseX >= (invX + backgroundWidth) || mouseY >= (invY + backgroundHeight);
            return inventory && !isMouseOver(mouseX, mouseY);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int x = (this.parentWidth - WIDTH) / 2 + this.leftOffset;
        int y = (this.parentHeight - HEIGHT) / 2;
        return mouseX >= x && mouseX < (x + WIDTH) && mouseY >= y && mouseY < (y + HEIGHT);
    }

    @Override
    public SelectionType getType() {
        return this.open ? Selectable.SelectionType.HOVERED : Selectable.SelectionType.NONE;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }
}
