package com.yousam.collection.progress;

import net.minecraft.server.level.ServerLevel;

public class CollectionProgressManager {
    public static CollectionProgressState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CollectionProgressState.TYPE);
    }

    public static PlayerNameCache getNameCache(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PlayerNameCache.TYPE);
    }

    public static NicknameStorage getNicknameStorage(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(NicknameStorage.TYPE);
    }
}