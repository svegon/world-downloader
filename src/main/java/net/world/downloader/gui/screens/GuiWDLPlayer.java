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
package net.world.downloader.gui.screens;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.world.downloader.WDL;
import net.world.downloader.api.IConfiguration;
import net.world.downloader.config.settings.PlayerSettings;
import net.world.downloader.gui.widget.ButtonDisplayGui;
import net.world.downloader.gui.widget.GuiNumericTextField;
import net.world.downloader.gui.widget.SettingButton;
import net.world.downloader.gui.widget.WDLButton;
import net.world.downloader.utils.VersionedFunctions;

public class GuiWDLPlayer extends WDLScreen {
	@Nullable
	private final Screen parent;
	private final WDL wdl;
	private final IConfiguration config;
	private SettingButton healthBtn;
	private SettingButton hungerBtn;
	private SettingButton playerPosBtn;
	private WDLButton pickPosBtn;
	private boolean showPosFields = false;
	private GuiNumericTextField posX;
	private GuiNumericTextField posY;
	private GuiNumericTextField posZ;
	private int posTextY;

	public GuiWDLPlayer(@Nullable Screen parent, WDL wdl) {
		super(new TranslatableText("wdl.gui.player.title", WDL.baseFolderName));
		this.parent = parent;
		this.wdl = wdl;
		this.config = wdl.worldProps;
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	@Override
	public void init() {
		int y = this.height / 4 - 15;
		this.healthBtn = this.addButton(new SettingButton(
				PlayerSettings.HEALTH, this.config, this.width / 2 - 100, y));
		y += 22;
		this.hungerBtn = this.addButton(new SettingButton(
				PlayerSettings.HUNGER, this.config, this.width / 2 - 100, y));
		y += 22;
		this.playerPosBtn = this.addButton(new SettingButton(
				PlayerSettings.PLAYER_POSITION, this.config, this.width / 2 - 100, y) {
			public @Override void performAction() {
				super.performAction();
				upadatePlayerPosVisibility();
			}
		});
		y += 22;
		this.posTextY = y + 4;
		this.posX = this.addTextField(new GuiNumericTextField(this.textRenderer,
				this.width / 2 - 87, y, 50, 16,
				new TranslatableText("wdl.gui.player.position.coord", "X")));
		this.posY = this.addTextField(new GuiNumericTextField(this.textRenderer,
				this.width / 2 - 19, y, 50, 16,
				new TranslatableText("wdl.gui.player.position.coord", "Y")));
		this.posZ = this.addTextField(new GuiNumericTextField(this.textRenderer,
				this.width / 2 + 48, y, 50, 16,
				new TranslatableText("wdl.gui.player.position.coord", "Z")));
		this.posX.setValue(config.getValue(PlayerSettings.PLAYER_X));
		this.posY.setValue(config.getValue(PlayerSettings.PLAYER_Y));
		this.posZ.setValue(config.getValue(PlayerSettings.PLAYER_Z));
		this.posX.setMaxLength(7);
		this.posY.setMaxLength(7);
		this.posZ.setMaxLength(7);
		y += 18;
		this.pickPosBtn = this.addButton(new WDLButton(
				this.width / 2 - 0, y, 100, 20,
				new TranslatableText("wdl.gui.player.setPositionToCurrentPosition")) {
			public @Override void performAction() {
				setPlayerPosToPlayerPosition();
			}
		});

		upadatePlayerPosVisibility();

		this.addButton(new ButtonDisplayGui(this.width / 2 - 100, this.height - 29,
				200, 20, this.parent));
	}

	@Override
	public void removed() {
		if (this.showPosFields) {
			this.config.setValue(PlayerSettings.PLAYER_X, posX.getValue());
			this.config.setValue(PlayerSettings.PLAYER_Y, posY.getValue());
			this.config.setValue(PlayerSettings.PLAYER_Z, posZ.getValue());
		}

		wdl.saveProps();
	}

	/**
	 * Draws the screen and all the components in it.
	 */
	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		this.drawListBackground(23, 32, 0, 0, height, width);

		Text tooltip = null;

		if (this.showPosFields) {
			this.drawString(this.textRenderer, "X:", this.width / 2 - 99,
					this.posTextY, 0xFFFFFF);
			this.drawString(this.textRenderer, "Y:", this.width / 2 - 31,
					this.posTextY, 0xFFFFFF);
			this.drawString(this.textRenderer, "Z:", this.width / 2 + 37,
					this.posTextY, 0xFFFFFF);

			if (posX.isHovered()) {
				tooltip = new TranslatableText("wdl.gui.player.positionTextBox.description", "X");
			} else if (posY.isHovered()) {
				tooltip = new TranslatableText("wdl.gui.player.positionTextBox.description", "Y");
			} else if (posZ.isHovered()) {
				tooltip = new TranslatableText("wdl.gui.player.positionTextBox.description", "Z");
			}

			if (pickPosBtn.isHovered()) {
				tooltip = new TranslatableText("wdl.gui.player.setPositionToCurrentPosition.description");
			}
		}

		if (healthBtn.isHovered()) {
			tooltip = healthBtn.getTooltip();
		}
		if (hungerBtn.isHovered()) {
			tooltip = hungerBtn.getTooltip();
		}
		if (playerPosBtn.isHovered()) {
			tooltip = playerPosBtn.getTooltip();
		}

		super.render(mouseX, mouseY, partialTicks);

		if (tooltip != null) {
			this.drawGuiInfoBox(tooltip, width, height, 48);
		}
	}

	private void upadatePlayerPosVisibility() {
		boolean show = config.getValue(PlayerSettings.PLAYER_POSITION) == PlayerSettings.PlayerPos.XYZ;

		showPosFields = show;
		posX.setVisible(show);
		posY.setVisible(show);
		posZ.setVisible(show);
		pickPosBtn.visible = showPosFields;
	}

	private void setPlayerPosToPlayerPosition() {
		this.posX.setValue((int)VersionedFunctions.getEntityX(wdl.player));
		this.posY.setValue((int)VersionedFunctions.getEntityY(wdl.player));
		this.posZ.setValue((int)VersionedFunctions.getEntityZ(wdl.player));
	}
}
