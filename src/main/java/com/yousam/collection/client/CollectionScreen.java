package com.yousam.collection.client;

import com.yousam.collection.CollectionMod;
import com.yousam.collection.data.CollectionCategory;
import com.yousam.collection.network.EntrySyncPayload;
import com.yousam.collection.network.LeaderboardSyncPayload;
import com.yousam.collection.network.RequestLeaderboardPayload;
import com.yousam.collection.network.SubmitItemPayload;
import com.yousam.collection.network.UsePassTicketPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CollectionScreen extends Screen {

    // 도감 창 전체 크기 배율 (원본 배경 이미지 기준). 줄이면 창이 작아짐.
    private static final float SCALE = 0.7f;

    private static final int PAGE_WIDTH = 410;
    private static final int PAGE_HEIGHT = 256;

    private static final int CELL_SIZE = 30;
    private static final int GRID_ROWS = 5;
    private static final int GRID_COLUMNS_PER_PAGE = 5;
    private static final int LEFT_PAGE_X = 36;
    private static final int RIGHT_PAGE_X = 216;
    private static final int GRID_TOP_Y = 50;

    private static final float ITEM_ICON_SCALE = 1.2f;
    private static final float COUNT_TEXT_SCALE = 1f;
    private static final float TAB_LABEL_SCALE = 0.7f;
    // 탭 텍스트 위치 미세조정 (왼쪽/오른쪽 탭 각각 따로 조절 가능)
    private static final float LEFT_TAB_LABEL_OFFSET_X = 0;
    private static final float LEFT_TAB_LABEL_OFFSET_Y = 0;
    private static final float RIGHT_TAB_LABEL_OFFSET_X = 0.5f;
    private static final float RIGHT_TAB_LABEL_OFFSET_Y = 0;

    // shop 모드의 도감 패스권 아이템 ID. 두 모드는 gradle 의존성이 없으므로
    // 클래스가 아닌 레지스트리 ID 문자열로만 느슨하게 연결한다.
    private static final Identifier PASS_TICKET_ITEM_ID =
            Identifier.fromNamespaceAndPath("shop", "collection_pass_ticket");

    private static final Identifier COMPLETE_MARK_TEXTURE =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "textures/gui/complete_mark.png");
    private static final Identifier BACKGROUND_TEXTURE =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "textures/gui/collection_background.png");
    // 탭이 선택됐을 때 덧그리는 활성 인덱스 이미지 (index1~index11.png, 탭 배치 순서와 동일)
    private static final Identifier[] ACTIVE_TAB_TEXTURES = new Identifier[11];
    static {
        for (int i = 0; i < ACTIVE_TAB_TEXTURES.length; i++) {
            ACTIVE_TAB_TEXTURES[i] = Identifier.fromNamespaceAndPath(
                    CollectionMod.MOD_ID, "textures/gui/index" + (i + 1) + ".png");
        }
    }

    // 배경 이미지의 색깔 인덱스 탭 좌표 (원본 이미지 기준 상대좌표)
    private static final int TAB_WIDTH = 34;
    private static final int TAB_HEIGHT = 25;
    private static final int LEFT_TAB_X = 1;
    private static final int RIGHT_TAB_X = 367;
    private static final int[] LEFT_TAB_Y = {34, 65, 96, 127, 158, 189};
    private static final int[] RIGHT_TAB_Y = {34, 65, 96, 127, 158};
    private static final CollectionCategory[] LEFT_TAB_CATEGORIES = {
        CollectionCategory.OVERWORLD, CollectionCategory.TREE, CollectionCategory.PLANT,
            CollectionCategory.ANIMAL, CollectionCategory.MONSTER, CollectionCategory.MINERALS
    };
    private static final CollectionCategory[] RIGHT_TAB_CATEGORIES = {
        CollectionCategory.RARE, CollectionCategory.DISK, CollectionCategory.NETHER_ENDER,
            CollectionCategory.POTION, CollectionCategory.OCEAN
    };

    // 기여도 버튼 자리 (배경 이미지 좌상단 노란 태그 부분, 원본 이미지 기준)
    private static final int CONTRIBUTION_BUTTON_X = 73;
    private static final int CONTRIBUTION_BUTTON_Y = 6;
    private static final int CONTRIBUTION_BUTTON_WIDTH = 48;
    private static final int CONTRIBUTION_BUTTON_HEIGHT = 18;

    private CollectionCategory selectedCategory = CollectionCategory.ANIMAL;

    // panelX/panelY = 화면상 축소된 책의 좌상단 좌표
    private int panelX, panelY;

    public CollectionScreen() {
        super(Component.translatable("screen.collection.title"));
    }

    @Override
    protected void init() {
        super.init();

        int panelWidth = Math.round(PAGE_WIDTH * SCALE);
        int panelHeight = Math.round(PAGE_HEIGHT * SCALE);
        panelX = this.width / 2 - panelWidth / 2;
        panelY = this.height / 2 - panelHeight / 2;

        rebuildScreenWidgets();
    }

    private void rebuildScreenWidgets() {
        this.addRenderableWidget(new ContributionButton(
                panelX + Math.round(CONTRIBUTION_BUTTON_X * SCALE), panelY + Math.round(CONTRIBUTION_BUTTON_Y * SCALE),
                Math.round(CONTRIBUTION_BUTTON_WIDTH * SCALE), Math.round(CONTRIBUTION_BUTTON_HEIGHT * SCALE),
                Component.literal("기여도"),
                button -> {
                    ClientPlayNetworking.send(new RequestLeaderboardPayload());
                    this.minecraft.gui.setScreen(new ContributionScreen(this));
                }
        ));
    }

    private String categoryLabel(CollectionCategory category) {
        return switch (category) {
            case ANIMAL -> "동물";
            case DISK -> "음반";
            case MINERALS -> "광물";
            case MONSTER -> "몬스터";
            case NETHER_ENDER -> "네더&엔더";
            case POTION -> "포션";
            case OCEAN -> "바다";
            case PLANT -> "식물&음식";
            case TREE -> "나무";
            case OVERWORLD -> "오버월드";
            case RARE -> "희귀";

        };
    }

    private List<EntrySyncPayload.EntryData> filteredEntries() {
        return ClientCollectionCache.getEntries().stream()
                .filter(entry -> entry.category() == selectedCategory)
                .toList();
    }

    // 아래 좌표들은 모두 축소 전(원본 배경 이미지) 기준 "디자인 좌표"다.
    // 실제 화면에는 extractRenderState에서 panelX/panelY로 이동 후 SCALE만큼 축소해서 그린다.
    private int slotX(int index) {
        int col = index % GRID_COLUMNS_PER_PAGE;
        int baseX = index < GRID_ROWS * GRID_COLUMNS_PER_PAGE ? LEFT_PAGE_X : RIGHT_PAGE_X;
        return baseX + col * CELL_SIZE;
    }

    private int slotY(int index) {
        int localIndex = index % (GRID_ROWS * GRID_COLUMNS_PER_PAGE);
        int row = localIndex / GRID_COLUMNS_PER_PAGE;
        return GRID_TOP_Y + row * CELL_SIZE;
    }

    private int toDesignX(double screenX) {
        return (int) Math.floor((screenX - panelX) / SCALE);
    }

    private int toDesignY(double screenY) {
        return (int) Math.floor((screenY - panelY) / SCALE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int designMouseX = toDesignX(mouseX);
        int designMouseY = toDesignY(mouseY);

        graphics.pose().pushMatrix();
        graphics.pose().translate(panelX, panelY);
        graphics.pose().scale(SCALE, SCALE);

        graphics.blit(BACKGROUND_TEXTURE,
                0, 0, PAGE_WIDTH, PAGE_HEIGHT,
                0f, 1f, 0f, 1f);

        List<EntrySyncPayload.EntryData> entries = filteredEntries();

        for (int i = 0; i < entries.size() && i < GRID_ROWS * GRID_COLUMNS_PER_PAGE * 2; i++) {
            EntrySyncPayload.EntryData entry = entries.get(i);
            int x = slotX(i);
            int y = slotY(i);

            renderSlot(graphics, entry, x, y, designMouseX, designMouseY, mouseX, mouseY);
        }

        renderTabs(graphics);

        graphics.pose().popMatrix();

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void renderTabs(GuiGraphicsExtractor graphics) {
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            renderTab(graphics, LEFT_TAB_X, LEFT_TAB_Y[i], LEFT_TAB_CATEGORIES[i], i,
                    LEFT_TAB_LABEL_OFFSET_X, LEFT_TAB_LABEL_OFFSET_Y);
        }
        for (int i = 0; i < RIGHT_TAB_Y.length; i++) {
            renderTab(graphics, RIGHT_TAB_X, RIGHT_TAB_Y[i], RIGHT_TAB_CATEGORIES[i], LEFT_TAB_Y.length + i,
                    RIGHT_TAB_LABEL_OFFSET_X, RIGHT_TAB_LABEL_OFFSET_Y);
        }
    }

    private void renderTab(GuiGraphicsExtractor graphics, float x, float y, CollectionCategory category, int activeTextureIndex,
                            float labelOffsetX, float labelOffsetY) {
        if (category == selectedCategory) {
            graphics.blit(ACTIVE_TAB_TEXTURES[activeTextureIndex],
                    0, 0, PAGE_WIDTH, PAGE_HEIGHT,
                    0f, 1f, 0f, 1f);
        }

        float centerX = x + TAB_WIDTH / 2;

        Style boldFontStyle = Style.EMPTY.withFont(
                new FontDescription.Resource(Identifier.withDefaultNamespace("wantedsans_bold"))
        );
        Component label = Component.literal(categoryLabel(category)).setStyle(boldFontStyle);
        int textWidth = this.font.width(label);

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX + labelOffsetX, y + TAB_HEIGHT / 2 + 5 + labelOffsetY);
        graphics.pose().scale(TAB_LABEL_SCALE, TAB_LABEL_SCALE);
        graphics.text(this.font, label, -textWidth / 2 + 1, 0, 0xFF000000, false);
        graphics.pose().popMatrix();
    }

    private CollectionCategory tabAt(int designMouseX, int designMouseY) {
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            if (designMouseX >= LEFT_TAB_X && designMouseX < LEFT_TAB_X + TAB_WIDTH
                    && designMouseY >= LEFT_TAB_Y[i] && designMouseY < LEFT_TAB_Y[i] + TAB_HEIGHT) {
                return LEFT_TAB_CATEGORIES[i];
            }
        }
        for (int i = 0; i < RIGHT_TAB_Y.length; i++) {
            if (designMouseX >= RIGHT_TAB_X && designMouseX < RIGHT_TAB_X + TAB_WIDTH
                    && designMouseY >= RIGHT_TAB_Y[i] && designMouseY < RIGHT_TAB_Y[i] + TAB_HEIGHT) {
                return RIGHT_TAB_CATEGORIES[i];
            }
        }
        return null;
    }


    private void renderSlot(GuiGraphicsExtractor graphics, EntrySyncPayload.EntryData entry, int x, int y,
                             int designMouseX, int designMouseY, int screenMouseX, int screenMouseY) {
        boolean complete = ClientCollectionCache.isComplete(entry);

        Item item = BuiltInRegistries.ITEM.get(entry.itemId())
                .map(Holder.Reference::value)
                .orElse(null);

        if (item == null) {
            return;
        }

        ItemStack stack = buildDisplayStack(entry, item);

        int iconCenterX = x + CELL_SIZE / 2;
        int iconCenterY = y + CELL_SIZE / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconCenterX, iconCenterY);
        graphics.pose().scale(ITEM_ICON_SCALE, ITEM_ICON_SCALE);
        graphics.item(stack, -8, -8);
        graphics.pose().popMatrix();

        if (complete) {
            graphics.blit(COMPLETE_MARK_TEXTURE, x + 5, y + 5, x + CELL_SIZE - 2, y + CELL_SIZE - 2, 0f, 1f, 0f, 1f);
        } else {
            int submitted = ClientCollectionCache.getSubmitted(entry.id());


            Style boldFontStyle = Style.EMPTY.withFont(
                    new FontDescription.Resource(Identifier.withDefaultNamespace("wantedsans_bold"))
            );
            Component styledText = Component.literal(String.valueOf(entry.required() - submitted))
                    .setStyle(boldFontStyle);

            int textWidth = this.font.width(styledText);

            int textCenterX = x + CELL_SIZE / 2;
            int textY = y + CELL_SIZE - 10;
            graphics.pose().pushMatrix();
            graphics.pose().translate(textCenterX, textY);
            graphics.pose().scale(COUNT_TEXT_SCALE, COUNT_TEXT_SCALE);
            graphics.text(this.font, styledText, 6, -2, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }

        // 마우스가 이 슬롯 위에 있으면 아이템 이름 툴팁 표시 (툴팁은 화면 좌표 기준)
        if (designMouseX >= x && designMouseX < x + CELL_SIZE && designMouseY >= y && designMouseY < y + CELL_SIZE) {
            graphics.setTooltipForNextFrame(this.font, stack, screenMouseX, screenMouseY);
        }
    }

    private ItemStack buildDisplayStack(EntrySyncPayload.EntryData entry, Item item) {
        ItemStack stack = new ItemStack(item);

        if (entry.componentKey() == null) {
            return stack;
        }

        Identifier valueId = entry.componentValue().contains(":")
                ? Identifier.parse(entry.componentValue())
                : Identifier.withDefaultNamespace(entry.componentValue());

        if (entry.componentKey().equals("minecraft:potion_contents") || entry.componentKey().equals("potion_contents")) {
            var registryAccess = net.minecraft.client.Minecraft.getInstance().level.registryAccess();
            var potionRegistry = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.POTION);
            potionRegistry.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.POTION, valueId))
                    .ifPresent(holder -> {
                        var contents = new net.minecraft.world.item.alchemy.PotionContents(holder);
                        stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, contents);
                    });
        }

        if (entry.componentKey().equals("instrument")) {
            var registryAccess = net.minecraft.client.Minecraft.getInstance().level.registryAccess();
            var instrumentRegistry = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.INSTRUMENT);
            instrumentRegistry.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.INSTRUMENT, valueId))
                    .ifPresent(holder -> {
                        stack.set(net.minecraft.core.component.DataComponents.INSTRUMENT, new net.minecraft.world.item.component.InstrumentComponent(holder));
                    });
        }

        return stack;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        int designMouseX = toDesignX(event.x());
        int designMouseY = toDesignY(event.y());

        CollectionCategory clickedTab = tabAt(designMouseX, designMouseY);
        if (clickedTab != null) {
            selectedCategory = clickedTab;
            return true;
        }

        List<EntrySyncPayload.EntryData> entries = filteredEntries();

        for (int i = 0; i < entries.size() && i < GRID_ROWS * GRID_COLUMNS_PER_PAGE * 2; i++) {
            EntrySyncPayload.EntryData entry = entries.get(i);
            int x = slotX(i);
            int y = slotY(i);

            if (designMouseX >= x && designMouseX < x + CELL_SIZE && designMouseY >= y && designMouseY < y + CELL_SIZE) {
                if (!ClientCollectionCache.isComplete(entry)) {
                    if (isHoldingPassTicket()) {
                        ClientPlayNetworking.send(new UsePassTicketPayload(entry.id()));
                    } else {
                        ClientPlayNetworking.send(new SubmitItemPayload(entry.id()));
                    }
                }
                return true;
            }
        }

        return false;
    }

    private boolean isHoldingPassTicket() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return matchesPassTicket(player.getMainHandItem()) || matchesPassTicket(player.getOffhandItem());
    }

    private boolean matchesPassTicket(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return PASS_TICKET_ITEM_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static class ClientLeaderboardCache {
        private static List<LeaderboardSyncPayload.PlayerContribution> entries = List.of();

        public static void set(List<LeaderboardSyncPayload.PlayerContribution> newEntries) {
            entries = newEntries;
        }

        public static List<LeaderboardSyncPayload.PlayerContribution> get() {
            return entries;
        }
    }
}
