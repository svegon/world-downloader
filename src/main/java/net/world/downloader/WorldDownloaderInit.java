package net.world.downloader;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

public final class WorldDownloaderInit implements ModInitializer, ClientModInitializer {
	@Override
	public void onInitializeClient() {
		WDL.bootstrap(MinecraftClient.getInstance());
	}

	@Override
	public void onInitialize() {
	}
}
