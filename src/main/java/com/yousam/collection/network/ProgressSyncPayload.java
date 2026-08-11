package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Map;

public record ProgressSyncPayload(Map<Identifier, Integer> submitted) implements CustomPacketPayload {

    public static final Type<ProgressSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "progress_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(
                            java.util.HashMap::new,
                            Identifier.STREAM_CODEC,
                            ByteBufCodecs.VAR_INT
                    ),
                    ProgressSyncPayload::submitted,
                    ProgressSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}