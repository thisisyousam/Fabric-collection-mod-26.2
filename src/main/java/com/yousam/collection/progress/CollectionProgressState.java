package com.yousam.collection.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yousam.collection.CollectionMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CollectionProgressState extends SavedData {

    // entryId -> 전체 제출 개수 (모든 플레이어 공유)
    private final Map<String, Integer> globalProgress;

    // playerUUID -> (entryId -> 그 플레이어가 제출한 개수) - 기여도 집계용
    private final Map<String, Map<String, Integer>> playerContributions;

    // 마지막으로 1위 변경 알림을 보낸 시점의 1등 UUID. null이면 아직 한 번도 기록된 적 없음(서버 최초 기동 등)
    private UUID currentFirstPlace;

    private static final Codec<Map<String, Integer>> INT_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(HashMap::new, map -> map);

    private static final Codec<Map<String, Map<String, Integer>>> CONTRIBUTION_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, INT_MAP_CODEC).xmap(HashMap::new, map -> map);

    private static final Codec<CollectionProgressState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            INT_MAP_CODEC.fieldOf("globalProgress").forGetter(state -> state.globalProgress),
            CONTRIBUTION_MAP_CODEC.fieldOf("playerContributions").forGetter(state -> state.playerContributions),
            Codec.STRING.optionalFieldOf("currentFirstPlace").forGetter(state ->
                    java.util.Optional.ofNullable(state.currentFirstPlace).map(UUID::toString))
    ).apply(instance, (globalProgress, playerContributions, currentFirstPlace) -> new CollectionProgressState(
            globalProgress, playerContributions, currentFirstPlace.map(UUID::fromString).orElse(null)
    )));

    public static final SavedDataType<CollectionProgressState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "collection_progress"),
            () -> new CollectionProgressState(new HashMap<>(), new HashMap<>(), null),
            CODEC,
            null
    );

    private CollectionProgressState(Map<String, Integer> globalProgress, Map<String, Map<String, Integer>> playerContributions, UUID currentFirstPlace) {
        this.globalProgress = globalProgress;
        this.playerContributions = playerContributions;
        this.currentFirstPlace = currentFirstPlace;
    }

    // 전체 공유 진행도 조회 (플레이어 무관)
    public int getSubmitted(Identifier entryId) {
        return globalProgress.getOrDefault(entryId.toString(), 0);
    }

    public boolean isComplete(Identifier entryId, int required) {
        return getSubmitted(entryId) >= required;
    }

    // 제출 시: 전체 진행도 + 개인 기여도 둘 다 갱신
    public void addSubmitted(UUID playerId, Identifier entryId, int amount) {
        globalProgress.merge(entryId.toString(), amount, Integer::sum);
        playerContributions
                .computeIfAbsent(playerId.toString(), k -> new HashMap<>())
                .merge(entryId.toString(), amount, Integer::sum);
        setDirty();
    }

    // 기여도 리더보드용: 플레이어별 총 제출 개수
    public Map<UUID, Integer> getAllTotals() {
        Map<UUID, Integer> totals = new LinkedHashMap<>();
        for (var entry : playerContributions.entrySet()) {
            UUID playerId = UUID.fromString(entry.getKey());
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            totals.put(playerId, total);
        }
        return totals;
    }

    public UUID getCurrentFirstPlace() {
        return currentFirstPlace;
    }

    public void setCurrentFirstPlace(UUID playerId) {
        this.currentFirstPlace = playerId;
        setDirty();
    }

    public void resetAll() {
        globalProgress.clear();
        playerContributions.clear();
        currentFirstPlace = null;
        setDirty();
    }
}