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
package net.world.downloader.handler;


import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.world.downloader.WDL;
import net.world.downloader.api.IEntityManager;
import net.world.downloader.utils.EntityUtils.ISpigotEntityManager;
import net.world.downloader.utils.EntityUtils.SpigotEntityType;
/**
 * Standard implementations of IEntityManager. These implementations are used on
 * 1.13.
 */
public class StandardEntityManagers {
	private StandardEntityManagers() { throw new AssertionError(); }

	// For most entities, we want them to be enabled by default. A few dangerous
	// entities should't be saved, though.
	private static final Set<String> DANGEROUS_ENTITIES = ImmutableSet.of(
			"minecraft:firework_rocket",
			"minecraft:ender_dragon",
			"minecraft:wither",
			"minecraft:tnt");

	public static final ISpigotEntityManager SPIGOT = new ISpigotEntityManager() {
		@Override
		public Set<String> getProvidedEntities() {
			if (WDL.getInstance().isSpigot()) {
				return PROVIDED_ENTITIES;
			} else {
				// Don't try to do spigot ranges on non-spigot servers
				return Collections.emptySet();
			}
		}

		@Override
		public String getIdentifierFor(Entity entity) {
			// Handled by default
			return null;
		}

		@Override
		public int getTrackDistance(String identifier, Entity entity) {
			return getSpigotType(identifier).getDefaultRange();
		}

		@Nonnull
		@Override
		public SpigotEntityType getSpigotType(String identifier) {
			Optional<EntityType<?>> otype = EntityType.get(identifier);
			
			if (!otype.isPresent()) {
				return SpigotEntityType.UNKNOWN;
			}
			
			EntityType<?> type = otype.get();
			SpawnGroup vanillaClassification = type.getSpawnGroup();
			// Spigot's mapping, which is based off of bukkit inheritance (which
			// doesn't match vanilla)
			// TODO Not sure I've ported this correctly, nor that spigot is still doing it this way
			if (vanillaClassification == SpawnGroup.MONSTER) {
				return SpigotEntityType.MONSTER;
			} else if (vanillaClassification == SpawnGroup.CREATURE ||
					vanillaClassification == SpawnGroup.AMBIENT) {
				// Is WATER_CREATURE included?  There's a lot more now...
				return SpigotEntityType.ANIMAL;
			} else if (type == EntityType.ITEM_FRAME ||
					type == EntityType.PAINTING ||
					type == EntityType.ITEM ||
					type == EntityType.EXPERIENCE_ORB) {
				return SpigotEntityType.MISC;
			} else {
				return SpigotEntityType.OTHER;
			}
		}

		@Override
		public boolean enabledByDefault(String identifier) {
			return !DANGEROUS_ENTITIES.contains(identifier);
		}

		// Not intended to be used as a regular extension, so don't worry about
		// these methods
		@Override
		public boolean isValidEnvironment(String version) {
			return true;
		}
		@Override
		public String getEnvironmentErrorMessage(String version) {
			return null;
		}

		@Override
		public String getGroup(String identifier) {
			return null;
		}

		@Override
		public String getDisplayIdentifier(String identifier) {
			return null;
		}

		@Override
		public String getDisplayGroup(String group) {
			return null;
		}
	};

	public static final IEntityManager VANILLA = new IEntityManager() {
		@Override
		public Set<String> getProvidedEntities() {
			return PROVIDED_ENTITIES;
		}

		@Override
		public String getIdentifierFor(Entity entity) {
			Identifier loc = EntityType.getId(entity.getType());
			if (loc == null) {
				// Eg players
				return null;
			} else {
				return loc.toString();
			}
		}

		/**
		 * Gets the entity tracking range used by vanilla Minecraft.
		 */
		@Override
		public int getTrackDistance(String identifier, Entity entity) {
			Optional<EntityType<?>> type = EntityType.get(identifier);
			if (!type.isPresent()) {
				return -1;
			}
			return type.get().getMaxTrackDistance() * 16;
		}

		@Override
		public String getGroup(String identifier) {
			Optional<EntityType<?>> otype = EntityType.get(identifier);
			
			if (!otype.isPresent()) {
				return null;
			}
			
			SpawnGroup classification = otype.get().getSpawnGroup();

			if (classification == SpawnGroup.MONSTER) {
				return "Hostile";
			} else if (classification == SpawnGroup.CREATURE ||
					classification == SpawnGroup.WATER_CREATURE ||
					classification == SpawnGroup.AMBIENT) {
				return "Passive";
			} else {
				return "Other";
			}
		}

		@Override
		public String getDisplayIdentifier(String identifier) {
			String i18nKey = EntityType.get(identifier).get().getTranslationKey();
			if (I18n.hasTranslation(i18nKey)) {
				return I18n.translate(i18nKey);
			} else {
				// We want to be clear that there is no result, rather than returning
				// the key (the default for failed formatting)
				// Since MC-68446 has been fixed, this shouldn't be hit normally,
				// but it's best to still be careful.
				return null;
			}
		}

		@Override
		public String getDisplayGroup(String group) {
			// TODO
			return null;
		}

		@Override
		public boolean enabledByDefault(String identifier) {
			return !DANGEROUS_ENTITIES.contains(identifier);
		}

		// Not intended to be used as a regular extension, so don't worry about
		// these methods
		@Override
		public boolean isValidEnvironment(String version) {
			return true;
		}
		@Override
		public String getEnvironmentErrorMessage(String version) {
			return null;
		}
	};

	private static final Logger LOGGER = LogManager.getLogger();

	// XXX whatever happens with forge, this doesn't handle it yet

	/**
	 * As returned by {@link #getProvidedEntities()}
	 */
	private static final Set<String> PROVIDED_ENTITIES;
	static {
		try {
			PROVIDED_ENTITIES = Registry.ENTITY_TYPE.stream()
					.filter(EntityType::isSaveable)
					.filter(EntityType::isSummonable)
					.map(EntityType::getTranslationKey)
					.collect(ImmutableSet.toImmutableSet());
		} catch (Throwable ex) {
			LOGGER.error("[WDL] Failed to load entity list: ", ex);
			throw new RuntimeException(ex);
		}
	}
}
