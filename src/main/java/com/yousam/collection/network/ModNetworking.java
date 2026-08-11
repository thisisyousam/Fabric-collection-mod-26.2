package com.yousam.collection.network;

import com.yousam.collection.CollectionMod;
import com.yousam.collection.data.CollectionEntry;
import com.yousam.collection.data.CollectionEntryLoader;
import com.yousam.collection.progress.CollectionProgressManager;
import com.yousam.collection.progress.CollectionProgressState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.component.DataComponents;


import java.util.HashMap;
import java.util.Map;

public class ModNetworking {

    public static void register() {
        // 클라이언트 -> 서버
        PayloadTypeRegistry.serverboundPlay().register(SubmitItemPayload.TYPE, SubmitItemPayload.STREAM_CODEC);
        // 서버 -> 클라이언트
        PayloadTypeRegistry.clientboundPlay().register(EntrySyncPayload.TYPE, EntrySyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ProgressSyncPayload.TYPE, ProgressSyncPayload.STREAM_CODEC);

        PayloadTypeRegistry.serverboundPlay().register(RequestLeaderboardPayload.TYPE, RequestLeaderboardPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LeaderboardSyncPayload.TYPE, LeaderboardSyncPayload.STREAM_CODEC);

        PayloadTypeRegistry.clientboundPlay().register(NicknameSyncPayload.TYPE, NicknameSyncPayload.STREAM_CODEC);


        ServerPlayNetworking.registerGlobalReceiver(RequestLeaderboardPayload.TYPE, (payload, context) -> {
            handleLeaderboardRequest(context.player());
        });

        ServerPlayNetworking.registerGlobalReceiver(SubmitItemPayload.TYPE, (payload, context) -> {
            handleSubmit(context.player(), payload.entryId());
        });

        // 플레이어 접속 시 항목 목록 + 진행도 동기화, 이름 기록
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            CollectionProgressManager.getNameCache(player.level())
                    .record(player.getUUID(), player.getName().getString());
            sendEntrySync(player);
            sendProgressSync(player);

            ServerPlayNetworking.send(player, new NicknameSyncPayload(
                    CollectionProgressManager.getNicknameStorage(player.level()).getAll()
            ));
        });
    }

    private static void sendEntrySync(ServerPlayer player) {
        ServerPlayNetworking.send(player, EntrySyncPayload.fromEntries(CollectionEntryLoader.getAll()));
    }

    public static void sendProgressSync(ServerPlayer player) {
        CollectionProgressState progress = CollectionProgressManager.get(player.level());
        Map<Identifier, Integer> submitted = new HashMap<>();
        for (Identifier entryId : CollectionEntryLoader.getAll().keySet()) {
            int count = progress.getSubmitted(entryId);
            if (count > 0) {
                submitted.put(entryId, count);
            }
        }
        ServerPlayNetworking.send(player, new ProgressSyncPayload(submitted));
    }

    public static void sendProgressSyncPublic(ServerPlayer player) {
        sendProgressSync(player);
    }

    private static void broadcastProgressSync(net.minecraft.server.level.ServerLevel level) {
        for (ServerPlayer p : net.fabricmc.fabric.api.networking.v1.PlayerLookup.level(level)) {
            sendProgressSync(p);
        }
    }

    private static void handleSubmit(ServerPlayer player, Identifier entryId) {
        CollectionEntry entry = CollectionEntryLoader.getAll().get(entryId);
        if (entry == null) {
            CollectionMod.LOGGER.warn("존재하지 않는 도감 항목 제출 시도: {}", entryId);
            return;
        }

        CollectionProgressState progress = CollectionProgressManager.get(player.level());
        int alreadySubmitted = progress.getSubmitted(entryId);

        if (alreadySubmitted >= entry.requiredCount()) {
            // 이미 전체 완료된 항목 - 아무도 더 제출 못 함
            return;
        }

        Item targetItem = BuiltInRegistries.ITEM.get(entry.itemId())
                .map(Holder.Reference::value)
                .orElse(null);

        if (targetItem == null) {
            CollectionMod.LOGGER.warn("등록되지 않은 아이템 ID: {}", entry.itemId());
            return;
        }

        int slot = findSlotWithItem(player, entry, targetItem);
        if (slot < 0) {
            CollectionMod.LOGGER.info("{}님 인벤토리에 {} 없음", player.getName().getString(), entry.itemId());
            return;
        }

        ItemStack stack = player.getInventory().getItem(slot);
        stack.shrink(1);

        progress.addSubmitted(player.getUUID(), entryId, 1);

        CollectionMod.LOGGER.info(
                "{}님이 {} 제출 ({}/{})",
                player.getName().getString(),
                entryId,
                alreadySubmitted + 1,
                entry.requiredCount()
        );

        // 공유 상태이므로 이 항목을 볼 수 있는 모든 플레이어에게 갱신된 진행도 전송
        broadcastProgressSync(player.level());
    }

    private static int findSlotWithItem(ServerPlayer player, CollectionEntry entry, Item baseItem) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || stack.getItem() != baseItem) continue;
            if (matchesComponent(stack, entry)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matchesComponent(ItemStack stack, CollectionEntry entry) {
        if (entry.componentKey() == null) {
            return true;
        }
        String key = entry.componentKey();
        String value = entry.componentValue();
        Identifier expected = value.contains(":") ? Identifier.parse(value) : Identifier.withDefaultNamespace(value);

        if (key.equals("instrument")) {
            var instrumentComponent = stack.get(DataComponents.INSTRUMENT);
            if (instrumentComponent == null) return false;

            var actualKey = instrumentComponent.instrument().unwrapKey();
            return actualKey.map(k -> k.identifier().equals(expected)).orElse(false);
        }

        if (key.equals("minecraft:potion_contents") || key.equals("potion_contents")) {
            var potionContents = stack.get(DataComponents.POTION_CONTENTS);
            if (potionContents == null) return false;

            var actualKey = potionContents.potion().flatMap(net.minecraft.core.Holder::unwrapKey);
            return actualKey.map(k -> k.identifier().equals(expected)).orElse(false);
        }

        return false;
    }

    private static void handleLeaderboardRequest(ServerPlayer player) {
        CollectionProgressState progress = CollectionProgressManager.get(player.level());
        java.util.Map<java.util.UUID, Integer> totals = progress.getAllTotals();

        var contributions = totals.entrySet().stream()
                .map(e -> new LeaderboardSyncPayload.PlayerContribution(resolvePlayerName(player, e.getKey()), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.total(), a.total()))
                .toList();

        ServerPlayNetworking.send(player, new LeaderboardSyncPayload(contributions));
    }

    private static String resolvePlayerName(ServerPlayer requester, java.util.UUID uuid) {
        var onlinePlayer = requester.level().getServer().getPlayerList().getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName().getString();
        }

        String cachedName = CollectionProgressManager.getNameCache(requester.level()).get(uuid);
        if (cachedName != null) {
            return cachedName;
        }

        return uuid.toString().substring(0, 8);
    }
}