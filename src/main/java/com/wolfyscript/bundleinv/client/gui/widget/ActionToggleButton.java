package com.wolfyscript.bundleinv.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ActionToggleButton extends ButtonWidget {

    protected Identifier texture;
    protected boolean toggled;
    protected int pressedUOffset;
    private final int u;
    private final int v;
    private final int hoveredVOffset;
    private final int textureWidth;
    private final int textureHeight;

    public ActionToggleButton(int x, int y, int width, int height, int u, int v, boolean toggled, Identifier texture, PressAction pressAction) {
        this(x, y, width, height, u, v, height, toggled, texture, 256, 256, pressAction);
    }

    public ActionToggleButton(int x, int y, int width, int height, int u, int v, int hoveredVOffset, boolean toggled, Identifier texture, PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, toggled, texture, 256, 256, pressAction);
    }

    public ActionToggleButton(int x, int y, int width, int height, int u, int v, int hoveredVOffset, boolean toggled, Identifier texture, int textureWidth, int textureHeight, PressAction pressAction) {
        this(x, y, width, height, u, v, hoveredVOffset, toggled, texture, textureWidth, textureHeight, pressAction, ScreenTexts.EMPTY);
    }

    public ActionToggleButton(int x, int y, int width, int height, int u, int v, int hoveredVOffset, boolean toggled, Identifier texture, int textureWidth, int textureHeight, PressAction pressAction, Text text) {
        this(x, y, width, height, u, v, hoveredVOffset, toggled, texture, textureWidth, textureHeight, pressAction, EMPTY, text);
    }

    public ActionToggleButton(int x, int y, int width, int height, int u, int v, int hoveredVOffset, boolean toggled, Identifier texture, int textureWidth, int textureHeight, PressAction pressAction, TooltipSupplier tooltipSupplier, Text text) {
        super(x, y, width, height, text, pressAction, tooltipSupplier);
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.u = u;
        this.v = v;
        this.hoveredVOffset = hoveredVOffset;
        this.toggled = toggled;
        this.texture = texture;
    }

    public void setPressedOffset(int pressedUOffset, Identifier texture) {
        this.pressedUOffset = pressedUOffset;
        this.texture = texture;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isToggled() {
        return this.toggled;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, this.texture);
        RenderSystem.disableDepthTest();
        int i = this.u;
        int j = this.v;
        if (this.toggled) {
            i += this.pressedUOffset;
        }
        if (this.isHovered()) {
            j += this.hoveredVOffset;
        }
        this.drawTexture(matrices, this.x, this.y, i, j, this.width, this.height);
        RenderSystem.enableDepthTest();
    }
}
