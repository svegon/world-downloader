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
package net.world.downloader.gui.widget;

import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;

/**
 * A slider that doesn't require a bunch of interfaces to work.
 *
 * Based off of {@link net.minecraft.client.gui.GuiOptionSlider}.
 */
public class GuiSlider extends ExtButton {
	private float sliderValue;
	/**
	 * I18n key for this slider.
	 */
	private final String text;
	/**
	 * Maximum value for the slider.
	 */
	private final int max;

	public GuiSlider(int x, int y, int width, int height, String text, int value, int max) {
		super(x, y, width, height, new LiteralText(text));

		this.text = text;
		this.max = max;

		setValue(value);
	}

	/**
	 * Returns 0 if the button is disabled, 1 if the mouse is NOT hovering over
	 * this button and 2 if it IS hovering over this button.
	 */
	@Override
	protected int getYImage(boolean mouseOver) {
		return 0;
	}

	@Override
	public void mouseDown(int mouseX, int mouseY) {
		this.sliderValue = (float)(mouseX - (this.x + 4))
				/ (float)(this.width - 8);
		this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F,
				1.0F);
		setMessage(I18n.translate(text, getValue()));
	}

	@Override
	public void mouseUp(int mouseX, int mouseY) { }

	@Override
	public void mouseDragged(int mouseX, int mouseY) {
		this.sliderValue = (float)(mouseX - (this.x + 4))
				/ (float)(this.width - 8);
		this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F,
				1.0F);

		setMessage(I18n.translate(text, getValue()));
	}

	@Override
	public void beforeDraw() { }

	@Override
	public void midDraw() {
		MinecraftClient.getInstance().getTextureManager().bindTexture(BUTTON_TEXTURES);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		if (this.isEnabled()) {
			this.drawTexture(matrixStack, this.x + (int) (this.sliderValue * (this.width - 8)),
					this.y, 0, 66, 4, 20);
			this.drawTexture(matrixStack, this.x + (int) (this.sliderValue * (this.width - 8))
					+ 4, this.y, 196, 66, 4, 20);
		} else {
			this.drawTexture(matrixStack, this.x + (int) (this.sliderValue * (this.width - 8)),
					this.y, 0, 46, 4, 20);
			this.drawTexture(matrixStack, this.x + (int) (this.sliderValue * (this.width - 8))
					+ 4, this.y, 196, 46, 4, 20);
		}
	}

	@Override
	public void afterDraw() { }

	/**
	 * Gets the current value of the slider.
	 * @return
	 */
	public int getValue() {
		return (int)(sliderValue * max);
	}

	/**
	 * Gets the current value of the slider.
	 * @return
	 */
	public void setValue(int value) {
		this.sliderValue = value / (float)max;

		setMessage(I18n.translate(text, getValue()));
	}

	@Override
	public List<? extends Element> children() {
		return null;
	}

	@Override
	public boolean isDragging() {
		return false;
	}

	@Override
	public void setDragging(boolean dragging) {
	}

	@Override
	public Element getFocused() {
		return null;
	}

	@Override
	public void setFocused(Element focused) {
	}
}