package com.primordial.part;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum PartRarity implements StringRepresentable {
	COMMON("common", ChatFormatting.WHITE, 0xE8E8E8),
	RARE("rare", ChatFormatting.AQUA, 0x4FC3F7),
	EPIC("epic", ChatFormatting.LIGHT_PURPLE, 0xBA68C8),
	LEGENDARY("legendary", ChatFormatting.GOLD, 0xFFB300);

	public static final Codec<PartRarity> CODEC = StringRepresentable.fromEnum(PartRarity::values);

	private final String name;
	public final ChatFormatting formatting;
	/** Cor RGB usada no brilho/partículas da Melhoria. */
	public final int color;

	PartRarity(String name, ChatFormatting formatting, int color) {
		this.name = name;
		this.formatting = formatting;
		this.color = color;
	}

	public String translationKey() {
		return "rarity.primordial." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
