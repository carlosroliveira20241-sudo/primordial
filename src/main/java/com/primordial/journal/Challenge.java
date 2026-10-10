package com.primordial.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

/**
 * Um desafio, lido de {@code data/primordial/challenges/<nome>.json}: quando o contador {@code counter} chega a
 * {@code goal}, o jogador ganha DNA e XP. Contadores usados: kills, kill:&lt;criatura&gt;, alpha, dig, plant, morsel,
 * coral, symbiont, mutation, level, stage. Nome e descrição: {@code challenge.primordial.<nome>} e {@code .desc}.
 */
public record Challenge(String counter, int goal, int dna, int xp) {
	public static final Codec<Challenge> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("counter").forGetter(Challenge::counter),
		Codec.INT.optionalFieldOf("goal", 1).forGetter(Challenge::goal),
		Codec.INT.optionalFieldOf("dna", 10).forGetter(Challenge::dna),
		Codec.INT.optionalFieldOf("xp", 10).forGetter(Challenge::xp)
	).apply(i, Challenge::new));

	public static Component name(String id) {
		return Component.translatable("challenge.primordial." + id);
	}

	public static Component description(String id) {
		return Component.translatable("challenge.primordial." + id + ".desc");
	}
}
