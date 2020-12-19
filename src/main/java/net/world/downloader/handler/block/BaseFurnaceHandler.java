/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2019 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader.handler.block;

import java.util.function.BiConsumer;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.world.downloader.handler.HandlerException;
import net.world.downloader.utils.VersionedFunctions;

public abstract class BaseFurnaceHandler<B extends AbstractFurnaceBlockEntity,
		C extends AbstractFurnaceScreenHandler> extends BlockHandler<B, C> {
	protected BaseFurnaceHandler(Class<B> blockEntityClass, Class<C> containerClass, String defaultName) {
		super(blockEntityClass, containerClass, defaultName);
	}

	@Override
	public Text handle(BlockPos clickedPos, C container, B blockEntity, BlockView world,
			BiConsumer<BlockPos, B> saveMethod) throws HandlerException {
		Inventory furnaceInventory = blockEntity;
		String title = getCustomDisplayName(furnaceInventory);
		saveContainerItems(container, blockEntity, 0);
		saveInventoryFields(containerClass.cast(container), blockEntityClass.cast(blockEntity));
		
		if (title != null) {
			blockEntity.setCustomName(VersionedFunctions.customName(title));
		}
		
		saveMethod.accept(clickedPos, blockEntity);
		return getMessage();
	}

	protected abstract TranslatableText getMessage();
}
