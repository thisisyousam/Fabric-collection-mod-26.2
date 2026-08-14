package com.yousam.collection.mixin;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public abstract class FontBoldMixin {

    private static final FontDescription WANTEDSANS_BOLD = new FontDescription.Resource(Identifier.withDefaultNamespace("wantedsans_bold"));

    @ModifyVariable(method = "accept(ILnet/minecraft/network/chat/Style;I)Z", at = @At("HEAD"), argsOnly = true)
    private Style collection$useRealBoldFont(Style style) {
        if (style.isBold() && style.getFont().equals(FontDescription.DEFAULT)) {
            return style.withFont(WANTEDSANS_BOLD).withBold(false);
        }
        return style;
    }
}
