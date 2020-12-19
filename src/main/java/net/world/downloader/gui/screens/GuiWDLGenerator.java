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
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.world.downloader.WDL;
import net.world.downloader.api.IConfiguration;
import net.world.downloader.config.settings.GeneratorSettings;
import net.world.downloader.gui.widget.ButtonDisplayGui;
import net.world.downloader.gui.widget.SettingButton;
import net.world.downloader.gui.widget.WDLButton;
import net.world.downloader.gui.widget.WDLTextField;
import net.world.downloader.utils.VersionedFunctions;

public class GuiWDLGenerator extends WDLScreen {
	@Nullable
	private final Screen parent;
	private final WDL wdl;
	private final IConfiguration config;
	private WDLTextField seedField;
	private SettingButton generatorBtn;
	private SettingButton generateStructuresBtn;
	private WDLButton settingsPageBtn;

	private String seedText;

	public GuiWDLGenerator(@Nullable Screen parent, WDL wdl) {
		super(new TranslatableText("wdl.gui.generator.title", WDL.baseFolderName));
		this.parent = parent;
		this.wdl = wdl;
		this.config = wdl.worldProps;
	}

	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	@Override
	public void init() {
		this.seedText = I18n.translate("wdl.gui.generator.seed");
		int seedWidth = this.textRenderer.getWidth(seedText + " ");

		int y = this.height / 4 - 15;
		this.seedField = this.addTextField(new WDLTextField(this.textRenderer,
				this.width / 2 - (100 - seedWidth), y, 200 - seedWidth, 18, new LiteralText(seedText)));
		this.seedField.setText(config.getValue(GeneratorSettings.SEED));
		y += 22;
		this.generatorBtn = this.addButton(new SettingButton(
				GeneratorSettings.GENERATOR, this.config, this.width / 2 - 100, y) {
			public @Override void performAction() {
				super.performAction();
				updateSettingsButtonVisibility();
				// Clear any existing custom values, as they don't apply to another generator.
				config.clearValue(GeneratorSettings.GENERATOR_NAME);
				config.clearValue(GeneratorSettings.GENERATOR_VERSION);
				config.clearValue(GeneratorSettings.GENERATOR_OPTIONS);
			}
		});
		y += 22;
		this.generateStructuresBtn = this.addButton(new SettingButton(
				GeneratorSettings.GENERATE_STRUCTURES, this.config, this.width / 2 - 100, y));
		y += 22;
		this.settingsPageBtn = this.addButton(new ButtonDisplayGui(
				this.width / 2 - 100, y, 200, 20,
				new LiteralText(""), this::makeGeneratorSettingsGui));
		updateSettingsButtonVisibility();

		this.addButton(new ButtonDisplayGui(this.width / 2 - 100, height - 29,
				200, 20, this.parent));
	}

	/**
	 * Gets the proxy GUI to use for the current settings.
	 */
	private Screen makeGeneratorSettingsGui() {
		GeneratorSettings.Generator generator = config.getValue(GeneratorSettings.GENERATOR);
		String generatorConfig = config.getValue(GeneratorSettings.GENERATOR_OPTIONS);
		return VersionedFunctions.makeGeneratorSettingsGui(generator, this, generatorConfig,
				value -> config.setValue(GeneratorSettings.GENERATOR_OPTIONS, value));
	}

	@Override
	public void removed() {
		config.setValue(GeneratorSettings.SEED, this.seedField.getText());

		wdl.saveProps();
	}

	/**
	 * Draws the screen and all the components in it.
	 */
	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		this.drawListBackground(23, 32, 0, 0, height, width);

		this.drawString(this.textRenderer, seedText, this.width / 2 - 100,
				this.height / 4 - 10, 0xFFFFFF);

		super.render(mouseX, mouseY, partialTicks);

		Text tooltip = null;

		if (seedField.isHovered()) {
			tooltip = new TranslatableText("wdl.gui.generator.seed.description");
		} else if (generatorBtn.isHovered()) {
			tooltip = generatorBtn.getTooltip();
		} else if (generateStructuresBtn.isHovered()) {
			tooltip = generateStructuresBtn.getTooltip();
		}
		this.drawGuiInfoBox(tooltip, width, height, 48);
	}

	/**
	 * Updates whether the {@link #settingsPageBtn} is shown or hidden, and
	 * the text on it.
	 */
	private void updateSettingsButtonVisibility() {
		switch (this.config.getValue(GeneratorSettings.GENERATOR)) {
		case FLAT:
			settingsPageBtn.visible = true;
			settingsPageBtn.setMessage(new TranslatableText("wdl.gui.generator.flatSettings"));
			break;
		case CUSTOMIZED:
			settingsPageBtn.visible = true;
			settingsPageBtn.setMessage(new TranslatableText("wdl.gui.generator.customSettings"));
			break;
		case BUFFET:
			settingsPageBtn.visible = true;
			settingsPageBtn.setMessage(new TranslatableText("wdl.gui.generator.buffetSettings"));
			break;
		case SINGLE_BIOME_SURFACE:
		case SINGLE_BIOME_CAVES:
		case SINGLE_BIOME_FLOATING_ISLANDS:
			settingsPageBtn.visible = true;
			settingsPageBtn.setMessage(new TranslatableText("wdl.gui.generator.selectBiome"));
			break;
		default:
			settingsPageBtn.visible = false;
		}
	}
}
