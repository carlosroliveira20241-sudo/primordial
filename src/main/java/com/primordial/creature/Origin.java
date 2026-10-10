package com.primordial.creature;

import com.mojang.serialization.Codec;
import com.primordial.Primordial;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/** As 3 larvas da tela "Escolha sua Origem". Cada uma começa com uma boca e um bônus diferente. */
public enum Origin implements StringRepresentable {
	FILTRADORA("filtradora", "boca_de_filtro"),
	MORDEDORA("mordedora", "boca_mordedora"),
	NECROFAGA("necrofaga", "boca_necrofaga");

	public static final Codec<Origin> CODEC = StringRepresentable.fromEnum(Origin::values);

	private final String name;
	private final String startingMouth;

	Origin(String name, String startingMouth) {
		this.name = name;
		this.startingMouth = startingMouth;
	}

	public Identifier startingMouth() {
		return Primordial.id(this.startingMouth);
	}

	public String translationKey() {
		return "origin.primordial." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
