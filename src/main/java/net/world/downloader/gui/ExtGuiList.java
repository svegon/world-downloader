/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
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
package net.world.downloader.gui;


import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.world.downloader.api.IExtGuiList;
import net.world.downloader.utils.VersionedFunctions;

public abstract class ExtGuiList<T extends ExtGuiList.ExtGuiListEntry<T>> extends EntryListWidget<T>
		implements IExtGuiList<T> {
	private int y;
	@Nullable
	private MatrixStack matrixStack;

	public static abstract class ExtGuiListEntry<T extends ExtGuiListEntry<T>>
			extends EntryListWidget.Entry<T> implements IExtGuiListEntry<T> {
		private static class ButtonWrapper {
			public final ButtonWidget button;
			public final int x;
			public final int y;
			
			public ButtonWrapper(ButtonWidget button, int x, int y) {
				this.button = button;
				this.x = x;
				this.y = y;
			}
		}
		
		private static class TextFieldWrapper {
			public final TextFieldWidget field;
			public final int x;
			public final int y;
			public TextFieldWrapper(TextFieldWidget field, int x, int y) {
				this.field = field;
				this.x = x;
				this.y = y;
			}
		}

		private final List<ButtonWrapper> buttonList = new ArrayList<>();
		private final List<TextFieldWrapper> fieldList = new ArrayList<>();
		@Nullable
		private ButtonWrapper activeButton;
		@Nullable
		protected MatrixStack matrixStack;

		@Override
		public final <B extends ButtonWidget> B addButton(B button, int x, int y) {
			this.buttonList.add(new ButtonWrapper(button, x, y));
			return button;
		}

		@Override
		public final <B extends TextFieldWidget> B addTextField(B field, int x, int y) {
			this.fieldList.add(new TextFieldWrapper(field, x, y));
			return field;
		}

		@Override
		public final void render(MatrixStack matrixStack, int index, int y, int x, int entryWidth,
				int entryHeight, int mouseX, int mouseY, boolean mouseOver, float partialTicks) {
			this.matrixStack = matrixStack;
			// XXX: Note that y and x here are swapped!
			this.drawEntry(x, y, entryWidth, entryHeight, mouseX, mouseY);
			this.matrixStack = null;
			for (ButtonWrapper button : this.buttonList) {
				button.button.x = button.x + x + (entryWidth / 2);
				button.button.y = button.y + y;
				button.button.render(matrixStack, mouseX, mouseY, partialTicks);
			}
			for (TextFieldWrapper field : this.fieldList) {
				field.field.x = field.x + x + (entryWidth / 2);
				field.field.y = field.y + y;
				field.field.render(matrixStack, mouseX, mouseY, partialTicks);
			}
		}

		@Override
		public final boolean mouseClicked(double arg0, double arg1, int arg2) {
			boolean result = false;
			for (ButtonWrapper button : this.buttonList) {
				if (button.button.mouseClicked(arg0, arg1, arg2)) {
					this.activeButton = button;
					// no need to call playDownSound; mouseClicked does it automatically
					result = true;
				}
			}
			for (TextFieldWrapper field : this.fieldList) { 
				if (field.field.isVisible() && field.field.mouseClicked(arg0, arg1, arg2)) {
					result = true;
				}
			}
			result |= this.mouseDown((int)arg0, (int)arg1, arg2);
			return result;
		}

		@Override
		public final boolean mouseReleased(double arg0, double arg1, int arg2) {
			if (this.activeButton != null) {
				boolean result = this.activeButton.button.mouseReleased(arg0, arg1, arg2);
				this.activeButton = null;
				return result;
			}
			this.mouseUp((int)arg0, (int)arg1, arg2);
			return false;
		}

		@Override
		public final boolean mouseDragged(double arg0, double arg1, int arg2, double arg3, double arg4) {
			if (this.activeButton != null) {
				return this.activeButton.button.mouseDragged(arg0, arg1, arg2, arg3, arg4);
			}
			return false;
		}

		@Override
		public final boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
			for (TextFieldWrapper field : this.fieldList) {
				field.field.charTyped(p_charTyped_1_, p_charTyped_2_);
			}
			this.charTyped(p_charTyped_1_);
			this.anyKeyPressed();
			return false;
		}

		@Override
		public final boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
			for (TextFieldWrapper field : this.fieldList) {
				field.field.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
			}
			if (p_keyPressed_1_ == GLFW.GLFW_KEY_ENTER || p_keyPressed_1_ == GLFW.GLFW_KEY_KP_ENTER) {
				this.charTyped('\n');
			}
			this.anyKeyPressed();
			return false;
		}

		@Override
		public final boolean keyReleased(int p_keyReleased_1_, int p_keyReleased_2_, int p_keyReleased_3_) {
			for (TextFieldWrapper field : this.fieldList) {
				field.field.keyReleased(p_keyReleased_1_, p_keyReleased_2_, p_keyReleased_3_);
			}
			return false;
		}

		final void tick() {
			for (TextFieldWrapper field : this.fieldList) {
				field.field.tick();
			}
		}
	}

	public ExtGuiList(MinecraftClient mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
		super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
	}

	@Override
	public final List<T> getEntries() {
		return super.children();
	}

	@Override
	public final void setY(int pos) {
		this.y = pos;
	}

	@Override
	public final int getY() {
		return this.y;
	}

	@Override
	protected final boolean isSelectedItem(int slotIndex) {
		return getEntries().get(slotIndex).isSelected();
	}

	public final void tick() {
		for (T t : this.getEntries()) {
			t.tick();
		}
	}

	@Override
	public final int getRowWidth() {
		return this.getEntryWidth();
	}

	@Override
	public final int getWidth() {
		return this.width;
	}

	// Hacks for y offsetting
	@Override
	public final boolean mouseClicked(double mouseX, double mouseY, int mouseEvent) {
		if (mouseY - y >= this.top && mouseY - y <= this.bottom) {
			return super.mouseClicked(mouseX, mouseY - y, mouseEvent);
		} else {
			return false;
		}
	}

	@Override
	public final boolean mouseReleased(double x, double y, int mouseEvent) {
		return super.mouseReleased(x, y - this.y, mouseEvent);
	}

	@Override
	public final boolean mouseDragged(double arg0, double arg1, int arg2, double arg3, double arg4) {
		return super.mouseDragged(arg0, arg1 - y, arg2, arg3, arg4);
	}

	@Override
	public final boolean mouseScrolled(double amount, double mouseX, double mouseY) {
		// TODO check that the scroll is actually over this...
		return super.mouseScrolled(amount, mouseX, mouseY);
	}

	@Override
	public final void render(MatrixStack matrixStack, int mouseXIn, int mouseYIn, float partialTicks) {
		this.matrixStack = matrixStack;
		this.render(mouseXIn, mouseYIn, partialTicks);
		this.matrixStack = null;
	}

	@Override
	public void render(int mouseXIn, int mouseYIn, float partialTicks) {
		VersionedFunctions.glTranslatef(0, y, 0);
		super.render(matrixStack, mouseXIn, mouseYIn - y, partialTicks);
		VersionedFunctions.glTranslatef(0, -y, 0);
	}

	// Make the dirt background use visual positions that match the screen
	// so that dragging looks less weird
	// TODO: Reimplement this somehow (gone in 1.16)
	/*@Override
	protected final void renderHoleBackground(int y1, int y2,
			int alpha1, int alpha2) {
		if (y1 == 0) {
			super.renderHoleBackground(y1, y2, alpha1, alpha2);
			return;
		} else {
			VersionedFunctions.glTranslatef(0, -y, 0);

			super.renderHoleBackground(y1 + y, y2 + y, alpha1, alpha2);

			VersionedFunctions.glTranslatef(0, y, 0);
		}
	}*/

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
}
