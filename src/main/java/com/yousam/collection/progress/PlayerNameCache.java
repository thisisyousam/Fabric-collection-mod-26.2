package com.yousam.collection.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yousam.collection.CollectionMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNameCache extends SavedData {

    private final Map<String, String> uuidToName;

    private static final Codec<Map<String, String>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap(HashMap::new, map -> map);

    private static final Codec<PlayerNameCache> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MAP_CODEC.fieldOf("names").forGetter(state -> state.uuidToName)
    ).apply(instance, PlayerNameCache::new));

    public static final SavedDataType<PlayerNameCache> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "player_name_cache"),
            () -> new PlayerNameCache(new HashMap<>()),
            CODEC,
            null
    );

    private PlayerNameCache(Map<String, String> uuidToName) {
        this.uuidToName = uuidToName;
    }

    public void record(UUID playerId, String name) {
        String key = playerId.toString();
        if (!name.equals(uuidToName.get(key))) {
            uuidToName.put(key, name);
            setDirty();
        }
    }

    public String get(UUID playerId) {
        return uuidToName.get(playerId.toString());
    }
}