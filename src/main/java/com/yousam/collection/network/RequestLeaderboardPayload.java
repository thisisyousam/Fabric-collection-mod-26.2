package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestLeaderboardPayload() implements CustomPacketPayload {
    public static final Type<RequestLeaderboardPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "request_leaderboard"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestLeaderboardPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestLeaderboardPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}