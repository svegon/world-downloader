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
package net.world.downloader.handler.entity;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.world.downloader.handler.HandlerException;
import net.world.downloader.utils.ReflectionUtils;

public class VillagerHandler extends EntityHandler<VillagerEntity, MerchantScreenHandler> {
	public VillagerHandler() {
		super(VillagerEntity.class, MerchantScreenHandler.class);
	}

	@Override
	public Text copyData(MerchantScreenHandler container, VillagerEntity villager, boolean riding)
			throws HandlerException {
		Merchant merchant = ReflectionUtils.findAndGetPrivateField(container, Merchant.class);
		TradeOfferList recipes = merchant.getOffers();
		ReflectionUtils.findAndSetPrivateField(villager, TradeOfferList.class, recipes);
		
		if(!(merchant instanceof MerchantEntity)) {
			throw new HandlerException("Attempting to save a non-entity merchant.");
		}

		Text displayName = ((MerchantEntity) merchant).getDisplayName();
		
		if (!(displayName instanceof TranslatableText)) {
			// Taking the toString to reflect JSON structure
			String componentDesc = String.valueOf(displayName);
			throw new HandlerException("wdl.messages.onGuiClosedWarning.villagerCareer.notAComponent", componentDesc);
		}
		
		if(!(merchant instanceof VillagerEntity)) {
			return new LiteralText("Didn't save the merchant as it was a wandering trader.");
		}

		// I don't have the nerves for this.
		villager.fromTag(((VillagerEntity) merchant).toTag(new CompoundTag()));
		
		VillagerProfession profession = villager.getVillagerData().getProfession();

		return new TranslatableText("wdl.messages.onGuiClosedInfo.savedEntity.villager.tradesAndCareer",
				displayName, profession);
	}
}
