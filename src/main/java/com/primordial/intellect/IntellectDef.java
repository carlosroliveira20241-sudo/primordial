package com.primordial.intellect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Uma habilidade de inteligência, lida de {@code data/primordial/intellect/<habilidade>.json}. O nome do arquivo é o
 * nome de uma habilidade registrada em {@code Abilities}. Ela é COMPRADA com DNA e exige um nível mínimo de cérebro.
 *
 * @param cost  DNA para comprar
 * @param brain nível mínimo do cérebro
 */
public record IntellectDef(int cost, int brain) {
	public static final Codec<IntellectDef> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("cost").forGetter(IntellectDef::cost),
		Codec.INT.optionalFieldOf("brain", 1).forGetter(IntellectDef::brain)
	).apply(i, IntellectDef::new));
}
