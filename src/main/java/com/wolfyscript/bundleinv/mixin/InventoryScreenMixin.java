package com.wolfyscript.bundleinv.mixin;

import com.wolfyscript.bundleinv.client.gui.screen.AbstractInventoryScreenInterface;
import com.wolfyscript.bundleinv.client.gui.screen.BundleStorageWidget;
import com.wolfyscript.bundleinv.client.gui.screen.InvScreenRefresh;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements InvScreenRefresh {

    private static final Identifier BUNDLE_STORAGE_TEXTURE = new Identifier("bundleinv", "textures/gui/container/bundle_storage.png");
    private static final Identifier RECIPE_BUTTON_TEXTURE = new Identifier("textures/gui/recipe_button.png");
    private static final int RECIPE_BOOK_BUTTON_OFFSET = 104;
    private static final int BUNDLE_STORAGE_BUTTON_OFFSET = 140;

    @Shadow
    private boolean narrow;
    @Shadow
    private boolean mouseDown;
    @Final
    @Shadow
    private RecipeBookWidget recipeBook;
    @Shadow private float mouseX;
    @Shadow private float mouseY;

    @Shadow public abstract void refreshRecipeBook();

    private BundleStorageWidget bundleStorage;
    private List<Consumer<Integer>> updateButtonCallbacks = new LinkedList<>();

    private InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    protected int getTotalWidth() {
        return backgroundWidth + ((AbstractInventoryScreenInterface)this).getLeftWidth();
    }

    protected boolean canExpandRecipeBook() {
        return (width - getTotalWidth()) > 87;
    }

    @Inject(method = "handledScreenTick",at = @At("TAIL"))
    private void update(CallbackInfo ci) {
        bundleStorage.update();
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    private void constructor(PlayerEntity player, CallbackInfo ci) {
        this.bundleStorage = new BundleStorageWidget();
        this.updateButtonCallbacks = new LinkedList<>();
    }

    @Inject(at = @At("HEAD"), method = "init")
    public void initHead(CallbackInfo ci) {
        updateButtonCallbacks.clear();
    }

    @Inject(method = "init", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;x:I", shift = At.Shift.AFTER, ordinal = 0))
    public void initBundleStorageXPosition(CallbackInfo ci) {
        bundleStorage.init(width, height, client, narrow, handler);
        x = bundleStorage.findLeftEdge(x, width, backgroundWidth, recipeBook.isOpen());
        ((AbstractInventoryScreenInterface) this).setLeftWidth(bundleStorage.isOpen() ? BundleStorageWidget.WIDTH + 1 : 0);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        TexturedButtonWidget bundleStorageButton = new TexturedButtonWidget(this.x + BUNDLE_STORAGE_BUTTON_OFFSET, this.height / 2 - 22, 20, 18, 111, 0, 19, BUNDLE_STORAGE_TEXTURE, button -> {
            if (recipeBook.isOpen()) {
                if (narrow) return;
                recipeBook.toggleOpen();
            }
            bundleStorage.toggleOpen();
            x = bundleStorage.findLeftEdge(x, width, backgroundWidth, recipeBook.isOpen());
            updateButtons();
            this.mouseDown = true;
        });
        updateButtonCallbacks.add(x -> bundleStorageButton.setPos(x + 140, this.height / 2 - 22));
        addDrawableChild(bundleStorageButton);
        this.addSelectableChild(bundleStorage);
        this.setInitialFocus(bundleStorage);
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;", ordinal = 0))
    public Element init2(InventoryScreen instance, Element element) {
        if (element instanceof TexturedButtonWidget) {
            TexturedButtonWidget recipeBookButton = new TexturedButtonWidget(this.x + RECIPE_BOOK_BUTTON_OFFSET, this.height / 2 - 22, 20, 18, 0, 0, 19, RECIPE_BUTTON_TEXTURE, button -> {
                if (bundleStorage.isOpen()) {
                    bundleStorage.toggleOpen();
                }
                recipeBook.toggleOpen();
                x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
                updateButtons();
                mouseDown = true;
            });
            updateButtonCallbacks.add(x -> recipeBookButton.setPos(x + 104, this.height / 2 - 22));
            return addDrawableChild(recipeBookButton);
        }
        return element;
    }

    private void updateButtons() {
        updateButtonCallbacks.forEach(update -> update.accept(x));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"))
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        bundleStorage.render(matrices, mouseX, mouseY, delta);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;drawTooltip(Lnet/minecraft/client/util/math/MatrixStack;IIII)V"))
    public void drawTooltips(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        bundleStorage.drawTooltip(matrices, x, y, mouseX, mouseY);
    }

    @Inject(at = @At("HEAD"), method = "drawBackground")
    public void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY, CallbackInfo ci) {

    }

    @Inject(method = "isClickOutsideBounds", at = @At("RETURN"), cancellable = true)
    private void modifyIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        boolean clickedOutside = bundleStorage.isClickOutsideBounds(mouseX, mouseY, left, top, backgroundWidth, backgroundHeight, button);
        cir.setReturnValue(cir.getReturnValue() && clickedOutside);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        bundleStorage.mouseReleased(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return bundleStorage.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        bundleStorage.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void refreshWidgets() {
        refreshRecipeBook();
        bundleStorage.refresh();
    }

}
