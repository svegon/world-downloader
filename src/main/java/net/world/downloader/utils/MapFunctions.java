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
package net.world.downloader.utils;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.world.World;
import net.world.downloader.api.IDimensionWrapper;
import net.world.downloader.utils.HandlerFunctions.DimensionWrapper;

/**
 * Functions related to maps (the item).
 *
 * In 1.13.1 and later, loadMapData takes a string and the dimension field is a
 * DimensionType.
 */
final class MapFunctions {
	private MapFunctions() {
		throw new AssertionError();
	}

	/* (non-javadoc)
	 * {@see VersionedFunctions#getMapData}
	 */
	@Nullable
	public static MapState getMapData(World world, MapUpdateS2CPacket mapPacket) {
		return world.getMapState("map_" + mapPacket.getId());
	}

	/* (non-javadoc)
	 * {@see VersionedFunctions#getMapID}
	 */
	public static int getMapID(ItemStack stack) {
		return FilledMapItem.getMapId(stack);
	}

	/* (non-javadoc)
	 * {@see VersionedFunctions#isMapDimensionNull}
	 */
	public static boolean isMapDimensionNull(MapState map) {
		return map.dimension == null;
	}

	/* (non-javadoc)
	 * {@see VersionedFunctions#setMapDimension}
	 */
	public static void setMapDimension(MapState map, IDimensionWrapper dim) {
		map.dimension = ((DimensionWrapper)dim).getWorldKey();
	}
}
