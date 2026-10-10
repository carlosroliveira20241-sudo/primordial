package com.primordial.creature;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Tipo de encaixe de uma parte: diz em quais slots do corpo ela pode ser equipada. */
public enum SlotType implements StringRepresentable {
	MOUTH("mouth"),
	EYES("eyes"),
	HEAD("head"),
	LIMBS("limbs"),
	BACK("back"),
	TAIL("tail"),
	SKIN("skin"),
	ORGAN("organ"),
	WINGS("wings");

	public static final Codec<SlotType> CODEC = StringRepresentable.fromEnum(SlotType::values);

	private final String name;

	SlotType(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
