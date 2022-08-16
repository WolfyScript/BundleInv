package com.wolfyscript.bundleinv.mixin;

import com.wolfyscript.bundleinv.BundleStorageHolder;
import com.wolfyscript.bundleinv.PlayerBundleStorage;
import com.wolfyscript.bundleinv.network.packets.C2SPickFromBundleStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "doItemPick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSlotWithStack(Lnet/minecraft/item/ItemStack;)I", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION, cancellable = true)
    private void pickItemFromBundleInventory(CallbackInfo ci, boolean creative, BlockEntity blockEntity, ItemStack itemStack, HitResult.Type type, PlayerInventory playerInventory) {
        assert player != null;
        int itemSlot = playerInventory.getSlotWithStack(itemStack);
        if (!creative) {
            PlayerBundleStorage bundleStorage = ((BundleStorageHolder) player.getInventory()).getBundleStorage();
            if (itemSlot == -1) {
                int bundleIndex = bundleStorage.getStacks().indexOf(itemStack);
                if (bundleIndex != -1) {
                    C2SPickFromBundleStorage.sendToServer(C2SPickFromBundleStorage.Action.SWAP, itemSlot, itemStack);
                    ci.cancel();
                }
            } else if (PlayerInventory.isValidHotbarIndex(itemSlot)) {
                ItemStack hotbarStack = playerInventory.getStack(itemSlot);
                if (hotbarStack.getCount() < hotbarStack.getMaxCount() && bundleStorage.indexOf(hotbarStack) != -1) {
                    C2SPickFromBundleStorage.sendToServer(C2SPickFromBundleStorage.Action.REPLENISH, itemSlot, itemStack);
                    ci.cancel();
                }
            }
        }
    }

}
