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
    private static final int ROW_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 50;
    private static final int MAX_RANKS = 10;

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
            renderRow(graphics, rank, entry, panelX + PANEL_PADDING, y, panelWidth - PANEL_PADDING * 2);
            y += ROW_HEIGHT;
            rank++;
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void renderRow(GuiGraphicsExtractor graphics, int rank, LeaderboardSyncPayload.PlayerContribution entry, int x, int y, int width) {
        String rankText = rank + "위";
        graphics.text(this.font, rankText, x, y, 0xFF000000, false);

        String nameText = entry.playerName();
        graphics.text(this.font, nameText, x + 30, y, 0xFF000000, false);

        String countText = entry.total() + "개";
        int countWidth = this.font.width(countText);
        graphics.text(this.font, countText, x + width - countWidth, y, 0xFF000000, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}