package com.wolfyscript.bundleinv.network.packets;

import com.wolfyscript.bundleinv.BundleInv;
import com.wolfyscript.bundleinv.BundleStorageHolder;
import com.wolfyscript.bundleinv.client.BundleInvClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class BundleStorageDataPacket {

    public static final Identifier KEY = new Identifier("bundleinv", "bundle_storage_data");

    public static void registerServerReceiver(BundleInv bundleInv) {
        ServerPlayNetworking.registerGlobalReceiver(KEY, (server, player, handler, buf, responseSender) -> {
            boolean open = buf.readBoolean();
            server.executeSync(() -> {
                if (player.currentScreenHandler == player.playerScreenHandler || !open) {
                    ((BundleStorageHolder) player.getInventory()).getBundleStorage().setOpen(open);
                }
            });
        });
    }

    public static void registerClientReceiver(BundleInvClient bundleInvClient) {
        ClientPlayNetworking.registerGlobalReceiver(KEY, (client, handler, buf, responseSender) -> {
            boolean open = buf.readBoolean();
            client.execute(() -> {
                if (client.player != null) {
                    ((BundleStorageHolder) client.player.getInventory()).getBundleStorage().setOpen(open);
                }
            });
        });
    }

    private static PacketByteBuf create(boolean open) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(open);
        return buf;
    }

    public static void sendToServer(boolean open) {
        ClientPlayNetworking.send(KEY, create(open));
    }

    public static void sendToClient(ServerPlayerEntity player, boolean open) {
        ServerPlayNetworking.send(player, KEY, create(open));
    }

}
