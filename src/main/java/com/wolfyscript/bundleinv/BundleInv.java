package com.wolfyscript.bundleinv;

import com.wolfyscript.bundleinv.network.packets.BundleStorageDataPacket;
import com.wolfyscript.bundleinv.network.packets.C2SPickFromBundleStorage;
import com.wolfyscript.bundleinv.util.collection.IndexedSortedArraySet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
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
            int count = buf.readInt();
            server.executeSync(() -> {
                if (player.currentScreenHandler == player.playerScreenHandler) {
                    if (cursor) {
                        ItemStack cursorStack = player.playerScreenHandler.getCursorStack();
                        if (cursorStack == null || cursorStack.isEmpty()) return;
                        PlayerBundleStorage storage = ((BundleStorageHolder) player.getInventory()).getBundleStorage();

                        ItemStack toAdd = cursorStack.copy();
                        toAdd.setCount(count);
                        int remaining = storage.addStack(toAdd);
                        cursorStack.setCount(cursorStack.getCount() - count + remaining);
                        player.getInventory().markDirty();
                        player.playerScreenHandler.syncState();
                    }
                }
            });
        });
        BundleStorageDataPacket.registerServerReceiver(this);
        ServerPlayNetworking.registerGlobalReceiver(BundleInvConstants.C2S_PICK_FROM_BUNDLE_STORAGE, (server, player, handler, buf, responseSender) -> {
            C2SPickFromBundleStorage.Action action = buf.readEnumConstant(C2SPickFromBundleStorage.Action.class);
            int hotbarSlot = buf.readInt();
            ItemStack stackPicked = buf.readItemStack();

            server.executeSync(() -> {
                PlayerInventory inventory = player.getInventory();
                PlayerBundleStorage bundleStorage = ((BundleStorageHolder) inventory).getBundleStorage();

                if (action.equals(C2SPickFromBundleStorage.Action.REPLENISH)) {
                    ItemStack hotbarStack = inventory.getStack(hotbarSlot);
                    if (hotbarStack.getCount() >= hotbarStack.getMaxCount()) return;
                    int remaining = hotbarStack.getMaxCount() - hotbarStack.getCount();
                    if (remaining > 0) {
                        int index = bundleStorage.indexOf(hotbarStack);
                        if (index != -1) {
                            ItemStack removed = bundleStorage.removeStack(index, remaining);
                            inventory.selectedSlot = hotbarSlot;
                            hotbarStack.setCount(hotbarStack.getCount() + removed.getCount());

                            sendBundleStorageUpdate(player, false, removed);
                            handler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, inventory.selectedSlot, inventory.getStack(inventory.selectedSlot)));
                            handler.sendPacket(new UpdateSelectedSlotS2CPacket(inventory.selectedSlot));
                        }
                    }
                } else {
                    ItemStack bundleStack = bundleStorage.removeMaxStack(stackPicked);
                    int updatedHotbarSlot = bundleStorage.getSwappableHotbarSlotFor(bundleStack);
                    ItemStack hotbarItem = inventory.getStack(updatedHotbarSlot);

                    if (!hotbarItem.isEmpty()) {
                        // The hotbar item needs to be swapped. The hotbar and bundle item can have different load factors inside the bundle inventory, so they might not be able to swap!
                        int bundleItemLoad = PlayerBundleStorage.getItemStackLoad(bundleStack);
                        int hotbarItemLoad = PlayerBundleStorage.getItemStackLoad(hotbarItem);

                        if (bundleStorage.getRemainingCapacity() - bundleItemLoad >= hotbarItemLoad) {
                            inventory.selectedSlot = updatedHotbarSlot;
                            bundleStorage.addStack(hotbarItem);
                            inventory.setStack(updatedHotbarSlot, bundleStack);
                            sendBundleStorageUpdate(player, true, hotbarItem);
                            sendBundleStorageUpdate(player, false, bundleStack);
                            handler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, inventory.selectedSlot, inventory.getStack(inventory.selectedSlot)));
                            handler.sendPacket(new UpdateSelectedSlotS2CPacket(inventory.selectedSlot));
                        } else {
                            // There would be no space for the swapped items! what do?
                            // For now, do nothing, maybe I find a solution later on.
                        }
                    } else {
                        // Move item from Bundle into the hotbar slot
                        inventory.selectedSlot = updatedHotbarSlot;
                        inventory.setStack(updatedHotbarSlot, bundleStack);
                        sendBundleStorageUpdate(player, false, bundleStack);
                        handler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, inventory.selectedSlot, inventory.getStack(inventory.selectedSlot)));
                        handler.sendPacket(new UpdateSelectedSlotS2CPacket(inventory.selectedSlot));
                    }
                }
            });
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            //Send bundle storage to client
            PlayerBundleStorage bundleStorage = ((BundleStorageHolder) handler.player.getInventory()).getBundleStorage();
            IndexedSortedArraySet<ItemStack> stacks = bundleStorage.getStacks();
            BundleStorageDataPacket.sendToClient(handler.player, bundleStorage.isOpen());
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
