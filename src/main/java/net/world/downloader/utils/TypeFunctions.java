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
package net.world.downloader.utils;


import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.text.LiteralText;

/**
 * Functions that help deal with things that vary in type between versions.
 */
public final class TypeFunctions {
	/* (non-javadoc)
	 * @see VersionedFunctions#getChunksToSaveClass
	 */
	@SuppressWarnings("rawtypes")
	public static Class<Map> getChunksToSaveClass() {
		return Map.class;
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#customName
	 */
	public static LiteralText customName(String name) {
		return new LiteralText(name);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#createNewGameOptions
	 */
	@SuppressWarnings("resource")
	public static GameOptions createNewGameSettings() {
		return new GameOptions(MinecraftClient.getInstance(), MinecraftClient.getInstance().runDirectory);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getGameRendererClass
	 */
	public static Class<GameRenderer> getGameRendererClass() {
		return GameRenderer.class;
	}
}
