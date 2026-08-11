package com.yousam.collection.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void collection$hideCraftingLabel(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, CallbackInfo ci) {
        ci.cancel();
    }
}
