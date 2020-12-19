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

import java.util.function.BiConsumer;

import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.world.downloader.handler.HandlerException;

public class LecternHandler extends BlockHandler<LecternBlockEntity, LecternScreenHandler> {

	public LecternHandler() {
		super(LecternBlockEntity.class, LecternScreenHandler.class, "container.lectern");
	}

	@Override
	public Text handle(BlockPos clickedPos, LecternScreenHandler container, LecternBlockEntity blockEntity,
			BlockView world, BiConsumer<BlockPos, LecternBlockEntity> saveMethod) throws HandlerException {
		blockEntity.setBook(container.getBookItem());
		saveInventoryFields(container, blockEntity); // current page
		// NOTE: Cannot be renamed
		saveMethod.accept(clickedPos, blockEntity);
		return new TranslatableText("wdl.messages.onGuiClosedInfo.savedTileEntity.lectern");
	}

}
