package com.yousam.collection.command;

import com.yousam.collection.network.ModNetworking;
import com.yousam.collection.network.NicknameSyncPayload;
import com.yousam.collection.progress.CollectionProgressManager;
import com.yousam.collection.progress.NicknameStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class ModCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("도감리셋")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {
                        ServerPlayer executor = context.getSource().getPlayerOrException();

                        CollectionProgressManager.get(executor.level()).resetAll();

                        context.getSource().sendSuccess(() -> Component.literal("도감이 초기화되었습니다."), true);

                        for (ServerPlayer p : PlayerLookup.level(executor.level())) {
                            ModNetworking.sendProgressSync(p);
                        }
                        return 1;
                    })
            );
        });
    }

    public static void broadcastNicknames(ServerPlayer any) {
        NicknameStorage storage = CollectionProgressManager.getNicknameStorage(any.level());
        var payload = new NicknameSyncPayload(storage.getAll());
        for (ServerPlayer p : PlayerLookup.all(any.level().getServer())) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}