package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UsePassTicketPayload(Identifier entryId) implements CustomPacketPayload {

    public static final Type<UsePassTicketPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "use_pass_ticket"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UsePassTicketPayload> STREAM_CODEC =
            CustomPacketPayload.codec(
                    (payload, buf) -> buf.writeIdentifier(payload.entryId()),
                    buf -> new UsePassTicketPayload(buf.readIdentifier())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
