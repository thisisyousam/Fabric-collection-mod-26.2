package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record NicknameSyncPayload(Map<UUID, String> nicknames) implements CustomPacketPayload {

    public static final Type<NicknameSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "nickname_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NicknameSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.STRING_UTF8),
                    NicknameSyncPayload::nicknames,
                    NicknameSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}