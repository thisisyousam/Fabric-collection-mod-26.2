package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubmitItemPayload(Identifier entryId) implements CustomPacketPayload {

    public static final Type<SubmitItemPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "submit_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitItemPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    (payload, buf) -> buf.writeIdentifier(payload.entryId()),
                    buf -> new SubmitItemPayload(buf.readIdentifier())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}