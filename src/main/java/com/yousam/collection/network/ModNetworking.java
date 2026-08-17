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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.core.component.DataComponents;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModNetworking {

    // shop 모드에서 등록하는 도감 패스권 아이템 ID. 두 모드는 gradle 의존성이 없으므로
    // 클래스가 아닌 레지스트리 ID 문자열로만 느슨하게 연결한다.
    private static final Identifier PASS_TICKET_ITEM_ID = Identifier.fromNamespaceAndPath("shop", "collection_pass_ticket");

    public static void register() {
        // 클라이언트 -> 서버
        PayloadTypeRegistry.serverboundPlay().register(SubmitItemPayload.TYPE, SubmitItemPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UsePassTicketPayload.TYPE, UsePassTicketPayload.STREAM_CODEC);
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

        ServerPlayNetworking.registerGlobalReceiver(UsePassTicketPayload.TYPE, (payload, context) -> {
            handleUsePassTicket(context.player(), payload.entryId());
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

        checkFirstPlaceChange(player, progress);

        // 공유 상태이므로 이 항목을 볼 수 있는 모든 플레이어에게 갱신된 진행도 전송
        broadcastProgressSync(player.level());
    }

    private static void handleUsePassTicket(ServerPlayer player, Identifier entryId) {
        CollectionEntry entry = CollectionEntryLoader.getAll().get(entryId);
        if (entry == null) {
            CollectionMod.LOGGER.warn("존재하지 않는 도감 항목에 패스권 사용 시도: {}", entryId);
            return;
        }

        CollectionProgressState progress = CollectionProgressManager.get(player.level());
        int alreadySubmitted = progress.getSubmitted(entryId);
        if (alreadySubmitted >= entry.requiredCount()) {
            // 이미 완료된 항목 - 패스권을 소모하지 않는다.
            return;
        }

        InteractionHand hand = findHandWithPassTicket(player);
        if (hand == null) {
            CollectionMod.LOGGER.info("{}님이 도감 패스권 없이 사용 시도", player.getName().getString());
            return;
        }

        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }

        int remaining = entry.requiredCount() - alreadySubmitted;
        progress.addSubmitted(player.getUUID(), entryId, remaining);

        player.sendSystemMessage(Component.literal("§a도감 패스권을 사용하여 항목을 즉시 완료했습니다"));
        CollectionMod.LOGGER.info(
                "{}님이 도감 패스권으로 {} 항목을 즉시 완료 ({}개 처리)",
                player.getName().getString(), entryId, remaining
        );

        checkFirstPlaceChange(player, progress);

        broadcastProgressSync(player.level());
    }

    private static InteractionHand findHandWithPassTicket(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) continue;
            if (PASS_TICKET_ITEM_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return hand;
            }
        }
        return null;
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

    // 기여도 갱신 시마다 호출되어 1위가 실제로 바뀌었는지 확인하고, 바뀌었다면 서버 전체에 알린다.
    // 동점으로 인한 순위 변동(기존 1위와 동점이 된 경우)은 트리거하지 않고, 순수하게 추월한 경우에만 트리거한다.
    private static void checkFirstPlaceChange(ServerPlayer contributor, CollectionProgressState progress) {
        java.util.Map<java.util.UUID, Integer> totals = progress.getAllTotals();
        if (totals.isEmpty()) {
            return;
        }

        java.util.UUID newLeader = null;
        int newLeaderTotal = Integer.MIN_VALUE;
        boolean tie = false;
        for (var entry : totals.entrySet()) {
            int total = entry.getValue();
            if (total > newLeaderTotal) {
                newLeaderTotal = total;
                newLeader = entry.getKey();
                tie = false;
            } else if (total == newLeaderTotal) {
                tie = true;
            }
        }

        java.util.UUID previousFirstPlace = progress.getCurrentFirstPlace();

        if (previousFirstPlace == null) {
            // 서버 최초 기동(또는 이 필드가 없던 기존 세이브 최초 로드) - 스팸 방지를 위해 조용히 값만 세팅
            progress.setCurrentFirstPlace(newLeader);
            return;
        }

        if (tie || newLeader.equals(previousFirstPlace)) {
            // 동점 상태이거나 1위가 그대로면 알릴 것이 없음
            return;
        }

        String newLeaderName = resolveNickname(contributor, newLeader);

        Component message = Component.literal(
                "§e👑 " + newLeaderName + "§f님이 도감 순위 1위를 차지했습니다!"
        );
        for (ServerPlayer p : net.fabricmc.fabric.api.networking.v1.PlayerLookup.all(contributor.level().getServer())) {
            p.sendSystemMessage(message);
            playFirstPlaceNotifySound(p);
        }

        ServerPlayer newLeaderPlayer = contributor.level().getServer().getPlayerList().getPlayer(newLeader);
        if (newLeaderPlayer != null) {
            spawnOvertakeFirework(newLeaderPlayer);
        }

        progress.setCurrentFirstPlace(newLeader);
    }

    // 모든 접속자에게 위치와 무관하게 알림 효과음을 들려준다 (거리 기반 Level#playSound는 근처 플레이어만 들을 수 있어 부적합)
    private static void playFirstPlaceNotifySound(ServerPlayer player) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST),
                SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(),
                1.0f, 1.0f,
                player.level().getRandom().nextLong()
        ));
    }

    // 역전에 성공한 플레이어 머리 위에서 폭죽을 즉시 터뜨려 축하 효과를 준다
    private static void spawnOvertakeFirework(ServerPlayer player) {
        ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
        fireworkStack.set(DataComponents.FIREWORKS, new Fireworks(
                0,
                List.of(new FireworkExplosion(
                        FireworkExplosion.Shape.LARGE_BALL,
                        IntList.of(0xFFD700, 0xFF5555),
                        IntList.of(),
                        true,
                        true
                ))
        ));

        FireworkRocketEntity firework = new FireworkRocketEntity(
                player.level(), player.getX(), player.getY() + 1.0, player.getZ(), fireworkStack
        );
        player.level().addFreshEntity(firework);
    }

    private static String resolveNickname(ServerPlayer requester, java.util.UUID uuid) {
        String nickname = CollectionProgressManager.getNicknameStorage(requester.level()).get(uuid);
        if (nickname != null) {
            return nickname;
        }
        return resolvePlayerName(requester, uuid);
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