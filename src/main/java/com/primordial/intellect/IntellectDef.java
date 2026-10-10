package com.primordial.intellect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Uma habilidade de inteligência, lida de {@code data/primordial/intellect/<nome>.json}. Ela é COMPRADA com DNA, exige
 * um nível de cérebro e, equipada num slot, liberta uma ação do Minecraft normal.
 *
 * @param cost       DNA para comprar
 * @param brain      nível mínimo do cérebro
 * @param capability a ação que liberta: break_1..break_4 (quebrar), place (colocar bloco), use_/craft_ + tipo de ferramenta
 *                   (pickaxe, shovel, axe, sword, bow) ou shoot (tiro)
 * @param input      "passive" (vale sozinha, no botão esquerdo/direito do mouse) ou "r" (usada com a tecla R)
 */
public record IntellectDef(int cost, int brain, String capability, String input) {
	public static final Codec<IntellectDef> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("cost").forGetter(IntellectDef::cost),
		Codec.INT.optionalFieldOf("brain", 1).forGetter(IntellectDef::brain),
		Codec.STRING.fieldOf("capability").forGetter(IntellectDef::capability),
		Codec.STRING.optionalFieldOf("input", "passive").forGetter(IntellectDef::input)
	).apply(i, IntellectDef::new));

	public boolean usesR() {
		return this.input.equals("r");
	}
}
