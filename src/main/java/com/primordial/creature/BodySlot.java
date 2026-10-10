package com.primordial.creature;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Um lugar concreto no corpo da criatura (um órgão pode ocupar ORGAN_1, ORGAN_2...). */
public enum BodySlot implements StringRepresentable {
	MOUTH("mouth", SlotType.MOUTH),
	EYES("eyes", SlotType.EYES),
	HEAD("head", SlotType.HEAD),
	FINS("fins", SlotType.LIMBS),
	FORELIMBS("forelimbs", SlotType.LIMBS),
	BACK("back", SlotType.BACK),
	TAIL("tail", SlotType.TAIL),
	SKIN("skin", SlotType.SKIN),
	ORGAN_1("organ_1", SlotType.ORGAN),
	ORGAN_2("organ_2", SlotType.ORGAN),
	ORGAN_3("organ_3", SlotType.ORGAN),
	WINGS("wings", SlotType.WINGS);

	public static final Codec<BodySlot> CODEC = StringRepresentable.fromEnum(BodySlot::values);

	private final String name;
	private final SlotType type;

	BodySlot(String name, SlotType type) {
		this.name = name;
		this.type = type;
	}

	public SlotType type() {
		return this.type;
	}

	public String translationKey() {
		return "slot.primordial." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
