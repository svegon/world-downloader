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

import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.world.downloader.handler.HandlerException;
import net.world.downloader.utils.ReflectionUtils;

public class HorseHandler extends EntityHandler<HorseBaseEntity, HorseScreenHandler> {
	public HorseHandler() {
		super(HorseBaseEntity.class, HorseScreenHandler.class);
	}

	@Override
	public boolean checkRiding(HorseScreenHandler container, HorseBaseEntity riddenHorse) {
		HorseBaseEntity horseInContainer = ReflectionUtils
				.findAndGetPrivateField(container, HorseBaseEntity.class);

		// Intentional reference equals
		return horseInContainer == riddenHorse;
	}

	@Override
	public Text copyData(HorseScreenHandler container, HorseBaseEntity horse, boolean riding)
			throws HandlerException {
		Inventory horseInventory = ReflectionUtils.findAndGetPrivateField(container, Inventory.class);

		for (int i = 0; i < horseInventory.size(); i++) {
			Slot slot = container.getSlot(i);
			
			if (slot.hasStack()) {
				horseInventory.setStack(i, slot.getStack());
			}
		}

		ReflectionUtils.findAndSetPrivateField(horse, HorseBaseEntity.class, Inventory.class,
				horseInventory);

		if (riding) {
			return new TranslatableText("wdl.messages.onGuiClosedInfo.savedRiddenEntity.horse");
		} else {
			return new TranslatableText("wdl.messages.onGuiClosedInfo.savedEntity.horse");
		}
	}

}
