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

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.world.downloader.handler.HandlerException;
import net.world.downloader.utils.ReflectionUtils;
import net.world.downloader.utils.VersionedFunctions;

public class FurnaceHandler extends BlockHandler<FurnaceBlockEntity, FurnaceScreenHandler> {
	public FurnaceHandler() {
		super(FurnaceBlockEntity.class, FurnaceScreenHandler.class, "container.furnace");
	}

	@Override
	public Text handle(BlockPos clickedPos, FurnaceScreenHandler container, FurnaceBlockEntity blockEntity,
			BlockView world, BiConsumer<BlockPos, FurnaceBlockEntity> saveMethod) throws HandlerException {
		Inventory furnaceInventory = ReflectionUtils.findAndGetPrivateField(
				container, Inventory.class);
		String title = getCustomDisplayName(furnaceInventory);
		
		saveContainerItems(container, blockEntity, 0);
		saveInventoryFields(container, blockEntity);
		
		if (title != null) {
			blockEntity.setCustomName(VersionedFunctions.customName(title));
		}
		
		saveMethod.accept(clickedPos, blockEntity);
		return new TranslatableText("wdl.messages.onGuiClosedInfo.savedTileEntity.furnace");
	}
}
