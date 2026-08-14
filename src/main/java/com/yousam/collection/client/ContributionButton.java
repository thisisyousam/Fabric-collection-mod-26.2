package com.yousam.collection.client;

import com.yousam.collection.CollectionMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ContributionButton extends AbstractButton {

    private static final Identifier TEXTURE_NORMAL =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "textures/gui/contribution_button.png");
    private static final Identifier TEXTURE_HOVER =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "textures/gui/contribution_button_hover.png");

    private final OnPress onPress;

    public ContributionButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPress.onPress(this);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Identifier texture = this.isHoveredOrFocused() ? TEXTURE_HOVER : TEXTURE_NORMAL;

        graphics.blit(texture,
                this.getX() + 3, this.getY() + 2,
                this.getX() + this.getWidth() + 3, this.getY() + this.getHeight() + 2,
                0f, 1f, 0f, 1f);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(ContributionButton button);
    }
}