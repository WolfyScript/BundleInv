package com.wolfyscript.bundleinv.mixin;

import com.wolfyscript.bundleinv.BundleStorageHolder;
import com.wolfyscript.bundleinv.PlayerBundleStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin implements BundleStorageHolder {

    private PlayerBundleStorage bundleStorage;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(PlayerEntity player, CallbackInfo ci) {
        this.bundleStorage = new PlayerBundleStorage((PlayerInventory)(Object) this, player);
    }

    @Override
    public PlayerBundleStorage getBundleStorage() {
        return bundleStorage;
    }
}
