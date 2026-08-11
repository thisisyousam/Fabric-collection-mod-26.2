package com.yousam.collection.client;

import net.fabricmc.api.ClientModInitializer;

public class CollectionModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworkHandler.register();
        ModKeyBindings.register();
    }
}