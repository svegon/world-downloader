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
package net.world.downloader.gui.widget;

import java.util.List;

import net.minecraft.client.gui.Element;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/**
 * A Button class that works across versions.
 */
public abstract class WDLButton extends ExtButton {
	private boolean dragging;
	private Element focus;
	
	public WDLButton(int x, int y, int widthIn, int heightIn, String buttonText) {
		this(x, y, widthIn, heightIn, new LiteralText(buttonText), (b) -> {}, EMPTY);
	}
	
	public WDLButton(int x, int y, int widthIn, int heightIn, Text buttonText) {
		this(x, y, widthIn, heightIn, buttonText, (b) -> {}, EMPTY);
	}
	
	public WDLButton(int x, int y, int widthIn, int heightIn, Text buttonText, PressAction onPress,
			TooltipSupplier tooltipSupplier) {
		super(x, y, widthIn, heightIn, buttonText, onPress, tooltipSupplier);
	}

	/**
	 * Performs the action of this button when it has been clicked.
	 */
	public abstract void performAction();

	@Override
	public void beforeDraw() { }

	@Override
	public void midDraw() { }

	@Override
	public void afterDraw() { }

	@Override
	public void mouseDown(int mouseX, int mouseY) {
		this.performAction();
	}

	@Override
	public void mouseDragged(int mouseX, int mouseY) { }

	@Override
	public void mouseUp(int mouseX, int mouseY) { }

	@Override
	public List<? extends Element> children() {
		return null;
	}

	@Override
	public boolean isDragging() {
		return this.dragging;
	}

	@Override
	public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	@Override
	public Element getFocused() {
		return this.focus;
	}

	@Override
	public void setFocused(Element focused) {
		this.focus = focused;
	}
}
