package com.wolfyscript.bundleinv.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wolfyscript.bundleinv.BundleInvConstants;
import com.wolfyscript.bundleinv.client.gui.screen.BundleStorageWidget;
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

public class BundleItemContainer extends ClickableWidget {

    private static final int U = 1, V = 167;
    public static final int WIDTH = 24, HEIGHT = 24;

    private PlayerScreenHandler handler;
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

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack.copy();
    }

    public void checkVisibility() {
        visible = !itemStack.isEmpty();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        //TODO: Send packet to server to request an item
        ItemStack toSend = itemStack.copy();
        toSend.setCount(Math.min(toSend.getCount(), toSend.getMaxCount()));

        PacketByteBuf packetBuf = PacketByteBufs.create();
        packetBuf.writeByte(id);
        packetBuf.writeInt(toSend.getMaxCount());
        packetBuf.writeItemStack(toSend);
        ClientPlayNetworking.send(BundleInvConstants.C2S_BUNDLE_ITEM_CONTAINER_CLICK_PACKET, packetBuf);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, BundleStorageWidget.TEXTURE);
        this.drawTexture(matrices, this.x, this.y, U, V, this.width, this.height);
        minecraftClient.getItemRenderer().renderInGuiWithOverrides(itemStack, this.x + 4, this.y + 4);
        minecraftClient.getItemRenderer().renderGuiItemOverlay(minecraftClient.textRenderer, itemStack, x + 4, y + 4, null);
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }
}
