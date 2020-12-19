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
package net.world.downloader.config.settings;

import java.util.Map;
import java.util.function.BooleanSupplier;

import com.google.common.annotations.VisibleForTesting;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.StringIdentifiable;
import net.world.downloader.WDLPluginChannels;
import net.world.downloader.api.IConfiguration;
import net.world.downloader.config.CyclableSetting;
import net.world.downloader.utils.SettingsUtils;

/**
 * Contains settings that would be used in EntityUtils.
 * However, only the main configuration type is available at this time...
 */
public final class EntitySettings {
	private EntitySettings() { throw new AssertionError(); }

	public static final CyclableSetting<TrackDistanceMode> TRACK_DISTANCE_MODE =
		new TrackDistanceModeSetting("Entity.TrackDistanceMode", "wdl.gui.entities.trackDistanceMode");

	@VisibleForTesting
	static BooleanSupplier hasServerEntityRange = WDLPluginChannels::hasServerEntityRange;

	public enum TrackDistanceMode implements StringIdentifiable {
		DEFAULT("default"),
		SERVER("server"), // Only available in certain contexts
		USER("user");

		private final String confName;
		private static final Map<String, TrackDistanceMode> FROM_STRING = SettingsUtils.makeFromString(values(), t -> t.confName);

		private TrackDistanceMode(String confName) {
			this.confName = confName;
		}

		public static TrackDistanceMode fromString(String confName) {
			return FROM_STRING.get(confName);
		}

		@Override
		public String asString() {
			return confName;
		}
	}

	private static class TrackDistanceModeSetting implements CyclableSetting<TrackDistanceMode> {
		private final String name;
		private final String key;

		public TrackDistanceModeSetting(String name, String key) {
			this.name = name;
			this.key = key;
		}

		@Override
		public TrackDistanceMode deserializeFromString(String text) {
			return TrackDistanceMode.fromString(text);
		}

		@Override
		public String serializeToString(TrackDistanceMode value) {
			return value.asString();
		}

		@Override
		public String getConfigurationKey() {
			return name;
		}

		@Override
		public TrackDistanceMode getDefault(IConfiguration context) {
			if (hasServerEntityRange.getAsBoolean()) {
				return TrackDistanceMode.SERVER;
			} else {
				return TrackDistanceMode.DEFAULT;
			}
		}

		@Override
		public TrackDistanceMode cycle(TrackDistanceMode value) {
			if (value == TrackDistanceMode.DEFAULT) {
				if (hasServerEntityRange.getAsBoolean()) {
					return TrackDistanceMode.SERVER;
				} else {
					return TrackDistanceMode.USER;
				}
			} else if (value == TrackDistanceMode.SERVER) {
				return TrackDistanceMode.USER;
			} else {
				return TrackDistanceMode.DEFAULT;
			}
		}

		@Override
		public Text getDescription() {
			return new TranslatableText(key + ".description");
		}

		@Override
		public Text getButtonText(TrackDistanceMode curValue) {
			return new TranslatableText(key + "." + serializeToString(curValue));
		}
	}
}
