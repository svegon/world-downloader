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
package net.world.downloader.handler.entity;

import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.world.downloader.handler.HandlerException;

public class HopperMinecartHandler extends EntityHandler<HopperMinecartEntity, HopperScreenHandler> {

	public HopperMinecartHandler() {
		super(HopperMinecartEntity.class, HopperScreenHandler.class);
	}

	@Override
	public Text copyData(HopperScreenHandler container, HopperMinecartEntity minecart,
			boolean riding) throws HandlerException {
		for (int i = 0; i < minecart.size(); i++) {
			Slot slot = container.getSlot(i);
			
			if (slot.hasStack()) {
				minecart.setStack(i, slot.getStack());
			}
		}

		return new TranslatableText("wdl.messages.onGuiClosedInfo.savedEntity.hopperMinecart");
	}

}
