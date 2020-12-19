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

import io.netty.buffer.Unpooled;
import net.minecraft.network.MessageType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.world.downloader.utils.VersionedFunctions.ChannelName;

/**
 * Contains functions related to packets. This version is used in Minecraft 1.13
 * and newer.
 */
public final class PacketFunctions {
	private PacketFunctions() {
		throw new AssertionError();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#CHANNEL_NAME_REGEX
	 */
	public static final String CHANNEL_NAME_REGEX = "([a-z0-9_.-]+:)?[a-z0-9/._-]+";

	/* (non-javadoc)
	 * @see VersionedFunctions#makePluginMessagePacket
	 */
	public static CustomPayloadC2SPacket makePluginMessagePacket(@ChannelName String channel,
			byte[] bytes) {
		return new CustomPayloadC2SPacket(new Identifier(channel),
				new PacketByteBuf(Unpooled.copiedBuffer(bytes)));
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#makeServerPluginMessagePacket
	 */
	public static CustomPayloadS2CPacket makeServerPluginMessagePacket(@ChannelName String channel,
			byte[] bytes) {
		return new CustomPayloadS2CPacket(new Identifier(channel),
				new PacketByteBuf(Unpooled.copiedBuffer(bytes)));
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#makeChatPacket
	 */
	public static GameMessageS2CPacket makeChatPacket(String message) {
		return new GameMessageS2CPacket(new LiteralText(message), MessageType.SYSTEM, Util.NIL_UUID);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getRegisterChannel
	 */
	@ChannelName
	public static String getRegisterChannel() {
		return "minecraft:register";
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getUnregisterChannel
	 */
	@ChannelName
	static String getUnregisterChannel() {
		return "minecraft:unregister";
	}
}
