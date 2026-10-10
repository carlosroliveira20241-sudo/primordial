package com.primordial.intellect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Mente e corpo do jogador: nível do cérebro (slots de habilidade), nível de força (espaços de inventário), as
 * habilidades de inteligência já compradas e o que está nos slots (texto vazio = slot vazio).
 * Todo mundo recebe (o cérebro cresce no corpo e os amigos veem).
 */
public record IntellectState(int brain, int strength, List<Identifier> owned, List<String> slots) {
	public static final int MAX_SLOTS = 4;
	public static final int MAX_BRAIN = 8;
	public static final int MAX_STRENGTH = 10;
	public static final IntellectState DEFAULT = new IntellectState(1, 1, List.of(), List.of("", "", "", ""));

	public static final Codec<IntellectState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("brain", 1).forGetter(IntellectState::brain),
		Codec.INT.optionalFieldOf("strength", 1).forGetter(IntellectState::strength),
		Identifier.CODEC.listOf().optionalFieldOf("owned", List.of()).forGetter(IntellectState::owned),
		Codec.STRING.listOf().optionalFieldOf("slots", List.of("", "", "", "")).forGetter(IntellectState::slots)
	).apply(i, IntellectState::new));

	/** Quantos slots o cérebro libera: 1 no início, 2 no nível 3, 3 no nível 5, 4 no nível 7. */
	public static int slotsFor(int brain) {
		return brain >= 7 ? 4 : brain >= 5 ? 3 : brain >= 3 ? 2 : 1;
	}

	public int slotCount() {
		return slotsFor(this.brain);
	}

	/** A habilidade no slot, ou nulo se vazio ou bloqueado. */
	public Identifier slot(int index) {
		if (index < 0 || index >= this.slotCount() || index >= this.slots.size() || this.slots.get(index).isEmpty()) {
			return null;
		}
		return Identifier.tryParse(this.slots.get(index));
	}

	public boolean isEquipped(Identifier id) {
		return this.slots.contains(id.toString());
	}

	public IntellectState withBrain(int brain) {
		return new IntellectState(brain, this.strength, this.owned, this.slots);
	}

	public IntellectState withStrength(int strength) {
		return new IntellectState(this.brain, strength, this.owned, this.slots);
	}

	public IntellectState withOwned(Identifier id) {
		List<Identifier> copy = new ArrayList<>(this.owned);
		copy.add(id);
		return new IntellectState(this.brain, this.strength, copy, this.slots);
	}

	public IntellectState withSlot(int index, String value) {
		List<String> copy = new ArrayList<>(this.slots);
		while (copy.size() < MAX_SLOTS) {
			copy.add("");
		}
		copy.set(index, value);
		return new IntellectState(this.brain, this.strength, this.owned, copy);
	}
}
