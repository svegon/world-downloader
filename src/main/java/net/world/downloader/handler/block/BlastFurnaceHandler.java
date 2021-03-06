/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2019 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader.handler.block;

import net.minecraft.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.screen.BlastFurnaceScreenHandler;
import net.minecraft.text.TranslatableText;

public class BlastFurnaceHandler extends BaseFurnaceHandler<BlastFurnaceBlockEntity,
		BlastFurnaceScreenHandler> {
	public BlastFurnaceHandler() {
		super(BlastFurnaceBlockEntity.class, BlastFurnaceScreenHandler.class, "container.blast_furnace");
	}

	@Override
	protected TranslatableText getMessage() {
		return new TranslatableText("wdl.messages.onGuiClosedInfo.savedTileEntity.blastFurnace");
	}
}
