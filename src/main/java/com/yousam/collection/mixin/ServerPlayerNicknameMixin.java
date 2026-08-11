package com.yousam.collection.mixin;

import com.yousam.collection.progress.CollectionProgressManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerNicknameMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void collection$tabListNickname(CallbackInfoReturnable<@Nullable Component> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        String nickname = CollectionProgressManager.getNicknameStorage(self.level()).get(self.getUUID());
        if (nickname != null) {
            cir.setReturnValue(Component.literal(nickname));
        }
    }
}