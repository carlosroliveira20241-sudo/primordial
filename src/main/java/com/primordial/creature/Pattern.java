package com.primordial.creature;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Padrões de pele (só visual). A textura do padrão é pintada com a cor secundária. */
public enum Pattern implements StringRepresentable {
	LISO("liso"),
	LISTRAS("listras"),
	MANCHAS("manchas"),
	GRADIENTE("gradiente"),
	PONTILHADO("pontilhado"),
	TIGRADO("tigrado"),
	OCELOS("ocelos");

	public static final Codec<Pattern> CODEC = StringRepresentable.fromEnum(Pattern::values);

	private final String name;

	Pattern(String name) {
		this.name = name;
	}

	public String translationKey() {
		return "pattern.primordial." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
