package com.wolfyscript.bundleinv.mixin;

import com.wolfyscript.bundleinv.BundleStorageHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Shadow @Final private PlayerInventory inventory;

    @Inject(method = "writeCustomDataToNbt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V", shift = At.Shift.AFTER))
    private void writeBundleInvNBT(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("bundleStorage", ((BundleStorageHolder) inventory).getBundleStorage().writeNbt(new NbtCompound()));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;readNbt(Lnet/minecraft/nbt/NbtList;)V", shift = At.Shift.AFTER))
    private void readBundleInvNBT(NbtCompound nbt, CallbackInfo ci) {
        ((BundleStorageHolder) inventory).getBundleStorage().readNbt(nbt.getCompound("bundleStorage"));
    }

}
