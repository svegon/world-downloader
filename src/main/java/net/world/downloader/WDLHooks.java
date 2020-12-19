/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader;

import java.util.Collection;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
/**
 * The various hooks for wdl. <br/>
 * All of these should be called regardless of any WDL state variables.
 * This class forwards the hooks to the appropriate locations.
 */
public final class WDLHooks {
	private WDLHooks() {
		throw new AssertionError();
	}

	/**
	 * Listener which should receive event calls.
	 */
	@Nonnull
	public static IHooksListener listener = new BootstrapHooksListener();

	public static interface IHooksListener {
		public void onWorldClientTick(ClientWorld sender);
		public void onWorldClientRemoveEntityFromWorld(ClientWorld sender, int eid);
		public void onNHPCHandleChunkUnload(ClientPlayNetworkHandler sender, ClientWorld world,
				UnloadChunkS2CPacket packet);
		public void onNHPCHandleChat(ClientPlayNetworkHandler sender, GameMessageS2CPacket packet);
		public void onNHPCHandleMaps(ClientPlayNetworkHandler sender, MapUpdateS2CPacket packet);
		public void onNHPCHandleCustomPayload(ClientPlayNetworkHandler sender,
				CustomPayloadS2CPacket packet);
		public void onNHPCHandleBlockAction(ClientPlayNetworkHandler sender, BlockEventS2CPacket packet);
		public void onNHPCDisconnect(ClientPlayNetworkHandler sender, Text reason);
		public void onCrashReportPopulateEnvironment(CrashReport report);
		public void injectWDLButtons(GameMenuScreen gui, Collection<AbstractButtonWidget> buttonList,
				Consumer<AbstractButtonWidget> addButton);
		public void handleWDLButtonClick(GameMenuScreen gui, ButtonWidget button);
	}

	private static class BootstrapHooksListener implements IHooksListener {
		// All of these methods other than the crash one first bootstrap WDL,
		// and then forward the event to the new listener (which should have changed)
		private void bootstrap() {
			WDL.bootstrap(MinecraftClient.getInstance());
			if (listener == this) {
				throw new AssertionError("WDL bootstrap failed to change WDLHooks listener from " + this);
			}
		}

		@Override
		public void onWorldClientTick(ClientWorld sender) {
			bootstrap();
			listener.onWorldClientTick(sender);
		}

		@Override
		public void onWorldClientRemoveEntityFromWorld(ClientWorld sender, int eid) {
			bootstrap();
			listener.onWorldClientRemoveEntityFromWorld(sender, eid);
		}

		@Override
		public void onNHPCHandleChunkUnload(ClientPlayNetworkHandler sender, ClientWorld world, UnloadChunkS2CPacket packet) {
			bootstrap();
			listener.onNHPCHandleChunkUnload(sender, world, packet);
		}

		@Override
		public void onNHPCHandleChat(ClientPlayNetworkHandler sender, GameMessageS2CPacket packet) {
			bootstrap();
			listener.onNHPCHandleChat(sender, packet);
		}

		@Override
		public void onNHPCHandleMaps(ClientPlayNetworkHandler sender, MapUpdateS2CPacket packet) {
			bootstrap();
			listener.onNHPCHandleMaps(sender, packet);
		}

		@Override
		public void onNHPCHandleCustomPayload(ClientPlayNetworkHandler sender,
				CustomPayloadS2CPacket packet) {
			bootstrap();
			listener.onNHPCHandleCustomPayload(sender, packet);
		}

		@Override
		public void onNHPCHandleBlockAction(ClientPlayNetworkHandler sender, BlockEventS2CPacket packet) {
			bootstrap();
			listener.onNHPCHandleBlockAction(sender, packet);
		}

		@Override
		public void onNHPCDisconnect(ClientPlayNetworkHandler sender, Text reason) {
			bootstrap();
			listener.onNHPCDisconnect(sender, reason);
		}

		@Override
		public void injectWDLButtons(GameMenuScreen gui, Collection<AbstractButtonWidget> buttonList,
				Consumer<AbstractButtonWidget> addButton) {
			bootstrap();
			listener.injectWDLButtons(gui, buttonList, addButton);
		}

		@Override
		public void handleWDLButtonClick(GameMenuScreen gui, ButtonWidget button) {
			bootstrap();
			listener.handleWDLButtonClick(gui, button);
		}

		// NOTE: This does NOT bootstrap, as we do not want to bootstrap in a crash
		@Override
		public void onCrashReportPopulateEnvironment(CrashReport report) {
			// Trick the crash report handler into not storing a stack trace
			// (we don't want it)
			int stSize;
			try {
				stSize = Thread.currentThread().getStackTrace().length - 1;
			} catch (Exception e) {
				// Ignore
				stSize = 0;
			}
			
			CrashReportSection cat = report.addElement(
					"World Downloader Mod - not bootstrapped yet", stSize);
			cat.add("WDL version", VersionConstants::getModVersion);
			cat.add("Targeted MC version", VersionConstants::getExpectedVersion);
			cat.add("Actual MC version", VersionConstants::getMinecraftVersion);
		}
	}

	/**
	 * Called when {@link ClientWorld#tick()} is called.
	 * <br/>
	 * Should be at end of the method.
	 */
	public static void onWorldClientTick(ClientWorld sender) {
		listener.onWorldClientTick(sender);
	}

	/**
	 * Called when {@link ClientWorld#removeEntityFromWorld(int)} is called.
	 * <br/>
	 * Should be at the start of the method.
	 *
	 * @param eid
	 *            The entity's unique ID.
	 */
	public static void onWorldClientRemoveEntityFromWorld(ClientWorld sender,
			int eid) {
		listener.onWorldClientRemoveEntityFromWorld(sender, eid);
	}

	/**
	 * Called when {@link ClientPlayNetworkHandler#processChunkUnload(UnloadChunkS2CPacket)} is called.
	 * <br/>
	 * Should be at the start of the method.
	 */
	public static void onNHPCHandleChunkUnload(ClientPlayNetworkHandler sender,
			ClientWorld world, UnloadChunkS2CPacket packet) {
		listener.onNHPCHandleChunkUnload(sender, world, packet);
	}

	/**
	 * Called when {@link ClientPlayNetworkHandler#handleChat(SChatPacket)} is
	 * called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleChat(ClientPlayNetworkHandler sender, GameMessageS2CPacket packet) {
		listener.onNHPCHandleChat(sender, packet);
	}

	/**
	 * Called when {@link ClientPlayNetworkHandler#handleMaps(ChunkDataS2CPacket)} is
	 * called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleMaps(ClientPlayNetworkHandler sender, MapUpdateS2CPacket packet) {
		listener.onNHPCHandleMaps(sender, packet);
	}

	/**
	 * Called when
	 * {@link ClientPlayNetworkHandler#handleCustomPayload(CustomPayloadS2CPacket)}
	 * is called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleCustomPayload(ClientPlayNetworkHandler sender,
			CustomPayloadS2CPacket packet) {
		listener.onNHPCHandleCustomPayload(sender, packet);
	}

	/**
	 * Called when
	 * {@link ClientPlayNetworkHandler#handleBlockAction(BlockEventS2CPacket)} is
	 * called.
	 * <br/>
	 * Should be at the end of the method.
	 */
	public static void onNHPCHandleBlockAction(ClientPlayNetworkHandler sender,
			BlockEventS2CPacket packet) {
		listener.onNHPCHandleBlockAction(sender, packet);
	}

	/**
	 * Called when {@link ClientPlayNetworkHandler#onDisconnect(Text)} is called.
	 * <br/>
	 * Should be at the start of the method.
	 *
	 * @param reason The reason for the disconnect, as passed to onDisconnect.
	 */
	public static void onNHPCDisconnect(ClientPlayNetworkHandler sender, Text reason) {
		listener.onNHPCDisconnect(sender, reason);
	}

	/**
	 * Injects WDL information into a crash report.
	 *
	 * Called at the end of {@link CrashReport#populateEnvironment()}.
	 * @param report
	 */
	public static void onCrashReportPopulateEnvironment(CrashReport report) {
		listener.onCrashReportPopulateEnvironment(report);
	}


	/**
	 * Adds WDL's buttons to the pause menu GUI.
	 *
	 * @param gui        The GUI
	 * @param buttonList The list of buttons in the GUI. This list should not be
	 *                   modified directly.
	 * @param addButton  Method to add a button to the GUI.
	 */
	public static void injectWDLButtons(GameMenuScreen gui, Collection<AbstractButtonWidget> buttonList,
			Consumer<AbstractButtonWidget> addButton) {
		listener.injectWDLButtons(gui, buttonList, addButton);
	}
	/**
	 * Handle clicks to the ingame pause GUI, specifically for the disconnect
	 * button.
	 *
	 * @param gui    The GUI
	 * @param button The button that was clicked.
	 */
	public static void handleWDLButtonClick(GameMenuScreen gui, ButtonWidget button) {
		listener.handleWDLButtonClick(gui, button);
	}
}
