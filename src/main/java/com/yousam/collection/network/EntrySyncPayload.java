package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import com.yousam.collection.data.CollectionCategory;
import com.yousam.collection.data.CollectionEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record EntrySyncPayload(List<EntryData> entries) implements CustomPacketPayload {

    public record EntryData(
            Identifier id,
            Identifier itemId,
            int required,
            CollectionCategory category,
            String componentKey,   // nullable -> Optional로 직렬화
            String componentValue
    ) {}

    public static final Type<EntrySyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "entry_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, EntryData> ENTRY_DATA_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, EntryData::id,
                    Identifier.STREAM_CODEC, EntryData::itemId,
                    ByteBufCodecs.VAR_INT, EntryData::required,
                    ByteBufCodecs.idMapper(i -> CollectionCategory.values()[i], CollectionCategory::ordinal), EntryData::category,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), e -> Optional.ofNullable(e.componentKey()),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), e -> Optional.ofNullable(e.componentValue()),
                    (id, itemId, required, category, compKey, compValue) -> new EntryData(
                            id, itemId, required, category, compKey.orElse(null), compValue.orElse(null)
                    )
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, EntrySyncPayload> STREAM_CODEC =
            ENTRY_DATA_CODEC.apply(ByteBufCodecs.list()).map(EntrySyncPayload::new, EntrySyncPayload::entries);

    public static EntrySyncPayload fromEntries(java.util.Map<Identifier, CollectionEntry> entries) {
        return new EntrySyncPayload(
                entries.values().stream()
                        .map(e -> new EntryData(e.id(), e.itemId(), e.requiredCount(), e.category(), e.componentKey(), e.componentValue()))
                        .toList()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}