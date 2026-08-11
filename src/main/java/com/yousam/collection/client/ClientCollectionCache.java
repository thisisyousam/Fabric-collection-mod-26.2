package com.yousam.collection.client;

import com.yousam.collection.network.EntrySyncPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientCollectionCache {
    private static List<EntrySyncPayload.EntryData> entries = List.of();
    private static Map<Identifier, Integer> submitted = new HashMap<>();

    public static void setEntries(List<EntrySyncPayload.EntryData> newEntries) {
        entries = newEntries;
    }

    public static void setSubmitted(Map<Identifier, Integer> newSubmitted) {
        submitted = newSubmitted;
    }

    public static List<EntrySyncPayload.EntryData> getEntries() {
        return entries;
    }

    public static int getSubmitted(Identifier entryId) {
        return submitted.getOrDefault(entryId, 0);
    }

    public static boolean isComplete(EntrySyncPayload.EntryData entry) {
        return getSubmitted(entry.id()) >= entry.required();
    }
}