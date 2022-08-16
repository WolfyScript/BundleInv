package com.wolfyscript.bundleinv.network.packets;

import com.wolfyscript.bundleinv.BundleInvConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

public class C2SPickFromBundleStorage {

    public static void sendToServer(Action action, int hotbarSlot, ItemStack stack) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(action);
        buf.writeInt(hotbarSlot);
        buf.writeItemStack(stack);
        ClientPlayNetworking.send(BundleInvConstants.C2S_PICK_FROM_BUNDLE_STORAGE, buf);
    }

    public enum Action {
        SWAP, REPLENISH
    }

}
