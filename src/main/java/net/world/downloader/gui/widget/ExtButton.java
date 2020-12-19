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

import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.world.downloader.api.IExtButton;

/**
 * Extendible button, to deal with changing method signatures between versions.
 * The actual implementation is {@link WDLButton}, and methods are declared in {@link IExtButton}.
 */
public abstract class ExtButton extends ButtonWidget implements IExtButton, ParentElement {
	/**
	 * @deprecated Do not use; use {@link #setMessage} instead.
	 */
	@Deprecated
	protected static final Void message = null;
	/**
	 * @deprecated Do not use; use {@link #setEnabled} instead.
	 */
	@Deprecated
	protected static final Void active = null;
	
	// I don't have the textures for the buttons. -Svegon
	protected static final Identifier BUTTON_TEXTURES = new Identifier("button");
	
	@Nullable
	protected MatrixStack matrixStack;
	
	public ExtButton(int x, int y, int width, int height, Text message) {
		this(x, y, width, height, message, (b) -> {}, EMPTY);
	}
	
	public ExtButton(int x, int y, int width, int height, Text message, PressAction onPress,
			TooltipSupplier tooltipSupplier) {
		super(x, y, width, height, message, onPress, tooltipSupplier);
	}
	
	@Override
	public boolean mouseClicked(double arg0, double arg1, int arg2) {
		boolean result = super.mouseClicked(arg0, arg1, arg2);
		
		if (result && arg2 == 0) {
			this.mouseDown((int)arg0, (int)arg1);
		}
		
		return result;
	}

	@Override
	public boolean mouseReleased(double arg0, double arg1, int arg2) {
		boolean result = super.mouseReleased(arg0, arg1, arg2);
		
		if (arg2 == 0) {
			this.mouseUp((int)arg0, (int)arg1);
		}
		
		return result;
	}

	@Override
	public final boolean mouseDragged(double arg0, double arg1, int arg2, double arg3, double arg4) {
		boolean result = super.mouseDragged(arg0, arg1, arg2, arg3, arg4);
		
		if (arg2 == 0) {
			this.mouseDragged((int) arg0, (int) arg1);
		}
		
		return result;
	}

	@Override
	public final void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
		this.matrixStack = matrices;
		this.beforeDraw();
		super.render(matrices, mouseX, mouseY, partialTicks);
		this.afterDraw();
	}

	@Override
	protected final void renderBg(MatrixStack matrices, MinecraftClient mc, int mouseX, int mouseY) {
		super.renderBg(matrices, mc, mouseX, mouseY);
		this.midDraw();
	}

	@Override
	public void setMessage(Text message) {
		super.setMessage(message);
	}

	@Override
	public void setMessage(String message) {
		setMessage(new LiteralText(message));
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.active = enabled;
	}

	@Override
	public boolean isEnabled() {
		return super.active;
	}
}
