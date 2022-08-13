package com.wolfyscript.bundleinv.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wolfyscript.bundleinv.BundleInvConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenTexts;
import org.jetbrains.annotations.NotNull;

public class BundleItemContainer extends ClickableWidget {

    private static final int U = 1, V = 167;
    public static final int WIDTH = 24, HEIGHT = 24;

    private final PlayerScreenHandler handler;
    private final int x;
    private final int y;
    private final int id;
    private ItemStack itemStack;

    public BundleItemContainer(PlayerScreenHandler handler, int x, int y, int id) {
        super(x, y, WIDTH, HEIGHT, ScreenTexts.EMPTY);
        this.handler = handler;
        this.x = x;
        this.y = y;
        this.id = id;
        this.itemStack = ItemStack.EMPTY;
        this.visible = false;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public int getId() {
        return id;
    }

    public void setItemStack(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void checkVisibility() {
        visible = !itemStack.isEmpty();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!handler.getCursorStack().isEmpty()) {
            return false;
        }
        if (!this.active || !this.visible) {
            return false;
        }
        if (this.isValidClickButton(button) && clicked(mouseX, mouseY)) {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());

            int count = Math.min(itemStack.getCount(), itemStack.getMaxCount());

            ItemStack stackToRemove = itemStack.split(button == 0 ? count : count/2);
            handler.setCursorStack(stackToRemove);

            PacketByteBuf packetBuf = PacketByteBufs.create();
            packetBuf.writeByte(id);
            packetBuf.writeInt(stackToRemove.getCount());
            packetBuf.writeItemStack(stackToRemove);
            ClientPlayNetworking.send(BundleInvConstants.C2S_BUNDLE_ITEM_CONTAINER_CLICK_PACKET, packetBuf);

            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return super.isValidClickButton(button) || button == 1;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (handler.getCursorStack().isEmpty()) {

        }
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, BundleStorageWidget.TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        drawTexture(matrices, this.x, this.y, U, V, this.width, this.height);
        minecraftClient.getItemRenderer().renderInGuiWithOverrides(itemStack, this.x + 4, this.y + 4);
        String label = null;
        if (itemStack.getCount() > 1000) {
            label = itemStack.getCount() / 1000 + "k";
        }
        minecraftClient.getItemRenderer().renderGuiItemOverlay(minecraftClient.textRenderer, itemStack, x + 4, y + 4, label);
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }
}
