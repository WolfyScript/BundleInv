package com.wolfyscript.bundleinv.mixin;

import com.wolfyscript.bundleinv.client.gui.screen.AbstractInventoryScreenInterface;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin<T extends ScreenHandler> extends HandledScreen<T> implements AbstractInventoryScreenInterface {

    private int leftWidth;

    private AbstractInventoryScreenMixin(T handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    public void setLeftWidth(int leftWidth) {
        this.leftWidth = leftWidth;
    }

    @Override
    public int getLeftWidth() {
        return leftWidth;
    }

    @Redirect(method = "hideStatusEffectHud", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractInventoryScreen;backgroundWidth:I", opcode = Opcodes.GETFIELD))
    private int editBackgroundWidth0(AbstractInventoryScreen<T> instance) {
        return backgroundWidth + leftWidth;
    }

    @Redirect(method = "drawStatusEffects", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/AbstractInventoryScreen;backgroundWidth:I", opcode = Opcodes.GETFIELD))
    private int editBackgroundWidth(AbstractInventoryScreen<T> instance) {
        return backgroundWidth + leftWidth;
    }

    @Inject(method = "drawStatusEffects", at = @At(value = "TAIL"))
    private void drawStatusEffects(MatrixStack matrices, int mouseX, int mouseY, CallbackInfo ci) {

    }

}
