package com.yousam.collection.client;

import com.yousam.collection.CollectionMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class TabButton extends AbstractButton {

    private static final Identifier TEXTURE_SELECTED =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "textures/gui/tab_selected.png");
    private static final Identifier TEXTURE_UNSELECTED =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "textures/gui/tab_unselected.png");

    private final Supplier<Boolean> isSelected;
    private final ItemStack icon;
    private final String label;
    private final OnPress onPress;

    public TabButton(int x, int y, int width, int height, Component message, String label,
                     ItemStack icon, Supplier<Boolean> isSelected, OnPress onPress) {
        super(x, y, width, height, message);
        this.label = label;
        this.icon = icon;
        this.isSelected = isSelected;
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPress.onPress(this);
    }

    private static final float ICON_SCALE = 0.5f;
    private static final float LABEL_SCALE = 0.48f;

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Identifier texture = isSelected.get() ? TEXTURE_SELECTED : TEXTURE_UNSELECTED;

        graphics.blit(texture,
                this.getX(), this.getY(),
                this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                0f, 1f, 0f, 1f);

        int centerX = this.getX() + this.getWidth() / 2;
        int centerY = this.getY() + this.getHeight() / 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(ICON_SCALE, ICON_SCALE);
        graphics.item(icon, -8, -14);
        graphics.pose().popMatrix();

        // 라벨 텍스트 (축소해서 그리기)
        int labelY = this.getY() + this.getHeight() + 2;
        var font = net.minecraft.client.Minecraft.getInstance().font;

//        Style boldStyle = Style.EMPTY.withBold(true);
//        Component boldLabel = Component.literal(label).setStyle(boldStyle);
        int textWidth = font.width(label);

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, labelY);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE);
        graphics.text(
                font,
                label,
                -textWidth / 2,
                -19,
                0xFF000000,
                false
        );
        graphics.pose().popMatrix();
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(TabButton button);
    }
}