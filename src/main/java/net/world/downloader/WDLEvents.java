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
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.WorldChunk;
import net.world.downloader.api.IBlockEventListener;
import net.world.downloader.api.IChatMessageListener;
import net.world.downloader.api.IGuiHooksListener;
import net.world.downloader.api.IPluginChannelListener;
import net.world.downloader.api.IWorldLoadListener;
import net.world.downloader.api.WDLApi;
import net.world.downloader.api.WDLApi.ModInfo;
import net.world.downloader.config.settings.GeneratorSettings;
import net.world.downloader.gui.screens.GuiTurningCameraBase;
import net.world.downloader.gui.screens.GuiWDL;
import net.world.downloader.gui.screens.GuiWDLAbout;
import net.world.downloader.gui.screens.GuiWDLChunkOverrides;
import net.world.downloader.gui.screens.GuiWDLPermissions;
import net.world.downloader.gui.widget.WDLButton;
import net.world.downloader.handler.HandlerException;
import net.world.downloader.handler.MapDataHandler;
import net.world.downloader.handler.block.BlockHandler;
import net.world.downloader.handler.entity.EntityHandler;
import net.world.downloader.update.WDLUpdateChecker;
import net.world.downloader.utils.EntityUtils;
import net.world.downloader.utils.ReflectionUtils;
import net.world.downloader.utils.VersionedFunctions;

/**
 * Handles all of the events for WDL.
 *
 * These should be called regardless of whether downloading is
 * active; they handle that logic themselves.
 */
public class WDLEvents {
	public static void createListener(WDL wdl) {
		// TODO: Actually store this instance somewhere, instead of having it just floating about
		WDLEvents wdlEvents = new WDLEvents(wdl);
		WDLHooks.listener = new HooksListener(wdlEvents);
	}

	private WDLEvents(WDL wdl) {
		this.wdl = wdl;
	}

	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * If set, enables the profiler.  For unknown reasons, the profiler seems
	 * to use up some memory even when not enabled; see
	 * <a href="https://github.com/Pokechu22/WorldDownloader/pull/77">pull request 77</a>
	 * for more information.
	 *
	 * The compiler should eliminate all references to the profiler when set to false,
	 * as per <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1-110-C">JLS §13.1</a>
	 * constants must be inlined.  It is not guaranteed that the compiler eliminates
	 * code in an <code>if (false)</code> condition (per JLS §14.9.1) but javac does
	 * optimize it out, as may be verified by javap.
	 */
	private static final boolean ENABLE_PROFILER = false;
	private static final Profiler PROFILER = ENABLE_PROFILER ? MinecraftClient.getInstance().getProfiler() : null;

	private final WDL wdl;

	/**
	 * Must be called after the static World object in Minecraft has been
	 * replaced.
	 */
	public void onWorldLoad(ClientWorld world) {
		if (ENABLE_PROFILER) PROFILER.push("Core");

		if (wdl.minecraft.isIntegratedServerRunning()) {
			// Don't do anything else in single player

			if (ENABLE_PROFILER) PROFILER.pop();  // "Core"
			return;
		}

		// If already downloading
		if (WDL.downloading) {
			// If not currently saving, stop the current download and start
			// saving now
			if (!WDL.saving) {
				wdl.saveForWorldChange();
			}

			if (ENABLE_PROFILER) PROFILER.pop();  // "Core"
			return;
		}

		boolean sameServer = wdl.loadWorld();

		// Disabled to avoid updating to a forge version. TODO
		//WDLUpdateChecker.startIfNeeded();  // TODO: Always check for updates, even in single player

		if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

		for (ModInfo<IWorldLoadListener> info : WDLApi
				.getImplementingExtensions(IWorldLoadListener.class)) {
			if (ENABLE_PROFILER) PROFILER.push(info.id);
			info.mod.onWorldLoad(world, sameServer);
			if (ENABLE_PROFILER) PROFILER.pop();  // info.id
		}
	}

	/**
	 * Must be called when a chunk is no longer needed and is about to be removed.
	 */
	public void onChunkNoLongerNeeded(WorldChunk unneededChunk) {
		if (!WDL.downloading) { return; }

		if (unneededChunk == null) {
			return;
		}

		if (WDLPluginChannels.canSaveChunk(unneededChunk)) {
			WDLMessages.chatMessageTranslated(
					WDL.serverProps,
					WDLMessageTypes.ON_CHUNK_NO_LONGER_NEEDED,
					"wdl.messages.onChunkNoLongerNeeded.saved", unneededChunk.getPos().x, unneededChunk.getPos().z);
			wdl.saveChunk(unneededChunk);
		} else {
			WDLMessages.chatMessageTranslated(
					WDL.serverProps,
					WDLMessageTypes.ON_CHUNK_NO_LONGER_NEEDED,
					"wdl.messages.onChunkNoLongerNeeded.didNotSave", unneededChunk.getPos().x, unneededChunk.getPos().z);
		}
	}

	/**
	 * Must be called when a GUI that receives item stacks from the server is
	 * shown.
	 */
	public void onItemGuiOpened() {
		if (!WDL.downloading) { return; }

		HitResult result = wdl.minecraft.crosshairTarget;
		if (result == null) {
			// This case previously was hit via https://bugs.mojang.com/browse/MC-79925
			// but that was fixed in 1.14, so this should be impossible now.
			wdl.lastEntity = null;
			wdl.lastClickedBlock = null;
			return;
		}

		switch (result.getType()) {
		case ENTITY:
			wdl.lastEntity = ((EntityHitResult) result).getEntity();
			wdl.lastClickedBlock = null;
			break;
		case BLOCK:
			wdl.lastEntity = null;
			wdl.lastClickedBlock = ((BlockHitResult) result).getBlockPos();
			break;
		case MISS:
			wdl.lastEntity = null;
			wdl.lastClickedBlock = null;
		}
	}

	/**
	 * Must be called when a GUI that triggered an onItemGuiOpened is no longer
	 * shown.
	 */
	public boolean onItemGuiClosed() {
		if(!WDL.downloading) {
			return true;
		}

		ScreenHandler windowContainer = wdl.windowContainer;

		if(windowContainer == null ||
				ReflectionUtils.isCreativeContainer(windowContainer.getClass())) {
			// Can't do anything with null containers or the creative inventory
			return true;
		}

		Entity ridingEntity = wdl.player.getVehicle();
		
		if(ridingEntity != null) {
			// Check for ridden entities.  See EntityHandler.checkRiding for
			// more info about why this is useful.
			EntityHandler<?, ?> handler = EntityHandler.getHandler(ridingEntity.getClass(),
					windowContainer.getClass());
			if(handler != null) {
				if(handler.checkRidingCasting(windowContainer, ridingEntity)) {
					if(!WDLPluginChannels.canSaveEntities(
							ridingEntity.chunkX,
							ridingEntity.chunkZ)) {
						// Run this check now that we've confirmed that we're saving
						// the entity being ridden. If we're riding a pig but opening
						// a chest in another chunk, that should go to the other check.
						WDLMessages.chatMessageTranslated(WDL.serverProps,
								WDLMessageTypes.ON_GUI_CLOSED_INFO, "wdl.messages.onGuiClosedInfo.cannotSaveEntities");
						return true;
					}

					try {
						Text msg = handler.copyDataCasting(windowContainer, ridingEntity, true);
						WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
						return true;
					} catch (HandlerException e) {
						WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey, e.args);
						return false;
					}
				}
			} else {
				// A null handler is perfectly normal -- consider a player
				// riding a pig and then opening a chest
			}
		}

		// If the last thing clicked was an ENTITY
		Entity entity = wdl.lastEntity;
		if (entity != null) {
			if (!WDLPluginChannels.canSaveEntities(entity.chunkX, entity.chunkZ)) {
				WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO,
						"wdl.messages.onGuiClosedInfo.cannotSaveEntities");
				return true;
			}

			EntityHandler<?, ?> handler = EntityHandler.getHandler(entity.getClass(),
					windowContainer.getClass());
			if (handler != null) {
				try {
					Text msg = handler.copyDataCasting(windowContainer, entity, true);
					WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
					return true;
				} catch (HandlerException e) {
					WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey,
							e.args);
					return false;
				}
			} else {
				return false;
			}
		}

		// Else, the last thing clicked was a BLOCK ENTITY
		if (wdl.lastClickedBlock == null) {
			WDLMessages.chatMessageTranslated(WDL.serverProps,
					WDLMessageTypes.ON_GUI_CLOSED_WARNING,
					"wdl.messages.onGuiClosedWarning.noCoordinates");
			return true; // nothing else can handle this
		}

		// Get the block entity which we are going to update the inventory for
		BlockEntity te = wdl.worldClient.getBlockEntity(wdl.lastClickedBlock);

		if (te == null) {
			//TODO: Is this a good way to stop?  Is the event truely handled here?
			WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_WARNING, 
					"wdl.messages.onGuiClosedWarning.couldNotGetTE", wdl.lastClickedBlock);
			return true;
		}

		//Permissions check.
		if (!WDLPluginChannels.canSaveContainers(te.getPos().getX() >> 4, te
				.getPos().getZ() >> 4)) {
			WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO,
					"wdl.messages.onGuiClosedInfo.cannotSaveTileEntities");
			return true;
		}

		BlockHandler<? extends BlockEntity, ? extends ScreenHandler> handler =
				BlockHandler.getHandler(te.getClass(), wdl.windowContainer.getClass());
		
		if (handler != null) {
			try {
				Text msg = handler.handleCasting(wdl.lastClickedBlock, wdl.windowContainer, te,
						wdl.worldClient, wdl::saveTileEntity);
				WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
				return true;
			} catch (HandlerException e) {
				WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey, e.args);
				return false;
			}
		} else if (wdl.windowContainer instanceof GenericContainerScreenHandler 
				&& te instanceof EnderChestBlockEntity) {
			EnderChestInventory inventoryEnderChest = wdl.player.getEnderChestInventory();
			int inventorySize = inventoryEnderChest.size();
			int containerSize = wdl.windowContainer.slots.size();

			for (int i = 0; i < containerSize && i < inventorySize; i++) {
				Slot slot = wdl.windowContainer.getSlot(i);
				
				if (slot.hasStack()) {
					inventoryEnderChest.setStack(i, slot.getStack());
				}
			}

			WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO,
					"wdl.messages.onGuiClosedInfo.savedTileEntity.enderChest");
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Must be called when a block event/block action packet is received.
	 */
	public void onBlockEvent(BlockPos pos, Block block, int data1, int data2) {
		if (!WDL.downloading) {
			return;
		}

		if (!WDLPluginChannels.canSaveTileEntities(pos.getX() >> 4, pos.getZ() >> 4)) {
			return;
		}

		BlockEntity blockEntity = wdl.worldClient.getBlockEntity(pos);
		
		if (blockEntity == null) {
			return;
		}

		// Block action handlers were removed since note block data is stored in block states. -Svegon
		/*BlockActionHandler<? extends Block, ? extends BlockEntity> handler =
				BlockActionHandler.getHandler(block.getClass(), blockEntity.getClass());
		if (handler != null) {
			try {
				Text msg = handler.handleCasting(pos, block, blockEntity,
						data1, data2, wdl.worldClient, wdl::saveTileEntity);
				WDLMessages.chatMessage(WDL.serverProps, WDLMessageTypes.ON_GUI_CLOSED_INFO, msg);
			} catch (HandlerException e) {
				WDLMessages.chatMessageTranslated(WDL.serverProps, e.messageType, e.translationKey, e.args);
			}
		}*/
	}

	/**
	 * Must be called when a Map Data packet is received, to store the image on
	 * the map item.
	 */
	public void onMapDataLoaded(int mapID, @Nonnull MapState mapData) {
		if (!WDL.downloading) {
			return;
		}

		if (!WDLPluginChannels.canSaveMaps()) {
			return;
		}

		// Assume that the current dimension is the right one
		ClientPlayerEntity player = wdl.player;
		assert player != null;
		MapDataHandler.MapDataResult result = MapDataHandler.repairMapData(mapID, mapData, wdl.player);

		wdl.newMapDatas.put(mapID, result.map);

		WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.ON_MAP_SAVED,
				"wdl.messages.onMapSaved", mapID, result.toComponent());
	}

	/**
	 * Must be called whenever a plugin channel message / custom payload packet
	 * is received.
	 */
	public void onPluginChannelPacket(ClientPlayNetworkHandler sender, String channel, byte[] bytes) {
		WDLPluginChannels.onPluginChannelPacket(sender, channel, bytes);
	}

	/**
	 * Must be called when an entity is about to be removed from the world.
	 */
	public void onRemoveEntityFromWorld(Entity entity) {
		// If the entity is being removed and it's outside the default tracking
		// range, go ahead and remember it until the chunk is saved.
		if (WDL.downloading && entity != null
				&& WDLPluginChannels.canSaveEntities(entity.chunkX, entity.chunkZ)) {
			if (!EntityUtils.isEntityEnabled(entity)) {
				WDLMessages.chatMessageTranslated(
						WDL.serverProps,
						WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.allowingRemoveUserPref", entity);
				return;
			}

			int threshold = EntityUtils.getEntityTrackDistance(entity);

			if (threshold < 0) {
				WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.allowingRemoveUnrecognizedDistance", entity);
				return;
			}

			int serverViewDistance = 10; // XXX hardcoded for now

			if (EntityUtils.isWithinSavingDistance(entity, wdl.player,
					threshold, serverViewDistance)) {
				WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.savingDistance", entity, entity.getPos().toString(),
						wdl.player.getPos(), threshold, serverViewDistance);
				ChunkPos pos = new ChunkPos(entity.chunkX, entity.chunkZ);
				UUID uuid = entity.getUuid();
				
				if (wdl.entityPositions.containsKey(uuid)) {
					// Remove previous entity, to avoid saving the same one in multiple chunks.
					ChunkPos prevPos = wdl.entityPositions.get(uuid);
					boolean removedSome = wdl.newEntities.get(pos)
							.removeIf(e -> e.getUuid().equals(uuid));
					LOGGER.info("Replacing entity with UUID {} previously located at {} with new position {}.  There was an entity at old position (should be true): {}", uuid, prevPos, pos, removedSome);
				}
				
				wdl.newEntities.put(pos, entity);
				wdl.entityPositions.put(uuid, pos);
			} else {
				WDLMessages.chatMessageTranslated(WDL.serverProps, WDLMessageTypes.REMOVE_ENTITY,
						"wdl.messages.removeEntity.allowingRemoveDistance", entity,
						entity.getPos().toString(), wdl.player.getPos(), threshold, serverViewDistance);
			}
		}
	}

	/**
	 * Called upon any chat message.  Used for getting the seed.
	 */
	public void onChatMessage(String msg) {
		if (WDL.downloading && msg.startsWith("Seed: ")) {
			String seed = msg.substring(6);
			if (seed.startsWith("[") && seed.endsWith("]")) {
				// In 1.13, the seed is enclosed by brackets (and is also selectable on click)
				// We don't want those brackets.
				seed = seed.substring(1, seed.length() - 1);
			}
			wdl.worldProps.setValue(GeneratorSettings.SEED, seed);

			if (wdl.worldProps.getValue(GeneratorSettings.GENERATOR) ==
					GeneratorSettings.Generator.VOID) {

				wdl.worldProps.setValue(GeneratorSettings.GENERATOR,
						GeneratorSettings.Generator.DEFAULT);

				WDLMessages.chatMessageTranslated(WDL.serverProps,
						WDLMessageTypes.INFO, "wdl.messages.generalInfo.seedAndGenSet", seed);
			} else {
				WDLMessages.chatMessageTranslated(WDL.serverProps,
						WDLMessageTypes.INFO, "wdl.messages.generalInfo.seedSet", seed);
			}
		}
	}

	private static class HooksListener implements WDLHooks.IHooksListener {
		private final WDLEvents wdlEvents;
		private final WDL wdl;
		
		public HooksListener(WDLEvents wdlEvents) {
			this.wdlEvents = wdlEvents;
			this.wdl = wdlEvents.wdl;
		}

		@Override
		public void onWorldClientTick(ClientWorld sender) {
			try {
				if (ENABLE_PROFILER)
					PROFILER.push("wdl");

				if (sender != wdl.worldClient) {
					if (ENABLE_PROFILER)
						PROFILER.push("onWorldLoad");
					
					if (WDL.worldLoadingDeferred) {
						return;
					}

					wdlEvents.onWorldLoad(sender);
					
					if (ENABLE_PROFILER)
						PROFILER.pop();  // "onWorldLoad"
				} else {
					if (ENABLE_PROFILER)
						PROFILER.push("inventoryCheck");
					
					if (WDL.downloading && wdl.player != null) {
						if (wdl.player.currentScreenHandler != wdl.windowContainer) {
							if (wdl.player.currentScreenHandler == wdl.player.playerScreenHandler) {
								boolean handled;

								if (ENABLE_PROFILER) PROFILER.push("onItemGuiClosed");
								if (ENABLE_PROFILER) PROFILER.push("Core");
								handled = wdlEvents.onItemGuiClosed();
								if (ENABLE_PROFILER) PROFILER.pop();  // "Core"

								ScreenHandler container = wdl.player.currentScreenHandler;
								
								if (wdl.lastEntity != null) {
									Entity entity = wdl.lastEntity;

									for (ModInfo<IGuiHooksListener> info : WDLApi
											.getImplementingExtensions(IGuiHooksListener.class)) {
										if (handled) {
											break;
										}

										if (ENABLE_PROFILER)
											PROFILER.push(info.id);
										
										handled = info.mod.onEntityGuiClosed(
												sender, entity, container);
										
										if (ENABLE_PROFILER)
											PROFILER.pop();  // info.id
									}

									if (!handled) {
										WDLMessages.chatMessageTranslated(WDL.serverProps,
												WDLMessageTypes.ON_GUI_CLOSED_WARNING,
												"wdl.messages.onGuiClosedWarning.unhandledEntity",
												entity);
									}
								} else if (wdl.lastClickedBlock != null) {
									BlockPos pos = wdl.lastClickedBlock;
									
									for (ModInfo<IGuiHooksListener> info : WDLApi
											.getImplementingExtensions(IGuiHooksListener.class)) {
										if (handled) {
											break;
										}

										if (ENABLE_PROFILER)
											PROFILER.push(info.id);
										
										handled = info.mod.onBlockGuiClosed(
												sender, pos, container);
										
										if (ENABLE_PROFILER)
											PROFILER.pop();  // info.id
									}

									if (!handled) {
										WDLMessages.chatMessageTranslated(WDL.serverProps,
												WDLMessageTypes.ON_GUI_CLOSED_WARNING,
												"wdl.messages.onGuiClosedWarning.unhandledTileEntity",
												pos, sender.getBlockEntity(pos));
									}
								}

								if (ENABLE_PROFILER)
									PROFILER.pop();  // onItemGuiClosed
							} else {
								if (ENABLE_PROFILER)
									PROFILER.push("onItemGuiOpened");
								
								if (ENABLE_PROFILER)
									PROFILER.push("Core");
								
								wdlEvents.onItemGuiOpened();
								
								if (ENABLE_PROFILER)
									PROFILER.pop();  // "Core"
								
								if (ENABLE_PROFILER)
									PROFILER.pop();  // "onItemGuiOpened"
							}

							wdl.windowContainer = wdl.player.currentScreenHandler;
						}
					}
					
					if (ENABLE_PROFILER)
						PROFILER.pop();  // "inventoryCheck"
				}

				if (ENABLE_PROFILER)
					PROFILER.push("camera");
				
				GuiTurningCameraBase.onWorldTick();
				
				if (ENABLE_PROFILER)
					PROFILER.pop();  // "camera"
				
				if (ENABLE_PROFILER)
					PROFILER.pop();  // "wdl"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onWorldClientTick event");
			}
		}
		
		@Override
		public void onWorldClientRemoveEntityFromWorld(ClientWorld sender, int eid) {
			try {
				if (!WDL.downloading) {
					return;
				}

				if (ENABLE_PROFILER)
					PROFILER.push("wdl.onRemoveEntityFromWorld");

				Entity entity = sender.getEntityById(eid);

				if (ENABLE_PROFILER)
					PROFILER.push("Core");
				
				wdlEvents.onRemoveEntityFromWorld(entity);
				
				if (ENABLE_PROFILER)
					PROFILER.pop();  // "Core"

				if (ENABLE_PROFILER)
					PROFILER.pop();  // "wdl.onRemoveEntityFromWorld"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onWorldRemoveEntityFromWorld event");
			}
		}
		
		@Override
		public void onNHPCHandleChunkUnload(ClientPlayNetworkHandler sender, ClientWorld world,
				UnloadChunkS2CPacket packet) {
			try {
				if (!wdl.minecraft.isOnThread()) {
					return;
				}

				if (!WDL.downloading) {
					return;
				}

				if (ENABLE_PROFILER)
					PROFILER.push("wdl.onChunkNoLongerNeeded");
				
				WorldChunk chunk = world.getChunk(packet.getX(), packet.getZ());

				if (ENABLE_PROFILER)
					PROFILER.push("Core");
				
				wdlEvents.onChunkNoLongerNeeded(chunk);
				
				if (ENABLE_PROFILER)
					PROFILER.pop();  // "Core"

				if (ENABLE_PROFILER)
					PROFILER.pop();  // "wdl.onChunkNoLongerNeeded"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onNHPCHandleChunkUnload event");
			}
		}

		@Override
		public void onNHPCHandleChat(ClientPlayNetworkHandler sender, GameMessageS2CPacket packet) {
			try {
				if (!wdl.minecraft.isOnThread()) {
					return;
				}

				if (!WDL.downloading) {
					return;
				}

				if (ENABLE_PROFILER)
					PROFILER.push("wdl.onChatMessage");

				String chatMessage = packet.getMessage().getString();

				if (ENABLE_PROFILER
						) PROFILER.push("Core");
				
				wdlEvents.onChatMessage(chatMessage);
				
				if (ENABLE_PROFILER)
					PROFILER.pop();  // "Core"

				for (ModInfo<IChatMessageListener> info
						: WDLApi.getImplementingExtensions(IChatMessageListener.class)) {
					if (ENABLE_PROFILER)
						PROFILER.push(info.id);
					
					info.mod.onChat(wdl.worldClient, chatMessage);
					
					if (ENABLE_PROFILER)
						PROFILER.pop();  // info.id
				}

				if (ENABLE_PROFILER)
					PROFILER.pop();  // "wdl.onChatMessage"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onNHPCHandleChat event");
			}
		}
		
		@Override
		public void onNHPCHandleMaps(ClientPlayNetworkHandler sender, MapUpdateS2CPacket packet) {
			try {
				if (!wdl.minecraft.isOnThread()) {
					return;
				}

				if (!WDL.downloading) {
					return;
				}

				if (ENABLE_PROFILER)
					PROFILER.push("wdl.onMapStateLoaded");

				MapState mapData = VersionedFunctions.getMapData(wdl.worldClient, packet);

				if (mapData != null) {
					if (ENABLE_PROFILER)
						PROFILER.push("Core");
					
					wdlEvents.onMapDataLoaded(packet.getId(), mapData);
					
					if (ENABLE_PROFILER)
						PROFILER.pop();  // "Core"
				} else {
					LOGGER.warn("Received a null map data: " + packet.getId());
				}

				if (ENABLE_PROFILER
						) PROFILER.pop();  // "wdl.onMapStateLoaded"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onNHPCHandleMaps event");
			}
		}
		
		@Override
		public void onNHPCHandleCustomPayload(ClientPlayNetworkHandler sender,
				CustomPayloadS2CPacket packet) {
			try {
				if (!wdl.minecraft.isOnThread()) {
					return;
				}
				
				if (ENABLE_PROFILER)
					PROFILER.push("wdl.onPluginMessage");

				if (ENABLE_PROFILER)
					PROFILER.push("Parse");
				
				String channel = packet.getChannel().toString(); // 1.13: ResourceLocation -> String; otherwise no-op
				ByteBuf buf = packet.getData();
				int refCnt = buf.refCnt();
				
				if (refCnt <= 0) {
					// The buffer has already been released.  Just break out now.
					// This happens with e.g. the MC|TrList packet (villager trade list),
					// which closes the buffer after reading it.
					if (ENABLE_PROFILER)
						PROFILER.pop();  // "Parse"
					
					if (ENABLE_PROFILER)
						PROFILER.pop();  // "wdl.onPluginMessage"
					
					return;
				}

				// Something else may have already read the payload; return to the start
				buf.markReaderIndex();
				buf.readerIndex(0);
				
				byte[] payload = new byte[buf.readableBytes()];
				
				buf.readBytes(payload);
				// OK, now that we've done our reading, return to where it was before
				// (which could be the end, or other code might not have read it yet)
				buf.resetReaderIndex();
				// buf will be released by the packet handler, eventually.
				// It definitely is NOT our responsibility to release it, as
				// doing so would probably break other code outside of wdl.
				// Perhaps we might want to call retain once at the start of this method
				// and then release at the end, but that feels excessive (since there
				// _shouldn't_ be multiple threads at play at this point, and if there
				// were we'd be in trouble anyways).

				if (ENABLE_PROFILER)
					PROFILER.pop();  // "Parse"

				if (ENABLE_PROFILER)
					PROFILER.push("Core");
				
				wdlEvents.onPluginChannelPacket(sender, channel, payload);
				
				if (ENABLE_PROFILER)
					PROFILER.pop();  // "Core"

				for (ModInfo<IPluginChannelListener> info : WDLApi
						.getImplementingExtensions(IPluginChannelListener.class)) {
					if (ENABLE_PROFILER)
						PROFILER.push(info.id);
					
					info.mod.onPluginChannelPacket(wdl.worldClient, channel, payload);
					
					if (ENABLE_PROFILER)
						PROFILER.pop();  // info.id
				}

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onPluginMessage"
			} catch (Throwable e) {
				wdl.crashed(e,
						"WDL mod: exception in onNHPCHandleCustomPayload event");
			}
		}
		
		@Override
		public void onNHPCHandleBlockAction(ClientPlayNetworkHandler sender, BlockEventS2CPacket packet) {
			try {
				if (!wdl.minecraft.isOnThread()) {
					return;
				}

				if (!WDL.downloading) { return; }

				if (ENABLE_PROFILER) PROFILER.push("wdl.onBlockEvent");

				BlockPos pos = packet.getPos();
				Block block = packet.getBlock();
				int data1 = packet.getType();
				int data2 = packet.getData();

				if (ENABLE_PROFILER)
					PROFILER.push("Core");
				
				wdlEvents.onBlockEvent(pos, block, data1, data2);
				
				if (ENABLE_PROFILER)
					PROFILER.pop();  // "Core"

				for (ModInfo<IBlockEventListener> info : WDLApi
						.getImplementingExtensions(IBlockEventListener.class)) {
					if (ENABLE_PROFILER)
						PROFILER.push(info.id);
					
					info.mod.onBlockEvent(wdl.worldClient, pos, block,
							data1, data2);
					
					if (ENABLE_PROFILER)
						PROFILER.pop();  // info.id
				}

				if (ENABLE_PROFILER) PROFILER.pop();  // "wdl.onBlockEvent"
			} catch (Throwable e) {
				wdl.crashed(e, "WDL mod: exception in onNHPCHandleBlockAction event");
			}
		}
		@Override
		public void onNHPCDisconnect(ClientPlayNetworkHandler sender, Text reason) {
			if (WDL.downloading) {
				// This is likely to be called from an unexpected thread, so queue a task
				// if on a different thread (execute will run it immediately if on the right thread)
				wdl.minecraft.execute(wdl::stopDownload);

				// This code was present on older versions of WDL which weren't missing
				// the onDisconnect handler before.
				// It presumably makes sure that the disconnect doesn't propagate to other state variables,
				// but I don't completely trust it
				try {
					Thread.sleep(2000L);
				} catch (InterruptedException e) { }
			}
		}
		@Override
		public void onCrashReportPopulateEnvironment(CrashReport report) {
			wdl.addInfoToCrash(report);
		}

		private class StartDownloadButton extends WDLButton {
			public StartDownloadButton(Screen menu, int x, int y, int width, int height) {
				super(x, y, width, height, new LiteralText(""));
				this.menu = menu;
			}

			// The GuiScreen containing this button, as a parent for other GUIs
			private final Screen menu;

			@Override
			public void beforeDraw() {
				final Text displayString;
				final boolean enabled;
				if (wdl.minecraft.isIntegratedServerRunning()) {
					// Singleplayer
					displayString = new TranslatableText(
							"wdl.gui.ingameMenu.downloadStatus.singlePlayer");
					enabled = false;
				} else if (!WDLPluginChannels.canDownloadAtAll()) {
					if (WDLPluginChannels.canRequestPermissions()) {
						// Allow requesting permissions.
						displayString = new TranslatableText(
								"wdl.gui.ingameMenu.downloadStatus.request");
						enabled = true;
					} else {
						// Out of date plugin :/
						displayString = new TranslatableText(
								"wdl.gui.ingameMenu.downloadStatus.disabled");
						enabled = false;
					}
				} else if (WDL.saving) {
					// Normally not accessible; only happens as a major fallback...
					displayString = new TranslatableText(
							"wdl.gui.ingameMenu.downloadStatus.saving");
					enabled = false;
				} else if (WDL.downloading) {
					displayString = new TranslatableText(
							"wdl.gui.ingameMenu.downloadStatus.stop");
					enabled = true;
				} else {
					displayString = new TranslatableText(
							"wdl.gui.ingameMenu.downloadStatus.start");
					enabled = true;
				}
				this.setEnabled(enabled);
				this.setMessage(displayString);
			}

			@Override
			public void performAction() {
				if (wdl.minecraft.isIntegratedServerRunning()) {
					return; // WDL not available if in singleplayer or LAN server mode
				}

				if (WDL.downloading) {
					wdl.stopDownload();
					setEnabled(false); // Disable to stop double-clicks
				} else {
					if (!WDLPluginChannels.canDownloadAtAll()) {
						// If they don't have any permissions, let the player
						// request some.
						if (WDLPluginChannels.canRequestPermissions()) {
							wdl.minecraft.openScreen(new GuiWDLPermissions(menu, wdl));
						} else {
							// Should never happen
						}
					} else if (WDLPluginChannels.hasChunkOverrides()
							&& !WDLPluginChannels.canDownloadInGeneral()) {
						// Handle the "only has chunk overrides" state - notify
						// the player of limited areas.
						wdl.minecraft.openScreen(new GuiWDLChunkOverrides(menu, wdl));
					} else {
						wdl.startDownload();
						setEnabled(false); // Disable to stop double-clicks
					}
				}
			}
		}

		private class SettingsButton extends WDLButton {
			public SettingsButton(Screen menu, int x, int y, int width, int height, Text displayString) {
				super(x, y, width, height, displayString);
				this.menu = menu;
			}

			// The GuiScreen containing this button, as a parent for other GUIs
			private final Screen menu;

			@Override
			public void performAction() {
				if (wdl.minecraft.isIntegratedServerRunning()) {
					wdl.minecraft.openScreen(new GuiWDLAbout(menu, wdl));
				} else {
					if (wdl.promptForInfoForSettings("changeOptions", false, this::performAction, () -> wdl.minecraft.openScreen(null))) {
						return;
					}
					wdl.minecraft.openScreen(new GuiWDL(menu, wdl));
				}
			}
		}

		@SuppressWarnings("unused")
		private boolean isAdvancementsButton(ButtonWidget button) {
			Object message = button.getMessage(); // String or Text
			
			if (message instanceof String) {
				return message.equals(I18n.translate("gui.advancements"));
			} else if (message instanceof TranslatableText) {
				// Though the method returns an Text,
				// for the screen it'll be a translation component.
				return ((TranslatableText) message).getKey().equals("gui.advancements");
			} else {
				return false;
			}
		}

		@Override
		public void injectWDLButtons(GameMenuScreen gui, Collection<AbstractButtonWidget> buttonList,
				Consumer<AbstractButtonWidget> addButton) {
			int insertAtYPos = 0;

			for (Object o : buttonList) {
				if (!(o instanceof ButtonWidget)) {
					continue;
				}
				
				ButtonWidget btn = (ButtonWidget) o;
				
				if (isAdvancementsButton(btn)) {
					insertAtYPos = btn.y + 24;
					break;
				}
			}

			// Move other buttons down one slot (= 24 height units)
			for (Object o : buttonList) {
				if (!(o instanceof ButtonWidget)) {
					continue;
				}
				
				ButtonWidget btn = (ButtonWidget)o;
				
				if (btn.y >= insertAtYPos) {
					btn.y += 24;
				}
			}

			// Insert wdl buttons.
			addButton.accept(new StartDownloadButton(gui,
					gui.width / 2 - 102, insertAtYPos, 174, 20));

			addButton.accept(new SettingsButton(gui,
					gui.width / 2 + 74, insertAtYPos, 28, 20,
					new TranslatableText("wdl.gui.ingameMenu.settings")));
		}

		@Override
		public void handleWDLButtonClick(GameMenuScreen gui, ButtonWidget button) {
			if (button.getMessage().getString().equals(I18n.translate("menu.disconnect"))) { // "Disconnect", from vanilla
				wdl.stopDownload();
				// Disable the button to prevent double-clicks
				button.active = false;
			}
		}
	}
}
