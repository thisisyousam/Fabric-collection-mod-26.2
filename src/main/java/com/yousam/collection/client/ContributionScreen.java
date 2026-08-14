package com.yousam.collection.client;

import com.yousam.collection.CollectionMod;
import com.yousam.collection.network.LeaderboardSyncPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ContributionScreen extends Screen {

    private static final int PANEL_PADDING = 65;
    private static final int ROW_HEIGHT = 9;
    private static final int HEADER_HEIGHT = 50;
    private static final int MAX_RANKS = 10;

    private static final int BACK_BUTTON_SIZE = 16;
    private static final int BACK_BUTTON_MARGIN = 8;

    private static final float ROW_TEXT_SCALE = 0.7f;

    private static final Identifier PANEL_TEXTURE =
            Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "textures/gui/contribution_panel.png");

    private final Screen parent;
    private int panelX, panelY, panelWidth, panelHeight;

    public ContributionScreen(Screen parent) {
        super(Component.literal("도감 기여도"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        panelWidth = 220;
        panelHeight = 200;
        panelX = this.width / 2 - panelWidth / 2;
        panelY = this.height / 2 - panelHeight / 2;

        this.addRenderableWidget(new BackButton(
                panelX + BACK_BUTTON_MARGIN + 185, panelY + BACK_BUTTON_MARGIN + 175,
                BACK_BUTTON_SIZE, BACK_BUTTON_SIZE,
                Component.literal("뒤로가기"),
                button -> this.minecraft.gui.setScreen(parent)
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(PANEL_TEXTURE,
                panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                0f, 1f, 0f, 1f);

        List<LeaderboardSyncPayload.PlayerContribution> entries = CollectionScreen.ClientLeaderboardCache.get();

        int y = panelY + HEADER_HEIGHT;
        int rank = 1;
        for (LeaderboardSyncPayload.PlayerContribution entry : entries) {
            if (rank > MAX_RANKS) {
                break;
            }
            renderRow(graphics, rank, entry, panelX + PANEL_PADDING - 5, y + 7, panelWidth - PANEL_PADDING * 2);
            y += ROW_HEIGHT;
            rank++;
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void renderRow(GuiGraphicsExtractor graphics, int rank, LeaderboardSyncPayload.PlayerContribution entry, int x, int y, int width) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(ROW_TEXT_SCALE, ROW_TEXT_SCALE);

        Style boldFontStyle = Style.EMPTY.withFont(
                new FontDescription.Resource(Identifier.withDefaultNamespace("wantedsans_bold"))
        );

        String rankText = rank + "위";

        Component styledText = Component.literal(String.valueOf(rankText))
                .setStyle(boldFontStyle);
        graphics.text(this.font, styledText, 0, 0, 0xFF000000, false);

        String nameText = entry.playerName();
        graphics.text(this.font, nameText, 55, 0, 0xFF000000, false);

        String countText = entry.total() + "개";
        int countWidth = this.font.width(countText);
        graphics.text(this.font, countText, width - countWidth + 35, 0, 0xFF000000, false);

        graphics.pose().popMatrix();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}