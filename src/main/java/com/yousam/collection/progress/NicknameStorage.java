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

public class NicknameStorage extends SavedData {

    private final Map<String, String> nicknames;

    private static final Codec<Map<String, String>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap(HashMap::new, map -> map);

    private static final Codec<NicknameStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MAP_CODEC.fieldOf("nicknames").forGetter(state -> state.nicknames)
    ).apply(instance, NicknameStorage::new));

    public static final SavedDataType<NicknameStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "nickname_storage"),
            () -> new NicknameStorage(new HashMap<>()),
            CODEC,
            null
    );

    private NicknameStorage(Map<String, String> nicknames) {
        this.nicknames = nicknames;
    }

    public void set(UUID playerId, String nickname) {
        nicknames.put(playerId.toString(), nickname);
        setDirty();
    }

    public Map<UUID, String> getAll() {
        Map<UUID, String> result = new HashMap<>();
        for (var e : nicknames.entrySet()) {
            result.put(UUID.fromString(e.getKey()), e.getValue());
        }
        return result;
    }

    public String get(UUID playerId) {
        return nicknames.get(playerId.toString());
    }
}