package com.yousam.collection.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yousam.collection.CollectionMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class ModKeyBindings {
    private static KeyMapping openScreenKey;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CollectionMod.MOD_ID, "collection"));

    public static void register() {
        openScreenKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.collection.open_screen",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openScreenKey.consumeClick()) {
                if (client.player != null) {
                    client.gui.setScreen(new CollectionScreen());
                }
            }
        });
    }
}