package com.yousam.collection.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientNicknameCache {
    private static Map<UUID, String> nicknames = new HashMap<>();

    public static void set(Map<UUID, String> newNicknames) {
        nicknames = newNicknames;
    }

    public static String get(UUID uuid) {
        return nicknames.get(uuid);
    }
}