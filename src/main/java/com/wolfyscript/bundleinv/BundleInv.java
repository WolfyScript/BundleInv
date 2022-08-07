package com.wolfyscript.bundleinv;

import com.wolfyscript.bundleinv.util.collection.IndexedSortedArraySet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class BundleInv implements ModInitializer {

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(BundleInvConstants.C2S_BUNDLE_ITEM_CONTAINER_CLICK_PACKET, (server, player, handler, buf, responseSender) -> {
            int id = buf.readByte();
            int count = buf.readInt();
            ItemStack itemClicked = buf.readItemStack();

            //Look for open inventory and then change cursor with the clicked item
            server.executeSync(() -> {
                if (player.currentScreenHandler == player.playerScreenHandler && player.playerScreenHandler.onServer) {
                    PlayerBundleStorage storage = ((BundleStorageHolder) player.getInventory()).getBundleStorage();
                    ItemStack itemStack = storage.removeStack(itemClicked, count);
                    if (!itemStack.isEmpty()) { //Make sure the requested item actually was in the storage
                        player.playerScreenHandler.setCursorStack(itemStack);
                        sendBundleStorageUpdate(player, false, itemStack);
                    }
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(BundleInvConstants.C2S_BUNDLE_STORAGE_STORE_ITEM, (server, player, handler, buf, responseSender) -> {
            boolean cursor = buf.readBoolean();
            ItemStack itemStack = buf.readItemStack(); //TODO: actually not needed!
            server.executeSync(() -> {
                if (player.currentScreenHandler == player.playerScreenHandler) {
                    if (cursor) {
                        ItemStack cursorStack = player.playerScreenHandler.getCursorStack();
                        if (cursorStack == null || cursorStack.isEmpty()) return;
                        PlayerBundleStorage storage = ((BundleStorageHolder) player.getInventory()).getBundleStorage();
                        int remaining = storage.addStack(cursorStack);
                        cursorStack.setCount(remaining);
                        player.getInventory().markDirty();
                    }
                }
            });

        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            //Send bundle storage to client
            IndexedSortedArraySet<ItemStack> stacks = ((BundleStorageHolder) handler.player.getInventory()).getBundleStorage().getStacks();
            for (ItemStack stack : stacks) {
                sendBundleStorageUpdate(handler.player, true, stack);
            }
        });
    }

    public static void sendBundleStorageUpdate(ServerPlayerEntity player, boolean add, ItemStack itemStack) {
        PacketByteBuf packetBuf = PacketByteBufs.create();
        packetBuf.writeBoolean(add);
        packetBuf.writeInt(itemStack.getCount());
        packetBuf.writeItemStack(itemStack);
        ServerPlayNetworking.send(player, BundleInvConstants.S2C_BUNDLE_STORAGE_UPDATE_PACKET, packetBuf);
    }
}
