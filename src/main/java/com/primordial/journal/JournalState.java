package com.primordial.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * O diário do jogador: criaturas vistas e abatidas (bestiário), contadores de coisas que ele fez e os desafios
 * já cumpridos. Só o próprio jogador recebe.
 */
public record JournalState(Map<Identifier, Integer> kills, List<Identifier> seen, Map<String, Integer> counters, List<String> done) {
	public static final JournalState DEFAULT = new JournalState(Map.of(), List.of(), Map.of(), List.of());

	public static final Codec<JournalState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(JournalState::kills),
		Identifier.CODEC.listOf().optionalFieldOf("seen", List.of()).forGetter(JournalState::seen),
		Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("counters", Map.of()).forGetter(JournalState::counters),
		Codec.STRING.listOf().optionalFieldOf("done", List.of()).forGetter(JournalState::done)
	).apply(i, JournalState::new));

	public int kills(Identifier species) {
		return this.kills.getOrDefault(species, 0);
	}

	public int counter(String key) {
		return this.counters.getOrDefault(key, 0);
	}

	public boolean hasSeen(Identifier species) {
		return this.seen.contains(species);
	}

	public JournalState withKill(Identifier species) {
		Map<Identifier, Integer> copy = new HashMap<>(this.kills);
		copy.merge(species, 1, Integer::sum);
		return new JournalState(copy, this.seen, this.counters, this.done);
	}

	public JournalState withSeen(Identifier species) {
		List<Identifier> copy = new ArrayList<>(this.seen);
		copy.add(species);
		return new JournalState(this.kills, copy, this.counters, this.done);
	}

	public JournalState withCounter(String key, int value) {
		Map<String, Integer> copy = new HashMap<>(this.counters);
		copy.put(key, value);
		return new JournalState(this.kills, this.seen, copy, this.done);
	}

	public JournalState withDone(String id) {
		List<String> copy = new ArrayList<>(this.done);
		copy.add(id);
		return new JournalState(this.kills, this.seen, this.counters, copy);
	}
}
