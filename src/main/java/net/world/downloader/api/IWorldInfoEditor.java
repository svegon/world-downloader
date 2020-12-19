/*
 * This file is part of the World Downloader API.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2017 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * You are free to include the World Downloader API within your own mods, as
 * permitted via the MMPLv2.
 */
package net.world.downloader.api;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelStorage;

/**
 * {@link IWDLMod} that edits the world info NBT file (level.dat).
 */
public interface IWorldInfoEditor extends IWDLMod {
	/**
	 * Edits the world info NBT before it is saved.
	 *
	 * @param world
	 *            The world that is being saved ({@link wdl.WDL#worldClient})
	 * @param properties
	 *            The given world's {@link WorldInfo}.
	 * @param session
	 *            The current saveHandler ({@link wdl.WDL#saveHandler}).
	 * @param tag
	 *            The current {@link NBTTagCompound} that is being saved. Edit
	 *            or add info to this.
	 */
	public abstract void editWorldInfo(ClientWorld world, ClientWorld.Properties properties,
			LevelStorage.Session session, CompoundTag tag);
}
