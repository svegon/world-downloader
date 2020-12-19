/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.MinecraftClientforum.net/forums/mapping-and-modding-java-edition/MinecraftClient-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2018-2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader.gui.screens;


import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.world.downloader.api.IExtGuiScreen;
import net.world.downloader.gui.GuiList;
import net.world.downloader.gui.GuiList.GuiListEntry;
import net.world.downloader.utils.GuiUtils;
import net.world.downloader.utils.ReflectionUtils;
import net.world.downloader.utils.VersionedFunctions;

public abstract class ExtGuiScreen extends Screen implements IExtGuiScreen {
	/**
	 * Hide the buttonList field from subclasses.
	 * @deprecated Do not use; use {@link #addButton} instead.
	 */
	@Deprecated
	protected static final Void buttonList = null;

	private final List<GuiList<?>> listList = new ArrayList<>();
	private final List<TextFieldWidget> textFieldList = new ArrayList<>();
	@Nullable
	protected MatrixStack matrixStack = null;
	// Why is this unused? -Svegon
	protected CharacterVisitor characterManager;

	protected ExtGuiScreen(Text title) {
		super(title);
	}

	// Called before init
	@Override
	public final void init(MinecraftClient mc, int width, int height) {
		this.listList.clear();
		this.textFieldList.clear();
		this.characterManager = ReflectionUtils.findAndGetPrivateField(mc.textRenderer,
				CharacterVisitor.class);
		super.init(mc, width, height);
	}

	@Override
	public final <T extends AbstractButtonWidget> T addButton(T buttonIn) {
		return super.addButton(buttonIn);
	}

	@Override
	public final <T extends GuiList<E>, E extends GuiListEntry<E>> T addList(T list) {
		this.listList.add(list);
		this.children.add(list);
		return list;
	}

	@Override
	public final <T extends TextFieldWidget> T addTextField(T field) {
		this.textFieldList.add(field);
		this.children.add(field);
		return field;
	}

	@Override
	public final boolean shouldCloseOnEsc() {
		return this.onCloseAttempt();
	}

	@Override
	public final boolean mouseClicked(double mouseX, double mouseY, int state) {
		boolean result = super.mouseClicked(mouseX, mouseY, state);
		if (state == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			this.mouseDown((int)mouseX, (int)mouseY);
		}
		return result;
	}

	@Override
	public final boolean mouseReleased(double mouseX, double mouseY, int state) {
		boolean result = super.mouseReleased(mouseX, mouseY, state);
		if (state == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			this.mouseUp((int)mouseX, (int)mouseY);
		}
		return result;
	}

	@Override
	public final boolean mouseDragged(double mouseX, double mouseY, int state, double dx, double dy) {
		boolean result = super.mouseDragged(mouseX, mouseY, state, dx, dy);
		if (state == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			this.mouseDragged((int)mouseX, (int)mouseY);
		}
		return result;
	}

	@Override
	public final boolean keyPressed(int key, int scanCode, int modifiers) {
		boolean result = super.keyPressed(key, scanCode, modifiers);
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			this.charTyped('\n');
		}
		this.anyKeyPressed();
		return result;
	}

	@Override
	public final boolean charTyped(char c, int modifiers) {
		boolean result = super.charTyped(c, modifiers);
		this.charTyped(c);
		this.anyKeyPressed();
		return result;
	}

	@Override
	// must invoke super.tick()V
	public void tick() {
		super.tick();
		
		for (GuiList<?> list : this.listList) {
			list.tick();
		}
		
		for (TextFieldWidget field : this.textFieldList) {
			field.tick();
		}
	}

	@Override
	public final void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.matrixStack = matrixStack;
		this.render(mouseX, mouseY, partialTicks);
		this.matrixStack = null;
	}

	public void renderBackground() {
		super.renderBackground(matrixStack);
	}

	public void renderBackground(int p_238651_2_) {
		super.renderBackground(matrixStack, p_238651_2_);
	}

	@Override
	// must invoke super.render(IIF)V
	public void render(int mouseX, int mouseY, float partialTicks) {
		for (GuiList<?> list : this.listList) {
			list.render(matrixStack, mouseX, mouseY, partialTicks);
		}
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		for (TextFieldWidget field : this.textFieldList) {
			field.render(matrixStack, mouseX, mouseY, partialTicks);
		}
		this.renderTitle(mouseX, mouseY, partialTicks);
	}

	protected void renderTitle(int mouseX, int mouseY, float partialTicks) {
		super.drawCenteredText(matrixStack, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
	}

	public void hLine(int startX, int endX, int y, int color) {
		super.drawHorizontalLine(matrixStack, startX, endX, y, color);
	}
	
	public void vLine(int x, int startY, int endY, int color) {
		super.drawVerticalLine(matrixStack, x, startY, endY, color);
	}
	
	public void fill(int left, int top, int right, int bottom, int color) {
		super.fill(matrixStack, left, top, right, bottom, color);
	}
	
	public void drawCenteredString(TextRenderer font, String str, int x, int y, int color) {
		super.drawCenteredString(matrixStack, font, str, x, y, color);
	}
	
	public void drawRightAlignedString(TextRenderer font, String str, int x, int y, int color) {
		font.drawWithShadow(matrixStack, str, x - font.getWidth(new LiteralText(str).asOrderedText()),
				y, color, true);
	}
	
	public void drawString(TextRenderer font, String str, int x, int y, int color) {
		super.drawStringWithShadow(matrixStack, font, str, x, y, color);
	}
	
	public void blit(int x, int y, int textureX, int textureY, int width, int height) {
		super.drawTexture(matrixStack, x, y, textureX, textureY, width, height);
	}

	/**
	 * Draws a semitransparent description box.
	 *
	 * @param text
	 *            Text to display. Takes \n into consideration.
	 * @param guiWidth
	 *            Width of the GUI.
	 * @param guiHeight
	 *            Height of the GUI.
	 * @param bottomPadding
	 *            The amount of space to put below the bottom of the info box.
	 */
	public void drawGuiInfoBox(Text text, int guiWidth, int guiHeight,
			int bottomPadding) {
		drawGuiInfoBox(text, 300, 100, guiWidth, guiHeight, bottomPadding);
	}

	/**
	 * Draws a semitransparent description box.
	 *
	 * @param text
	 *            Text to display. Takes \n into consideration.
	 * @param infoBoxWidth
	 *            The width of the info box.
	 * @param infoBoxHeight
	 *            The height of the info box.
	 * @param guiWidth
	 *            Width of the GUI.
	 * @param guiHeight
	 *            Height of the GUI.
	 * @param bottomPadding
	 *            The amount of space to put below the bottom of the info box.
	 */
	public void drawGuiInfoBox(Text text, int infoBoxWidth,
			int infoBoxHeight, int guiWidth, int guiHeight, int bottomPadding) {
		if (text == null) {
			return;
		}

		int infoX = guiWidth / 2 - infoBoxWidth / 2;
		int infoY = guiHeight - bottomPadding - infoBoxHeight;
		int y = infoY + 5;

		fill(infoX, infoY, infoX + infoBoxWidth, infoY
				+ infoBoxHeight, 0x7F000000);

		List<String> lines = wordWrap(text.getString(), infoBoxWidth - 10);

		for (String s : lines) {
			drawString(textRenderer, s, infoX + 5, y, 0xFFFFFF);
			y += textRenderer.fontHeight;
		}
	}

	/**
	 * Converts a string into a list of lines that are each shorter than the
	 * given width.  Takes \n into consideration.
	 *
	 * @param s The string to word wrap.
	 * @param width The width to use.
	 * @return A list of lines.
	 */
	public List<String> wordWrap(String s, int width) {
		return GuiUtils.wordWrap(s, width);
	}

	/**
	 * Draws the background/border used by list GUIs.
	 * <br/>
	 * Based off of
	 * {@link net.MinecraftClient.client.gui.GuiSlot#drawScreen(int, int, float)}.
	 *
	 * Note that there is an additional 4-pixel padding on the margins for the gradient.
	 *
	 * @param topMargin Amount of space to give for the upper box.
	 * @param bottomMargin Amount of space to give for the lower box.
	 * @param top Where to start drawing (usually, 0)
	 * @param left Where to start drawing (usually, 0)
	 * @param bottom Where to stop drawing (usually, height).
	 * @param right Where to stop drawing (usually, width)
	 */
	public void drawListBackground(int topMargin, int bottomMargin, int top, int left, int bottom,
			int right) {
		VersionedFunctions.drawDarkBackground(top, left, bottom, right);
		drawBorder(topMargin, bottomMargin, top, left, bottom, right);
	}

	/**
	 * Draws the top and bottom borders found on gui lists (but no background).
	 * <br/>
	 * Based off of
	 * {@link net.MinecraftClient.client.gui.GuiSlot#overlayBackground(int, int, int, int)}.
	 *
	 * Note that there is an additional 4-pixel padding on the margins for the gradient.
	 *
	 * @param topMargin Amount of space to give for the upper box.
	 * @param bottomMargin Amount of space to give for the lower box.
	 * @param top Where to start drawing (usually, 0)
	 * @param left Where to start drawing (usually, 0)
	 * @param bottom Where to stop drawing (usually, height).
	 * @param right Where to stop drawing (usually, width)
	 */
	public void drawBorder(int topMargin, int bottomMargin, int top, int left, int bottom, int right) {
		VersionedFunctions.drawBorder(topMargin, bottomMargin, top, left, bottom, right);
	}
}
