/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2018 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader.handler.block;

import java.util.function.BiConsumer;

import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.world.downloader.handler.HandlerException;

public class BeaconHandler extends BlockHandler<BeaconBlockEntity, BeaconScreenHandler> {
	public BeaconHandler() {
		super(BeaconBlockEntity.class, BeaconScreenHandler.class, "container.beacon");
	}

	@Override
	public Text handle(BlockPos clickedPos, BeaconScreenHandler container, BeaconBlockEntity blockEntity,
			BlockView world, BiConsumer<BlockPos, BeaconBlockEntity> saveMethod) throws HandlerException {
		// NOTE: beacons do not have custom names, see https://bugs.mojang.com/browse/MC-124395
		saveContainerItems(container, blockEntity, 0);
		saveInventoryFields(container, blockEntity);
		saveMethod.accept(clickedPos, blockEntity);
		return new TranslatableText("wdl.messages.onGuiClosedInfo.savedTileEntity.beacon");
	}
}
