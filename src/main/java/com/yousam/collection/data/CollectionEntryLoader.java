package com.yousam.collection.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yousam.collection.CollectionMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class CollectionEntryLoader implements SimpleSynchronousResourceReloadListener {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "collection_entries");
    private static final Map<Identifier, CollectionEntry> ENTRIES = new LinkedHashMap<>();
    private static final String FOLDER = "collection_entries";

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Identifier, CollectionEntry> loaded = new LinkedHashMap<>();

        Map<Identifier, Resource> resources = manager.listResources(
                FOLDER,
                path -> path.getPath().endsWith(".json")
        );

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();
            try (Reader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8)) {

                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (!json.has("required")) {
                    throw new IllegalArgumentException("'required' 필드가 없습니다: " + fileId);
                }
                if (!json.has("order")) {
                    throw new IllegalArgumentException("'order' 필드가 없습니다: " + fileId);
                }

                String rawItem = json.get("item").getAsString();
                String baseItemStr = rawItem;
                String componentKey = null;
                String componentValue = null;

                int bracketIndex = rawItem.indexOf('[');
                if (bracketIndex >= 0 && rawItem.endsWith("]")) {
                    baseItemStr = rawItem.substring(0, bracketIndex);
                    String inner = rawItem.substring(bracketIndex + 1, rawItem.length() - 1);
                    int eqIndex = inner.indexOf('=');
                    if (eqIndex >= 0) {
                        componentKey = inner.substring(0, eqIndex);
                        componentValue = inner.substring(eqIndex + 1);
                    }
                }

                Identifier itemId = Identifier.parse(baseItemStr);
                int required = json.get("required").getAsInt();
                int order = json.get("order").getAsInt();
                CollectionCategory category = extractCategoryFromPath(fileId);

                Identifier collectionId = trimToId(fileId);

                loaded.put(collectionId, new CollectionEntry(
                        collectionId, itemId, required, category, order, componentKey, componentValue
                ));

            } catch (Exception e) {
                CollectionMod.LOGGER.error("수집 항목 로드 실패: {}", fileId, e);
            }
        }

        // order 기준으로 정렬한 새 맵으로 교체
        ENTRIES.clear();
        loaded.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getValue().order()))
                .forEach(e -> ENTRIES.put(e.getKey(), e.getValue()));

        CollectionMod.LOGGER.info("수집 항목 {}개 로드 완료", ENTRIES.size());
    }

    private static CollectionCategory extractCategoryFromPath(Identifier fileId) {
        String[] parts = fileId.getPath().split("/");
        return CollectionCategory.valueOf(parts[1].toUpperCase());
    }

    private static Identifier trimToId(Identifier fileId) {
        String[] parts = fileId.getPath().split("/");
        String filename = parts[parts.length - 1];
        String name = filename.substring(0, filename.length() - ".json".length());
        return Identifier.fromNamespaceAndPath(fileId.getNamespace(), name);
    }

    public static Map<Identifier, CollectionEntry> getAll() {
        return ENTRIES;
    }
}