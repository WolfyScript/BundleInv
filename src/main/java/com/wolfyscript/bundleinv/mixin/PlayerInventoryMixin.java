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

        if (player instanceof ServerPlayerEntity serverPlayer) {
            bundleStorage.addStack(new ItemStack(Items.STONE, 80));
            bundleStorage.addStack(new ItemStack(Items.STONE, 80));
            bundleStorage.addStack(new ItemStack(Items.OAK_WOOD, 80));
            bundleStorage.addStack(new ItemStack(Items.ENDER_PEARL, 16));
            bundleStorage.addStack(new ItemStack(Items.OBSIDIAN, 100));
            bundleStorage.addStack(new ItemStack(Items.DIAMOND, 90));
            bundleStorage.addStack(new ItemStack(Items.EMERALD, 90));
            bundleStorage.addStack(new ItemStack(Items.EMERALD_BLOCK, 90));
            bundleStorage.addStack(new ItemStack(Items.OAK_PLANKS, 30));
            bundleStorage.addStack(new ItemStack(Items.ACACIA_FENCE, 30));
            bundleStorage.addStack(new ItemStack(Items.ACACIA_BUTTON, 30));
            bundleStorage.addStack(new ItemStack(Items.BLACKSTONE, 30));
            bundleStorage.addStack(new ItemStack(Items.BLACKSTONE_SLAB, 30));
            bundleStorage.addStack(new ItemStack(Items.BLACKSTONE_STAIRS, 30));
            bundleStorage.addStack(new ItemStack(Items.BLACKSTONE_WALL, 30));
            /*
            bundleStorage.addStack(new ItemStack(Items.BLACK_CANDLE, 30));
            bundleStorage.addStack(new ItemStack(Items.BLACK_WOOL, 30));
            bundleStorage.addStack(new ItemStack(Items.BLACK_CARPET, 30));
            bundleStorage.addStack(new ItemStack(Items.AMETHYST_BLOCK, 30));
            bundleStorage.addStack(new ItemStack(Items.ACTIVATOR_RAIL, 30));

             */
        }
    }

    @Override
    public PlayerBundleStorage getBundleStorage() {
        return bundleStorage;
    }
}
