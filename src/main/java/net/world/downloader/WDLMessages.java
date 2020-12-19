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
package net.world.downloader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.world.downloader.api.IConfiguration;
import net.world.downloader.api.IWDLMessageType;
import net.world.downloader.config.CyclableSetting;
import net.world.downloader.config.settings.MessageSettings;
import net.world.downloader.utils.EntityUtils;

/**
 * Responsible for displaying messages in chat or the log, depending on whether
 * they are enabled.
 */
public class WDLMessages {
	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Information about an individual message type.
	 */
	public static class MessageRegistration {
		public final String name;
		public final IWDLMessageType type;
		public final MessageTypeCategory category;
		public final CyclableSetting<Boolean> setting;

		/**
		 * Creates a MessageRegistration.
		 *
		 * @param name The name to use.
		 * @param type The type bound to this registration.
		 * @param category The category.
		 */
		public MessageRegistration(String name, IWDLMessageType type,
				MessageTypeCategory category) {
			this.name = name;
			this.type = type;
			this.category = category;
			this.setting = new MessageSettings.MessageTypeSetting(this);
		}

		@Override
		public String toString() {
			return "MessageRegistration [name=" + name + ", type=" + type
					+ ", category=" + category + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((category == null) ? 0 : category.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			
			if (obj == null) {
				return false;
			}
			
			if (!(obj instanceof MessageRegistration)) {
				return false;
			}
			
			MessageRegistration other = (MessageRegistration) obj;
			
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			
			if (category == null) {
				if (other.category != null) {
					return false;
				}
			} else if (!category.equals(other.category)) {
				return false;
			}
			
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			
			return true;
		}
	}

	/**
	 * List of all registrations, by category.
	 */
	private static ListMultimap<MessageTypeCategory, MessageRegistration> registrations =
			LinkedListMultimap.create();

	/**
	 * Gets the {@link MessageRegistration} for the given name.
	 * @param name The name to look for
	 * @return The registration
	 * @throws IllegalArgumentException for unknown names
	 */
	@Nonnull
	public static MessageRegistration getRegistration(String name) {
		for (MessageRegistration r : registrations.values()) {
			if (r.name.equals(name)) {
				return r;
			}
		}
		
		throw new IllegalArgumentException("Asked for the registration for " + name
				+ ", but there is no registration for that!");
	}

	/**
	 * Gets the {@link MessageRegistration} for the given {@link IWDLMessageType}.
	 * @param type The type to look for
	 * @return The registration
	 * @throws IllegalArgumentException for unknown names
	 */
	@Nonnull
	public static MessageRegistration getRegistration(IWDLMessageType type) {
		for (MessageRegistration r : registrations.values()) {
			if (r.type.equals(type)) {
				return r;
			}
		}
		
		throw new IllegalArgumentException("Asked for the registration for " + type
				+ ", but there is no registration for that!");
	}

	/**
	 * Adds registration for another type of message.
	 *
	 * @param name The programmatic name.
	 * @param type The type.
	 * @param category The category.
	 */
	public static void registerMessage(String name, IWDLMessageType type, MessageTypeCategory category) {
		registrations.put(category, new MessageRegistration(name, type, category));
	}

	/**
	 * Gets all of the MessageTypes
	 * @return All the types, ordered by the category.
	 */
	@Nonnull
	public static ListMultimap<MessageTypeCategory, MessageRegistration> getRegistrations() {
		return ImmutableListMultimap.copyOf(registrations);
	}

	/**
	 * Prints the given message into the chat.
	 *
	 * @param config Configuration to use to check if a message is enabled
	 * @param type The type of the message.
	 * @param message The message to display.
	 */
	public static void chatMessage(@Nonnull IConfiguration config,
			@Nonnull IWDLMessageType type, @Nonnull String message) {
		chatMessage(config, type, new LiteralText(message));
	}

	/**
	 * Prints a translated chat message into the chat.
	 *
	 * @param config
	 *            Configuration to use to check if a message is enabled
	 * @param type
	 *            The type of the message.
	 * @param translationKey
	 *            I18n key that is translated.
	 * @param args
	 *            The arguments to pass to the {@link TranslatableText}.
	 *            A limited amount of processing is performed: {@link Entity}s
	 *            will be converted properly with a tooltip like the one
	 *            generated by {@link Entity#getDisplayName()}.
	 */
	public static void chatMessageTranslated(@Nonnull IConfiguration config,
			@Nonnull IWDLMessageType type, @Nonnull String translationKey, @Nonnull Object... args) {
		List<Throwable> exceptionsToPrint = new ArrayList<>();

		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				LiteralText text = new LiteralText("null");
				text.setStyle(text.getStyle().withHoverEvent(new HoverEvent(Action.SHOW_TEXT, new LiteralText("~~null~~"))));
				args[i] = text;
			} else if (args[i] instanceof Entity) {
				Entity e = (Entity)args[i];

				args[i] = convertEntityToComponent(e);
			} else if (args[i] instanceof Throwable) {
				Throwable t = (Throwable) args[i];

				args[i] = convertThrowableToComponent(t);
				exceptionsToPrint.add(t);
			} else if (args[i] instanceof BlockPos) {
				// Manually toString BlockPos instances to deal with obfuscation
				BlockPos pos = (BlockPos) args[i];
				args[i] = String.format("Pos[x=%d, y=%d, z=%d]", pos.getX(), pos.getY(), pos.getZ());
			}
		}

		final MutableText component;
		
		if (I18n.hasTranslation(translationKey)) {
			component = new TranslatableText(translationKey, args);
		} else {
			// Oh boy, no translation text.  Manually apply parameters.
			String message = translationKey;
			component = new LiteralText(message);
			component.append("[");
			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Text) {
					component.append((Text) args[i]);
				} else {
					component.append(String.valueOf(args[i]));
				}
				if (i != args.length - 1) {
					component.append(", ");
				}
			}
			component.append("]");
		}

		chatMessage(config, type, component);

		for (int i = 0; i < exceptionsToPrint.size(); i++) {
			LOGGER.warn("Exception #" + (i + 1) + ": ", exceptionsToPrint.get(i));
		}
	}

	/**
	 * Prints the given message into the chat.
	 *
	 * @param config Configuration to use to check if a message is enabled
	 * @param type The type of the message.
	 * @param message The message to display.
	 */
	public static void chatMessage(@Nonnull IConfiguration config,
			@Nonnull IWDLMessageType type, @Nonnull Text message) {
		boolean enabled;
		
		try {
			MessageRegistration registration = getRegistration(type);
			enabled = config.getValue(registration.setting);
		} catch (Exception ex) {
			enabled = false;
			LOGGER.error("Failed to check if type was enabled: " + type, ex);
		}

		// Can't use a TranslatableText here because it doesn't like new lines.
		String tooltipText = I18n.translate("wdl.messages.tooltip",
				type.getDisplayName().getString()).replace("\\n", "\n"); // XXX should be formatted
		Text tooltip = new LiteralText(tooltipText);

		MutableText text = new LiteralText("");

		LiteralText header = new LiteralText("[WorldDL]");
		header.setStyle(header.getStyle().withColor(type.getTitleColor())
				.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, tooltip)));

		// If the message has its own style, it'll use that instead.
		// TODO: Better way?
		LiteralText messageFormat = new LiteralText(" ");
		messageFormat.setStyle(messageFormat.getStyle().withColor(type.getTextColor()));

		messageFormat.append(message);
		text.append(header);
		text.append(messageFormat);
		
		if (enabled) {
			MinecraftClient minecraft = MinecraftClient.getInstance();
			// Cross-thread calls to printChatMessage are illegal in 1.13 due to accessing
			// the font renderer; add a scheduled task instead.
			minecraft.execute(() -> minecraft.inGameHud.getChatHud().addMessage(text));
		} else {
			LOGGER.info(text.getString());
		}
	}

	@Nonnull
	private static Text convertEntityToComponent(@Nonnull Entity e) {
		Text wdlName, displayName;

		try {
			String identifier = EntityUtils.getEntityType(e);
			
			if (identifier == null) {
				wdlName = new TranslatableText("wdl.messages.entityData.noKnownName");
			} else {
				String group = EntityUtils.getEntityGroup(identifier);
				String displayIdentifier = EntityUtils.getDisplayType(identifier);
				String displayGroup = EntityUtils.getDisplayGroup(group);
				wdlName = new LiteralText(displayIdentifier);

				MutableText hoverText = new LiteralText("");
				hoverText.append(new TranslatableText("wdl.messages.entityData.internalName",
						identifier));
				hoverText.append("\n");
				hoverText.append(new TranslatableText("wdl.messages.entityData.group",
						displayGroup, group));

				((MutableText)wdlName).setStyle(
						wdlName.getStyle().withHoverEvent(new HoverEvent(Action.SHOW_TEXT, hoverText)));
			}
		} catch (Exception ex) {
			LOGGER.warn("[WDL] Exception in entity name!", ex);
			wdlName = convertThrowableToComponent(ex);
		}
		
		try {
			displayName = e.getDisplayName();
		} catch (Exception ex) {
			LOGGER.warn("[WDL] Exception in entity display name!", ex);
			displayName = convertThrowableToComponent(ex);
		}

		return new TranslatableText("wdl.messages.entityData", wdlName, displayName);
	}

	@Nonnull
	private static Text convertThrowableToComponent(@Nonnull Throwable t) {
		LiteralText component = new LiteralText(t.toString());

		// https://stackoverflow.com/a/1149721/3991344
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();

		exceptionAsString = exceptionAsString.replace("\r", "")
				.replace("\t", "    ");

		HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new LiteralText(exceptionAsString));

		component.setStyle(component.getStyle().withHoverEvent(event));

		return component;
	}

	static {
		for (WDLMessageTypes type : WDLMessageTypes.values()) {
			registerMessage(type.name(), type, type.category);
		}
	}
}
