package com.primordial.creature;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Dieta definida pela boca. Diz quanto DNA/comida a criatura tira de plantas e de carne. */
public enum Diet implements StringRepresentable {
	HERBIVORE("herbivore", 1.0F, 0.0F),
	CARNIVORE("carnivore", 0.0F, 1.0F),
	OMNIVORE("omnivore", 0.75F, 0.75F);

	public static final Codec<Diet> CODEC = StringRepresentable.fromEnum(Diet::values);

	private final String name;
	public final float plantEfficiency;
	public final float meatEfficiency;

	Diet(String name, float plantEfficiency, float meatEfficiency) {
		this.name = name;
		this.plantEfficiency = plantEfficiency;
		this.meatEfficiency = meatEfficiency;
	}

	public boolean eatsPlants() {
		return this.plantEfficiency > 0;
	}

	public boolean eatsMeat() {
		return this.meatEfficiency > 0;
	}

	public String translationKey() {
		return "diet.primordial." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
