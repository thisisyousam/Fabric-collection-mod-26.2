package com.yousam.collection.mixin;

import com.yousam.collection.client.ClientNicknameCache;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void collection$replaceNickname(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player player) {
            String nickname = ClientNicknameCache.get(player.getUUID());
            if (nickname != null) {
                cir.setReturnValue(Component.literal(nickname));
            }
        }
    }
}