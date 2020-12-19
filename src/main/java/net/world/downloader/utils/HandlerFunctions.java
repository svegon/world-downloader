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
package net.world.downloader.utils;


import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Lifecycle;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.world.downloader.api.IDimensionWrapper;
import net.world.downloader.api.ISaveHandlerWrapper;
import net.world.downloader.handler.block.BarrelHandler;
import net.world.downloader.handler.block.BeaconHandler;
import net.world.downloader.handler.block.BlastFurnaceHandler;
import net.world.downloader.handler.block.BlockHandler;
import net.world.downloader.handler.block.BrewingStandHandler;
import net.world.downloader.handler.block.ChestHandler;
import net.world.downloader.handler.block.DispenserHandler;
import net.world.downloader.handler.block.DropperHandler;
import net.world.downloader.handler.block.FurnaceHandler;
import net.world.downloader.handler.block.HopperHandler;
import net.world.downloader.handler.block.LecternHandler;
import net.world.downloader.handler.block.ShulkerBoxHandler;
import net.world.downloader.handler.block.SmokerHandler;
import net.world.downloader.handler.block.TrappedChestHandler;
import net.world.downloader.handler.entity.EntityHandler;
import net.world.downloader.handler.entity.HopperMinecartHandler;
import net.world.downloader.handler.entity.HorseHandler;
import net.world.downloader.handler.entity.StorageMinecartHandler;
import net.world.downloader.handler.entity.VillagerHandler;

public final class HandlerFunctions {
	private HandlerFunctions() { throw new AssertionError(); }

	// NOTE: func_239770_b_ creates a new instance each time!  Even this use might be wrong;
	// probably vanilla's should be in use.  (XXX)
	public static final DynamicRegistryManager.Impl DYNAMIC_REGISTRIES = DynamicRegistryManager.create();

	public static final DimensionWrapper NETHER =
			new DimensionWrapper(DimensionType.THE_NETHER_REGISTRY_KEY, World.NETHER);
	public static final DimensionWrapper OVERWORLD =
			new DimensionWrapper(DimensionType.OVERWORLD_REGISTRY_KEY, World.OVERWORLD);
	public static final DimensionWrapper END =
			new DimensionWrapper(DimensionType.THE_END_REGISTRY_KEY, World.END);

	// TODO: This doesn't interact with the values above, but I'm not sure how to best handle that
	private static Map<World, DimensionWrapper> dimensions = new WeakHashMap<>();

	/* (non-javadoc)
	 * @see VersionedFunctions#getDimension
	 */
	public static DimensionWrapper getDimension(World world) {
		return dimensions.computeIfAbsent(world, DimensionWrapper::new);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#hasSkyLight
	 */
	static boolean hasSkyLight(World world) {
		// 1.11+: use hasSkyLight
		return getDimension(world).getType().hasSkyLight();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#BLOCK_HANDLERS
	 */
	static final ImmutableList<BlockHandler<?, ?>> BLOCK_HANDLERS = ImmutableList.of(
			new BarrelHandler(),
			new BeaconHandler(),
			new BrewingStandHandler(),
			new BlastFurnaceHandler(),
			new ChestHandler(),
			new DispenserHandler(),
			new DropperHandler(),
			new FurnaceHandler(),
			new HopperHandler(),
			new LecternHandler(),
			new ShulkerBoxHandler(),
			new SmokerHandler(),
			new TrappedChestHandler()
	);

	/* (non-javadoc)
	 * @see VersionedFunctions#ENTITY_HANDLERS
	 */
	public static final ImmutableList<EntityHandler<?, ?>> ENTITY_HANDLERS = ImmutableList.of(
			new HopperMinecartHandler(),
			new HorseHandler(),
			new StorageMinecartHandler(),
			new VillagerHandler()
	);

	/* (non-javadoc)
	 * @see VersionedFunctions#shouldImportBlockEntity
	 */
	public static boolean shouldImportBlockEntity(String entityID, BlockPos pos,
			Block block, CompoundTag blockEntityNBT, Chunk chunk) {
		// Note sBlock do not have a block entity in this version.
		if (block instanceof ChestBlock && entityID.equals("minecraft:chest")) {
			return true;
		} else if (block instanceof TrappedChestBlock && entityID.equals("minecraft:trapped_chest")) {
			// Separate block entity from regular chests in this version.
			return true;
		} else if (block instanceof DispenserBlock && entityID.equals("minecraft:dispenser")) {
			return true;
		} else if (block instanceof DropperBlock && entityID.equals("minecraft:dropper")) {
			return true;
		} else if (block instanceof FurnaceBlock && entityID.equals("minecraft:furnace")) {
			return true;
		} else if (block instanceof BrewingStandBlock && entityID.equals("minecraft:brewing_stand")) {
			return true;
		} else if (block instanceof HopperBlock && entityID.equals("minecraft:hopper")) {
			return true;
		} else if (block instanceof BeaconBlock && entityID.equals("minecraft:beacon")) {
			return true;
		} else if (block instanceof ShulkerBoxBlock && entityID.equals("minecraft:shulker_box")) {
			return true;
		} else if (block instanceof CommandBlock && entityID.equals("minecraft:command_block")) {
			// Only import command sBlock if the current world doesn't have a command set
			// for the one there, as WDL doesn't explicitly save them so we need to use the
			// one currently present in the world.
			BlockEntity temp = chunk.getBlockEntity(pos);
			if (temp == null || !(temp instanceof CommandBlockBlockEntity)) {
				// Bad/missing data currently there, import the old data
				return true;
			}
			CommandBlockBlockEntity te = (CommandBlockBlockEntity) temp;
			boolean currentBlockHasCommand = !te.getCommandExecutor().getCommand().isEmpty();
			// Only import if the current command block has no command.
			return !currentBlockHasCommand;
		} else {
			return false;
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#createNewBlockEntity
	 */
	@Nullable
	public static BlockEntity createNewBlockEntity(World world, BlockWithEntity block, BlockState state) {
		return block.createBlockEntity(world);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getSaveHandler
	 */
	public static ISaveHandlerWrapper getSaveHandler(MinecraftClient minecraft, String worldName)
			throws Exception {
		return new LevelSaveWrapper(minecraft.getLevelStorage().createSession(worldName));
	}

	static class LevelSaveWrapper implements ISaveHandlerWrapper {
		public final LevelStorage.Session save;
		public LevelSaveWrapper(LevelStorage.Session save) {
			this.save = save;
		}

		@Override
		public void close() throws Exception {
			this.save.close();
		}

		@Override
		public File getWorldDirectory() {
			// XXX: This is rather dubious
			return this.save.getWorldDirectory(OVERWORLD.getWorldKey());
		}

		@Override
		public void checkSessionLock() throws Exception {
			// Happens automatically?  func_237301_i_ does it, but it's not public.
			// Use func_237298_f_, which calls it and otherwise doesn't do much (it gets icon.png)
			this.save.getIconFile();
		}

		@Override
		public LevelStorage.Session getWrapped() {
			return this.save;
		}

		@Override
		public String toString() {
			return "LevelSaveWrapper [save=" + save + "]";
		}
	}

	public static class DimensionWrapper implements IDimensionWrapper {
		private final DimensionType dimensionType;
		private final RegistryKey<World> worldKey;

		public DimensionWrapper(World world) {
			this.dimensionType = world.getDimension();
			this.worldKey = world.getRegistryKey();
		}

		public DimensionWrapper(RegistryKey<DimensionType> dimensionTypeKey,
				RegistryKey<World> worldKey) {
			Registry<DimensionType> dimTypeReg = DYNAMIC_REGISTRIES.getDimensionTypes();
			this.dimensionType = dimTypeReg.get(dimensionTypeKey);
			this.worldKey = worldKey;
		}

		@Override
		public String getFolderName() {
			if (this.worldKey == World.OVERWORLD) {
				return "DIM1";
			} else if (this.worldKey == World.NETHER) {
				return "DIM-1";
			}
			return null;
		}

		@Override
		public DimensionType getType() {
			return this.dimensionType;
		}

		@Override
		public RegistryKey<DimensionType> getTypeKey() {
			return null;
		}

		@Override
		public RegistryKey<World> getWorldKey() {
			return this.worldKey;
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#writeAdditionalPlayerData
	 */
	public static void writeAdditionalPlayerData(ClientPlayerEntity player, CompoundTag nbt) {
		nbt.putString("Dimension", player.world.getRegistryManager().getDimensionTypes()
				.getId(player.world.getDimension()).toString());
		// TODO: handle everything in ServerPlayerEntity (but nothing is completely required)
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getWorldInfoNbt
	 */
	public static CompoundTag getWorldInfoNbt(ClientWorld world, CompoundTag playerNBT) {
		ClientWorld.Properties clientInfo = world.getLevelProperties();
		DynamicRegistryManager dynamicRegistries = world.getRegistryManager();
		return new LevelProperties(new LevelInfo("LevelName", GameMode.CREATIVE, false,
				clientInfo.getDifficulty(), true, new GameRules(), DataPackSettings.SAFE_MODE),
				GeneratorOptions.method_31112(dynamicRegistries),
				Lifecycle.stable()).cloneWorldTag(dynamicRegistries, playerNBT);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#VANILLA_VILLAGER_CAREERS
	 */
	public static final Int2ObjectMap<BiMap<String, Integer>> VANILLA_VILLAGER_CAREERS = new Int2ObjectArrayMap<>();
	static {
		BiMap<String, Integer> farmer = HashBiMap.create(4);
		farmer.put("entity.minecraft.villager.farmer", 1);
		farmer.put("entity.minecraft.villager.fisherman", 2);
		farmer.put("entity.minecraft.villager.shepherd", 3);
		farmer.put("entity.minecraft.villager.fletcher", 4);
		BiMap<String, Integer> librarian = HashBiMap.create(2);
		librarian.put("entity.minecraft.villager.librarian", 1);
		librarian.put("entity.minecraft.villager.cartographer", 2);
		BiMap<String, Integer> priest = HashBiMap.create(1);
		priest.put("entity.minecraft.villager.cleric", 1);
		BiMap<String, Integer> blacksmith = HashBiMap.create(3);
		blacksmith.put("entity.minecraft.villager.armorer", 1);
		blacksmith.put("entity.minecraft.villager.weapon_smith", 2);
		blacksmith.put("entity.minecraft.villager.tool_smith", 3);
		BiMap<String, Integer> butcher = HashBiMap.create(2);
		butcher.put("entity.minecraft.villager.butcher", 1);
		butcher.put("entity.minecraft.villager.leatherworker", 2);
		BiMap<String, Integer> nitwit = HashBiMap.create(1);
		nitwit.put("entity.minecraft.villager.nitwit", 1);

		VANILLA_VILLAGER_CAREERS.put(0, farmer);
		VANILLA_VILLAGER_CAREERS.put(1, librarian);
		VANILLA_VILLAGER_CAREERS.put(2, priest);
		VANILLA_VILLAGER_CAREERS.put(3, blacksmith);
		VANILLA_VILLAGER_CAREERS.put(4, butcher);
		VANILLA_VILLAGER_CAREERS.put(5, nitwit);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getEntityX
	 */
	public static double getEntityX(Entity e) {
		return e.getX();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getEntityY
	 */
	public static double getEntityY(Entity e) {
		return e.getY();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getEntityZ
	 */
	public static double getEntityZ(Entity e) {
		return e.getZ();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#setEntityPos
	 */
	public static void setEntityPos(Entity e, double x, double y, double z) {
		e.setPos(x, y, z);
	}
}
