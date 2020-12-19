/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2018-2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.server.world.SimpleTickScheduler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkTickScheduler;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.storage.RegionBasedStorage;
import net.minecraft.world.storage.RegionFile;
import net.minecraft.world.storage.StorageIoWorker;
import net.minecraft.world.storage.VersionedChunkStorage;
import net.world.downloader.api.IDimensionWrapper;
import net.world.downloader.api.ISaveHandlerWrapper;
import net.world.downloader.config.settings.MiscSettings;
import net.world.downloader.utils.ReflectionUtils;
import net.world.downloader.utils.VersionedFunctions;

/**
 * Alternative implementation of {@link VersionedChunkStorage} that handles editing
 * WDL-specific properties of chunks as they are being saved.
 *
 * This variant is used for chunks from 1.13 and later.
 */
public abstract class WDLChunkLoaderBase extends VersionedChunkStorage {
	protected final WDL wdl;
	/**
	 * Location where chunks are saved.
	 *
	 * In this version, this directly is the region folder for the given dimension;
	 * for the overworld it is world/region and others it is world/DIM#/region.
	 */
	protected final File chunkSaveLocation;

	// XXX HACK this is burried deep, and probably shouldn't be directly accessed
	protected final Long2ObjectLinkedOpenHashMap<RegionFile> cache;
	
	/**
	 * Gets the save folder for the given WorldProvider, respecting Forge's
	 * dimension names if forge is present.
	 */
	protected static File getWorldSaveFolder(ISaveHandlerWrapper handler,
			IDimensionWrapper dimension) {
		File baseFolder = handler.getWorldDirectory();
		// XXX No forge support at this time

		File dimensionFolder;
		if (WDL.serverProps.getValue(MiscSettings.FORCE_DIMENSION_TO_OVERWORLD)) {
			dimensionFolder = baseFolder;
		} else {
			@Nullable String dimName = dimension.getFolderName();
			if (dimName == null) {
				// Assume that this is the overworld.
				dimensionFolder = baseFolder;
			} else {
				dimensionFolder = new File(baseFolder, dimName);
			}
		}

		return new File(dimensionFolder, "region");
	}

	@SuppressWarnings({ "unchecked" })
	protected WDLChunkLoaderBase(WDL wdl, File file) {
		super(file, null, /* enable flushing */true);
		this.wdl = wdl;
		this.chunkSaveLocation = file;
		StorageIoWorker worker = ReflectionUtils.findAndGetPrivateField(this, VersionedChunkStorage.class,
				StorageIoWorker.class);
		RegionBasedStorage rfc = ReflectionUtils.findAndGetPrivateField(worker, RegionBasedStorage.class);
		this.cache = ReflectionUtils.findAndGetPrivateField(rfc, Long2ObjectLinkedOpenHashMap.class);
	}

	/**
	 * Saves the given chunk.
	 *
	 * Note that while the normal implementation swallows Exceptions, this
	 * version does not.
	 */
	public synchronized void saveChunk(World world, Chunk c) throws Exception {
		wdl.saveHandler.checkSessionLock();

		CompoundTag levelTag = writeChunkToNBT(c, world);

		CompoundTag rootTag = new CompoundTag();
		rootTag.put("Level", levelTag);
		rootTag.putInt("DataVersion", VersionConstants.getDataVersion());

		setTagAt(c.getPos(), rootTag);

		wdl.unloadChunk(c.getPos());
	}

	/**
	 * Writes the given chunk, creating an NBT compound tag.
	 *
	 * Note that this does <b>not</b> override the private method
	 * {@link AnvilChunkLoader#writeChunkToNBT(Chunk, World, NBTCompoundTag)}.
	 * That method is private and cannot be overridden; plus, this version
	 * returns a tag rather than modifying the one passed as an argument.
	 *
	 * @param c
	 *            The chunk to write
	 * @param world
	 *            The world the chunk is in, used to determine the modified
	 *            time.
	 * @return A new CompoundTag
	 */
	
	public CompoundTag writeChunkToNBT(Chunk c, World world) {
		CompoundTag compound = new CompoundTag();
		ChunkPos chunkpos = c.getPos();
		UpgradeData upgradedata = c.getUpgradeData();
		
		compound.putInt("xPos", chunkpos.x);
		compound.putInt("zPos", chunkpos.z);
		compound.putLong("LastUpdate", world.getTime());
		compound.putLong("InhabitedTime", c.getInhabitedTime());
		compound.putString("Status", ChunkStatus.FULL.toString()); // Make sure that the chunk is considered fully generated

		if (!upgradedata.isDone()) {
			compound.put("UpgradeData", upgradedata.toTag());
		}

		ChunkSection[] chunkSections = c.getSectionArray();
		ListTag chunkSectionList = new ListTag();
		LightingProvider worldlightmanager = world.getChunkManager().getLightingProvider();

		// XXX: VersionedFunctions.hasSkyLight is inapplicable here presumably, but it might still need to be used somehow
		for(int y = -1; y < 17; ++y) {
			final int f_y = y; // Compiler food
			ChunkSection chunkSection = Arrays.stream(chunkSections)
					.filter(section -> section != null && section.getYOffset() >> 4 == f_y)
					.findFirst()
					.orElse(WorldChunk.EMPTY_SECTION);
			
			ChunkNibbleArray blocklightArray = worldlightmanager.get(LightType.BLOCK)
					.getLightSection(ChunkSectionPos.from(chunkpos, y));
			ChunkNibbleArray skylightArray = worldlightmanager.get(LightType.SKY)
					.getLightSection(ChunkSectionPos.from(chunkpos, y));
			if (chunkSection != WorldChunk.EMPTY_SECTION || blocklightArray != null
					|| skylightArray != null) {
				CompoundTag sectionNBT = new CompoundTag();
				sectionNBT.putByte("Y", (byte) (y & 255));
				
				if (chunkSection != WorldChunk.EMPTY_SECTION) {
					chunkSection.getContainer().write(sectionNBT, "Palette", "BlockStates");
				}

				if (blocklightArray != null && !blocklightArray.isUninitialized()) {
					sectionNBT.putByteArray("BlockLight", blocklightArray.asByteArray());
				}

				if (skylightArray != null && !skylightArray.isUninitialized()) {
					sectionNBT.putByteArray("SkyLight", skylightArray.asByteArray());
				}

				chunkSectionList.add(sectionNBT);
			}
		}

		compound.put("Sections", chunkSectionList);

		if (c.isLightOn()) {
			compound.putBoolean("isLightOn", true);
		}

		BiomeArray biomes = c.getBiomeArray();
		
		if (biomes != null) {
			compound.putIntArray("Biomes", biomes.toIntArray());
		}

		ListTag entityList = getEntityList(c);
		compound.put("Entities", entityList);

		ListTag tileEntityList = getTileEntityList(c);
		compound.put("TileEntities", tileEntityList);

		// XXX: Note: This was re-sorted on mojang's end; I've undone that.
		if (world.getBlockTickScheduler() instanceof ServerTickScheduler) {
			compound.put("TileTicks", ((ServerTickScheduler<?>) world.getBlockTickScheduler())
					.toTag(chunkpos));
		}
		if (world.getFluidTickScheduler() instanceof ServerTickScheduler) {
			compound.put("LiquidTicks", ((ServerTickScheduler<?>) world.getFluidTickScheduler())
					.toTag(chunkpos));
		}

		compound.put("PostProcessing", listArrayToTag(c.getPostProcessingLists()));

		if (c.getBlockTickScheduler() instanceof ChunkTickScheduler) {
			compound.put("ToBeTicked", ((ChunkTickScheduler<?>) c.getBlockTickScheduler()).toNbt());
		}

		// XXX: These are new, and they might conflict with the other one.  Not sure which should be used.
		if (c.getBlockTickScheduler() instanceof SimpleTickScheduler) {
			compound.put("TileTicks", ((SimpleTickScheduler<?>) c.getBlockTickScheduler()).toNbt());
		}

		if (c.getFluidTickScheduler() instanceof ChunkTickScheduler) {
			compound.put("LiquidsToBeTicked", ((ChunkTickScheduler<?>) c.getFluidTickScheduler())
					.toNbt());
		}

		if (c.getFluidTickScheduler() instanceof SimpleTickScheduler) {
			compound.put("LiquidTicks", ((SimpleTickScheduler<?>) c.getFluidTickScheduler()).toNbt());
		}

		CompoundTag heightMaps = new CompoundTag();

		for (Entry<Heightmap.Type, Heightmap> entry : c.getHeightmaps()) {
			if (c.getStatus().getHeightmapTypes().contains(entry.getKey())) {
				heightMaps.put(entry.getKey().getName(),
						new LongArrayTag(entry.getValue().asLongArray()));
			}
		}

		compound.put("Heightmaps", heightMaps);
		// TODO
		//compound.put("Structures",
		//		writeStructures(chunkpos, chunk.getStructureStarts(), chunk.getStructureReferences()));
		return compound;
	}

	private static ListTag listArrayToTag(ShortList[] list) {
		ListTag listnbt = new ListTag();

		for (ShortList shortlist : list) {
			ListTag sublist;
			if (shortlist != null) {
				sublist = VersionedFunctions.createShortListTag(shortlist.toShortArray());
			} else {
				sublist = VersionedFunctions.createShortListTag();
			}

			listnbt.add(sublist);
		}

		return listnbt;
	}

	protected abstract ListTag getEntityList(Chunk c);
	protected abstract ListTag getTileEntityList(Chunk c);

	/**
	 * Gets a count of how many chunks there are that still need to be written to
	 * disk. (Does not include any chunk that is currently being written to disk)
	 *
	 * @return The number of chunks that still need to be written to disk
	 */
	public synchronized int getNumPendingChunks() {
		return this.cache.size(); // XXX This is actually the number of regions
	}

	/**
	 * Provided since the constructor changes between versions.
	 */
	protected RegionFile createRegionFile(File file) throws IOException {
		return new RegionFile(file, this.chunkSaveLocation, /*enable flushing*/false);
	}

	// I don't know if it should be this.close() or this.completeAll() -Svegon
	public void flush() {
		this.completeAll();
	}
}
