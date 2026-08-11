package com.yousam.collection.client;

import com.yousam.collection.network.EntrySyncPayload;
import com.yousam.collection.network.LeaderboardSyncPayload;
import com.yousam.collection.network.NicknameSyncPayload;
import com.yousam.collection.network.ProgressSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworkHandler {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EntrySyncPayload.TYPE, (payload, context) -> {
            ClientCollectionCache.setEntries(payload.entries());
        });

        ClientPlayNetworking.registerGlobalReceiver(ProgressSyncPayload.TYPE, (payload, context) -> {
            ClientCollectionCache.setSubmitted(payload.submitted());
        });

        ClientPlayNetworking.registerGlobalReceiver(NicknameSyncPayload.TYPE, (payload, context) -> {
            ClientNicknameCache.set(payload.nicknames());
        });

        ClientPlayNetworking.registerGlobalReceiver(LeaderboardSyncPayload.TYPE, (payload, context) -> {
            CollectionScreen.ClientLeaderboardCache.set(payload.entries());
        });
    }
}