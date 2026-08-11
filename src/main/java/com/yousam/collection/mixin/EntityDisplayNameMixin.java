package com.yousam.collection.mixin;

import com.yousam.collection.progress.CollectionProgressManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class EntityDisplayNameMixin {

    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void collection$nicknameGetName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer serverPlayer) {
            String nickname = CollectionProgressManager.getNicknameStorage((ServerLevel) serverPlayer.level()).get(serverPlayer.getUUID());
            if (nickname != null) {
                cir.setReturnValue(Component.literal(nickname));
            }
        }
    }
}