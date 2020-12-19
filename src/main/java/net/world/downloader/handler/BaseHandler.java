/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2018 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader.handler;

import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Shared logic between both block and entity handlers.
 */
public abstract class BaseHandler {
	/**
	 * Saves the items of a container to the given TileEntity.
	 *
	 * @param container
	 *            The container to save from, usually {@link wdl.WDL#windowContainer} .
	 * @param tileEntity
	 *            The TileEntity to save to.
	 * @param containerStartIndex
	 *            The index in the container to start copying items from.
	 */
	protected static void saveContainerItems(ScreenHandler container,
			LockableContainerBlockEntity tileEntity, int containerStartIndex) {
		int containerSize = container.slots.size();
		int inventorySize = tileEntity.size();
		int containerIndex = containerStartIndex;
		int inventoryIndex = 0;

		while ((containerIndex < containerSize) && (inventoryIndex < inventorySize)) {
			Slot slot = container.getSlot(containerIndex);
			
			if(slot.hasStack()) {
				tileEntity.setStack(inventoryIndex, slot.getStack());
			}
			
			inventoryIndex++;
			containerIndex++;
		}
	}
	
	protected static void saveContainerItems(BeaconScreenHandler container,
			BeaconBlockEntity tileEntity, int containerStartIndex) {
		// Beacons inventory can't be saved, so all saving is done in saveInventoryItems -Svegon
	}
}
