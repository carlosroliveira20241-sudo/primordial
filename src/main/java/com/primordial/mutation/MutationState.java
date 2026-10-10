package com.primordial.mutation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Mutações do jogador: as já escolhidas, quantas escolhas ainda tem direito e as 3 cartas oferecidas agora.
 * A cada 3 níveis ele ganha uma escolha.
 */
public record MutationState(List<Identifier> chosen, int pending, List<Identifier> offer) {
	public static final MutationState DEFAULT = new MutationState(List.of(), 0, List.of());

	public static final Codec<MutationState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Identifier.CODEC.listOf().optionalFieldOf("chosen", List.of()).forGetter(MutationState::chosen),
		Codec.INT.optionalFieldOf("pending", 0).forGetter(MutationState::pending),
		Identifier.CODEC.listOf().optionalFieldOf("offer", List.of()).forGetter(MutationState::offer)
	).apply(i, MutationState::new));

	public MutationState withPending(int pending, List<Identifier> offer) {
		return new MutationState(this.chosen, pending, offer);
	}

	public MutationState withChosen(Identifier id, int pending, List<Identifier> offer) {
		List<Identifier> copy = new ArrayList<>(this.chosen);
		copy.add(id);
		return new MutationState(copy, pending, offer);
	}
}
