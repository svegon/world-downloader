/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Extendible text field, to deal with changing constructors between versions.
 * The actual implementation is {@link WDLTextField}.
 */
abstract class ExtTextField extends TextFieldWidget {
	/**
	 * Since {@link GuiTextField#width} and {@link GuiTextField#height} are private,
	 * the same info is provided here.
	 */
	private final int width, height;
	/**
	 * Similarly, these coordinates are needed for {@link #isHovered()}.
	 */
	private int mouseX, mouseY;

	public ExtTextField(TextRenderer fontRenderer, int x, int y, int width, int height, Text label) {
		super(fontRenderer, x, y, width, height, label);
		this.width = width;
		this.height = height;
		// Label parameter is unused, but provided for narrator purposes in later versions
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		super.render(matrices, mouseX, mouseY, partialTicks);
	}

	// This method is in ExtTextField since 1.14+ have it in Widget, so it doesn't
	// need to be implemented in 1.14's WDLTextField.
	public boolean isHovered() {
		if (!this.isVisible()) {
			return false;
		}

		int scaledX = this.mouseX - this.x;
		int scaledY = this.mouseY - this.y;

		return scaledX >= 0 && scaledX < this.width && scaledY >= 0 && scaledY < this.height;
	}
}
