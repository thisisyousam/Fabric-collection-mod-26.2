package com.yousam.collection;

import com.yousam.collection.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yousam.collection.data.CollectionEntryLoader;

public class CollectionMod implements ModInitializer {
	public static final String MOD_ID = "collection-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Collection 모드 초기화 시작");

		ResourceManagerHelper.get(PackType.SERVER_DATA)
				.registerReloadListener(new CollectionEntryLoader());

		ModNetworking.register();

		com.yousam.collection.command.ModCommands.register();

		LOGGER.info("Collection 모드 초기화 완료");
	}
}