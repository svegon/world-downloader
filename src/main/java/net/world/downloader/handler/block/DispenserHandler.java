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

import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.world.downloader.handler.HandlerException;
import net.world.downloader.utils.ReflectionUtils;
import net.world.downloader.utils.VersionedFunctions;

public class DispenserHandler extends BlockHandler<DispenserBlockEntity,
		Generic3x3ContainerScreenHandler> {
	public DispenserHandler() {
		super(DispenserBlockEntity.class, Generic3x3ContainerScreenHandler.class, "container.dispenser");
	}

	@Override
	public Text handle(BlockPos clickedPos, Generic3x3ContainerScreenHandler container,
			DispenserBlockEntity blockEntity, BlockView world,
			BiConsumer<BlockPos, DispenserBlockEntity> saveMethod) throws HandlerException {
		Inventory dispenserInventory = ReflectionUtils.findAndGetPrivateField(
				container, Inventory.class);
		String title = getCustomDisplayName(dispenserInventory);
		
		saveContainerItems(container, blockEntity, 0);
		saveMethod.accept(clickedPos, blockEntity);
		
		if (title != null) {
			blockEntity.setCustomName(VersionedFunctions.customName(title));
		}
		
		return new TranslatableText("wdl.messages.onGuiClosedInfo.savedTileEntity.dispenser");
	}
}
