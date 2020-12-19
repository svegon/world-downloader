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
package net.world.downloader.config;

import net.minecraft.text.Text;

/**
 * A cyclable setting is a setting that can be cycled via a button through a
 * fixed set of values.
 */
public interface CyclableSetting<T> extends Setting<T> {
	/**
	 * Cycles into the next value.
	 *
	 * @param value Current value
	 * @return Next value
	 */
	public abstract T cycle(T value);

	/**
	 * Gets a text component that describes this setting.
	 *
	 * @return A text component.
	 */
	public abstract Text getDescription();

	/**
	 * Gets a text component that describes this setting's current value.
	 *
	 * @param curValue The current value.
	 * @return A translation string.
	 */
	public Text getButtonText(T curValue);
}
