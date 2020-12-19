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


import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.serialization.Dynamic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanRule;
import net.world.downloader.utils.VersionedFunctions.GameRuleType;

/**
 * Contains functions related to gamerules. This version of the class is used
 * for Minecraft 1.14.3.
 */
public final class GameRuleFunctions {
	private GameRuleFunctions() { throw new AssertionError(); }
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ServerCommandSource DUMMY_COMMAND_SOURCE =
			new ServerCommandSource(CommandOutput.DUMMY, Vec3d.ZERO, Vec2f.ZERO, null, 0,
					"", new LiteralText(""), null, null);

	private static class RuleInfo<T extends GameRules.Rule<T>> {
		public final GameRules.Key<T> key;
		public final GameRules.Type<T> type;
		@Nullable
		public final GameRuleType wdlType;
		private final CommandNode<ServerCommandSource> commandNode;
		
		public RuleInfo(GameRules.Key<T> key, GameRules.Type<T> type) {
			this.key = key;
			this.type = type;
			this.commandNode = this.type.argument("value").build();
			T defaultValue = type.createRule();
			if (defaultValue instanceof BooleanRule) {
				this.wdlType = GameRuleType.BOOLEAN;
			} else if (defaultValue instanceof GameRules.IntRule) {
				this.wdlType = GameRuleType.INTEGER;
			} else {
				LOGGER.warn("[WDL] Unknown gamerule type {} for {}, default value {} ({})", type,
						key, defaultValue, (defaultValue != null ? defaultValue.getClass() : "null"));
				this.wdlType = null;
			}
		}

		// I'm not particularly happy about the lack of a public generalized string set method...
		public void set(GameRules rules, String value) {
			try {
				CommandContextBuilder<ServerCommandSource> ctxBuilder =
						new CommandContextBuilder<ServerCommandSource>(null,
								DUMMY_COMMAND_SOURCE, null, 0);
				StringReader reader = new StringReader(value);
				this.commandNode.parse(reader, ctxBuilder);
				rules.get(this.key).set(ctxBuilder.build(value), "value");
			} catch (Exception ex) {
				LOGGER.error("[WDL] Failed to set rule {} to {}", key, value, ex);
				throw new IllegalArgumentException("Failed to set rule " + key + " to " + value, ex);
			}
		}
	}
	private static final Map<String, RuleInfo<?>> RULES;
	static {
		RULES = new TreeMap<>();
		GameRules.accept(new GameRules.Visitor() {
			@Override
			public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key,
					GameRules.Type<T> type) {
				RULES.put(key.getName(), new RuleInfo<>(key, type));
			}
		});
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getRuleType
	 */
	@Nullable
	public static GameRuleType getRuleType(GameRules rules, String rule) {
		if (RULES.containsKey(rule)) {
			return RULES.get(rule).wdlType;
		} else {
			return null;
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getRuleValue
	 */
	@Nullable
	public static String getRuleValue(GameRules rules, String rule) { 
		if (RULES.containsKey(rule)) {
			return rules.get(RULES.get(rule).key).toString();
		} else {
			return null;
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#setRuleValue
	 */
	public static void setRuleValue(GameRules rules, String rule, String value) {
		if (!RULES.containsKey(rule)) {
			throw new IllegalArgumentException("No rule named " + rule + " exists in " + rules + " (setting to " + value + ", rules list is " + getGameRules(rules) + ")");
		} else {
			RULES.get(rule).set(rules, value);
		}
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getGameRules
	 */
	public static Map<String, String> getGameRules(GameRules rules) {
		Map<String, String> result = RULES
				.keySet().stream()
				.collect(Collectors.toMap(
						rule -> rule,
						rule -> getRuleValue(rules, rule),
						(a, b) -> {throw new IllegalArgumentException("Mutliple rules with the same name!  " + a + "," + b);},
						TreeMap::new));
		return Collections.unmodifiableMap(result);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#loadGameRules
	 */
	public static GameRules loadGameRules(CompoundTag tag) {
		return new GameRules(new Dynamic<>(NbtOps.INSTANCE, tag));
	}
}
