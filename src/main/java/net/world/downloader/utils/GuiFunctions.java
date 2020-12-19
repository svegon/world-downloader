/*
 * This file is part of World Downloader: A mod to make backups of your multiplayer worlds.
 * https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/2520465-world-downloader-mod-create-backups-of-your-builds
 *
 * Copyright (c) 2014 nairol, cubic72
 * Copyright (c) 2017-2020 Pokechu22, julialy
 *
 * This project is licensed under the MMPLv2.  The full text of the MMPL can be
 * found in LICENSE.md, or online at https://github.com/iopleke/MMPLv2/blob/master/LICENSE.md
 * For information about this the MMPLv2, see https://stopmodreposts.org/
 *
 * Do not redistribute (in modified or unmodified form) without prior permission.
 */
package net.world.downloader.utils;

import static org.lwjgl.opengl.GL11.*;

import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.world.World;

/**
 * Versioned functions related to GUIs.
 */
public final class GuiFunctions {
	private GuiFunctions() {
		throw new AssertionError();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#makePlayer
	 */
	public static ClientPlayerEntity makePlayer(MinecraftClient minecraft, World world,
			ClientPlayNetworkHandler nhpc, ClientPlayerEntity base) {
		return new ClientPlayerEntity(minecraft, (ClientWorld)world, nhpc,
				base.getStatHandler(), base.getRecipeBook(), false, false);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#getPointOfView
	 */
	//TODO
	public static Perspective getPointOfView(GameOptions settings) {
		return settings.getPerspective();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#setFirstPersonPointOfView
	 */
	public static void setFirstPersonPointOfView(GameOptions settings) {
		settings.setPerspective(Perspective.FIRST_PERSON);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#restorePointOfView
	 */
	public static void restorePointOfView(GameOptions settings, Perspective value) {
		settings.setPerspective(value);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#drawDarkBackground
	 */
	public static void drawDarkBackground(int top, int left, int bottom, int right) {
		RenderSystem.disableLighting();
		RenderSystem.disableFog();

		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();

		MinecraftClient.getInstance().getTextureManager().bindTexture(
				DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		float textureSize = 32.0F;
		b.begin(7, VertexFormats.POSITION_COLOR_TEXTURE);
		b.normal(0, bottom, 0).color(32, 32, 32, 255).texture(0 / textureSize, bottom / textureSize);
		b.end();
		b.normal(right, bottom, 0).color(32, 32, 32, 255).texture(right / textureSize, bottom / textureSize);
		b.normal(right, top, 0).color(32, 32, 32, 255).texture(right / textureSize, top / textureSize);
		b.normal(left, top, 0).color(32, 32, 32, 255).texture(left / textureSize, top / textureSize);
		t.draw();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#drawBorder
	 */
	static void drawBorder(int topMargin, int bottomMargin, int top, int left, int bottom, int right) {
		RenderSystem.disableLighting();
		RenderSystem.disableFog();
		RenderSystem.disableDepthTest();
		byte padding = 4;

		MinecraftClient.getInstance().getTextureManager().bindTexture(
				DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		float textureSize = 32.0F;

		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();

		// Box code is GuiSlot.overlayBackground
		// Upper box
		int upperBoxEnd = top + topMargin;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		b.begin(7, VertexFormats.POSITION_COLOR_TEXTURE);
		b.normal(left, upperBoxEnd, 0.0F).color(64, 64, 64, 255).texture(0, upperBoxEnd / textureSize)
			.next();
		b.normal(right, upperBoxEnd, 0.0F).color(64, 64, 64, 255).texture(right / textureSize,
				upperBoxEnd / textureSize).next();
		b.normal(right, top, 0.0F).color(64, 64, 64, 255).texture(right / textureSize, top / textureSize)
			.next();
		b.normal(left, top, 0.0F).color(64, 64, 64, 255).texture(0, top / textureSize).next();
		t.draw();

		// Lower box
		int lowerBoxStart = bottom - bottomMargin;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		b.begin(7, VertexFormats.POSITION_COLOR_TEXTURE);
		b.normal(left, bottom, 0.0F).color(64, 64, 64, 255).texture(0, bottom / textureSize).next();
		b.normal(right, bottom, 0.0F).color(64, 64, 64, 255).texture(right / textureSize, bottom /
				textureSize).next();
		b.normal(right, lowerBoxStart, 0.0F).color(64, 64, 64, 255).texture(right / textureSize,
				lowerBoxStart / textureSize).next();
		b.normal(left, lowerBoxStart, 0.0F).color(64, 64, 64, 255).texture(0, lowerBoxStart /
				textureSize).next();
		t.draw();

		// Gradients
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 0, 1);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(GL_SMOOTH);
		RenderSystem.disableTexture();
		b.begin(7, VertexFormats.POSITION_COLOR_TEXTURE);
		b.normal(left, upperBoxEnd + padding, 0.0F).color(0, 0, 0, 0).texture(0, 1).next();
		b.normal(right, upperBoxEnd + padding, 0.0F).color(0, 0, 0, 0).texture(1, 1).next();
		b.normal(right, upperBoxEnd, 0.0F).color(0, 0, 0, 255).texture(1, 0).next();
		b.normal(left, upperBoxEnd, 0.0F).color(0, 0, 0, 255).texture(0, 0).next();
		t.draw();
		b.begin(7, VertexFormats.POSITION_COLOR_TEXTURE);
		b.normal(left, lowerBoxStart, 0.0F).color(0, 0, 0, 255).texture(0, 1).next();
		b.normal(right, lowerBoxStart, 0.0F).color(0, 0, 0, 255).texture(1, 1).next();
		b.normal(right, lowerBoxStart - padding, 0.0F).color(0, 0, 0, 0).texture(1, 0).next();
		b.normal(left, lowerBoxStart - padding, 0.0F).color(0, 0, 0, 0).texture(0, 0).next();
		t.draw();

		RenderSystem.enableTexture();
		RenderSystem.shadeModel(GL_FLAT);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#setClipboardString
	 */
	@SuppressWarnings("resource")
	static void setClipboardString(String text) {
		MinecraftClient.getInstance().keyboard.setClipboard(text);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#openLink
	 */
	static void openLink(String url) {
		Util.getOperatingSystem().open(url);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#glColor4f
	 */
	static void glColor4f(float r, float g, float b, float a) {
		RenderSystem.color4f(r, g, b, a);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#glTranslatef
	 */
	static void glTranslatef(float x, float y, float z) {
		RenderSystem.translatef(x, y, z);
	}

	/* (non-javadoc)
	 * @see VersionedFunctions#applyLinkFormatting
	 */
	static Style createLinkFormatting(String url) {
		return Style.EMPTY.withColor(Formatting.BLUE)
				.withFormatting(Formatting.UNDERLINE)
				.withClickEvent(new ClickEvent(Action.OPEN_URL, url));
	}

	/* (non-javadoc)
	 * @See VersionedFunctions#createConfirmScreen
	 */
	static ConfirmScreen createConfirmScreen(BooleanConsumer action, Text line1, Text line2,
			Text confirm, Text cancel) {
		return new ConfirmScreen(action, line1, line2, confirm, cancel);
	}
}
