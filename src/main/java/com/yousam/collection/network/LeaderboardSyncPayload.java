package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record LeaderboardSyncPayload(List<PlayerContribution> entries) implements CustomPacketPayload {

    public record PlayerContribution(String playerName, int total) {}

    public static final Type<LeaderboardSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "leaderboard_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerContribution> ENTRY_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, PlayerContribution::playerName,
                    ByteBufCodecs.VAR_INT, PlayerContribution::total,
                    PlayerContribution::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardSyncPayload> STREAM_CODEC =
            ENTRY_CODEC.apply(ByteBufCodecs.list()).map(LeaderboardSyncPayload::new, LeaderboardSyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
