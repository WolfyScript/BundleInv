package com.wolfyscript.bundleinv.client;

import com.wolfyscript.bundleinv.BundleInvConstants;
import com.wolfyscript.bundleinv.BundleStorageHolder;
import com.wolfyscript.bundleinv.client.gui.screen.InvScreenRefresh;
import com.wolfyscript.bundleinv.network.packets.BundleStorageDataPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

@Environment(EnvType.CLIENT)
public class BundleInvClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BundleInvConstants.S2C_BUNDLE_STORAGE_UPDATE_PACKET, (client, handler, buf, responseSender) -> {
            boolean add = buf.readBoolean();
            int count = buf.readInt();
            ItemStack itemStack = buf.readItemStack();
            itemStack.setCount(count);
            client.executeSync(() -> {
                if (add) {
                    ((BundleStorageHolder)client.player.getInventory()).getBundleStorage().addStack(itemStack);
                } else {
                    ((BundleStorageHolder)client.player.getInventory()).getBundleStorage().removeStack(itemStack, count);
                }
                if (client.currentScreen instanceof InvScreenRefresh inventoryScreen) {
                    inventoryScreen.refreshWidgets();
                }
           });
        });
        BundleStorageDataPacket.registerClientReceiver(this);
    }

    public static void sendBundleStorageStoreItem(boolean cursor, ItemStack itemStack) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(cursor);
        buf.writeInt(itemStack.getCount());
        ClientPlayNetworking.send(BundleInvConstants.C2S_BUNDLE_STORAGE_STORE_ITEM, buf);
    }

}
