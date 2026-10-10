package com.primordial.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;

/**
 * O progresso particular do jogador (só ele recebe esses dados).
 *
 * @param eaten  quantas Melhorias de cada parte ele já comeu (1 = desbloqueada, depois sobe de nível)
 * @param pity   abates seguidos sem Melhoria, por espécie (garantia contra azar)
 * @param bought partes que já foram pagas com DNA (trocar de novo é de graça)
 */
public record CreatureProgress(
	boolean originChosen,
	Origin origin,
	int dna,
	int xp,
	int level,
	Map<Identifier, Integer> eaten,
	Map<Identifier, Integer> pity,
	Optional<GlobalPos> nest,
	int bigKills,
	Set<Identifier> bought
) {
	/** Quantas Melhorias iguais é preciso comer para chegar a cada nível. Índice 0 = nível I. */
	public static final int[] EATEN_FOR_LEVEL = {1, 3, 6};

	public static final CreatureProgress DEFAULT = new CreatureProgress(false, Origin.FILTRADORA, 0, 0, 1, Map.of(), Map.of(), Optional.empty(), 0, Set.of());

	public static final Codec<CreatureProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.BOOL.optionalFieldOf("origin_chosen", false).forGetter(CreatureProgress::originChosen),
		Origin.CODEC.optionalFieldOf("origin", Origin.FILTRADORA).forGetter(CreatureProgress::origin),
		Codec.INT.optionalFieldOf("dna", 0).forGetter(CreatureProgress::dna),
		Codec.INT.optionalFieldOf("xp", 0).forGetter(CreatureProgress::xp),
		Codec.INT.optionalFieldOf("level", 1).forGetter(CreatureProgress::level),
		Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("eaten", Map.of()).forGetter(CreatureProgress::eaten),
		Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("pity", Map.of()).forGetter(CreatureProgress::pity),
		GlobalPos.CODEC.optionalFieldOf("nest").forGetter(CreatureProgress::nest),
		Codec.INT.optionalFieldOf("big_kills", 0).forGetter(CreatureProgress::bigKills),
		Identifier.CODEC.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("bought", Set.of()).forGetter(CreatureProgress::bought)
	).apply(i, CreatureProgress::new));

	public CreatureProgress {
		eaten = Map.copyOf(eaten);
		pity = Map.copyOf(pity);
		bought = Set.copyOf(bought);
	}

	public boolean unlocked(Identifier part) {
		return this.eaten.getOrDefault(part, 0) > 0;
	}

	public int eatenCount(Identifier part) {
		return this.eaten.getOrDefault(part, 0);
	}

	/** Nível da parte (0 = bloqueada, 1 a 3). */
	public int partLevel(Identifier part) {
		int count = this.eatenCount(part);
		int level = 0;
		for (int threshold : EATEN_FOR_LEVEL) {
			if (count >= threshold) {
				level++;
			}
		}
		return level;
	}

	public CreatureProgress withOrigin(Origin origin) {
		return new CreatureProgress(true, origin, this.dna, this.xp, this.level, this.eaten, this.pity, this.nest, this.bigKills, this.bought);
	}

	public CreatureProgress withDna(int dna) {
		return new CreatureProgress(this.originChosen, this.origin, Math.max(0, dna), this.xp, this.level, this.eaten, this.pity, this.nest, this.bigKills, this.bought);
	}

	public CreatureProgress withXpAndLevel(int xp, int level) {
		return new CreatureProgress(this.originChosen, this.origin, this.dna, xp, level, this.eaten, this.pity, this.nest, this.bigKills, this.bought);
	}

	public CreatureProgress withEaten(Identifier part, int count) {
		Map<Identifier, Integer> copy = new HashMap<>(this.eaten);
		copy.put(part, count);
		return new CreatureProgress(this.originChosen, this.origin, this.dna, this.xp, this.level, copy, this.pity, this.nest, this.bigKills, this.bought);
	}

	public CreatureProgress withPity(Identifier species, int count) {
		Map<Identifier, Integer> copy = new HashMap<>(this.pity);
		if (count <= 0) {
			copy.remove(species);
		} else {
			copy.put(species, count);
		}
		return new CreatureProgress(this.originChosen, this.origin, this.dna, this.xp, this.level, this.eaten, copy, this.nest, this.bigKills, this.bought);
	}

	public CreatureProgress withNest(Optional<GlobalPos> nest) {
		return new CreatureProgress(this.originChosen, this.origin, this.dna, this.xp, this.level, this.eaten, this.pity, nest, this.bigKills, this.bought);
	}

	public CreatureProgress withBigKills(int bigKills) {
		return new CreatureProgress(this.originChosen, this.origin, this.dna, this.xp, this.level, this.eaten, this.pity, this.nest, bigKills, this.bought);
	}

	public CreatureProgress withBought(Identifier part) {
		Set<Identifier> copy = new HashSet<>(this.bought);
		copy.add(part);
		return new CreatureProgress(this.originChosen, this.origin, this.dna, this.xp, this.level, this.eaten, this.pity, this.nest, this.bigKills, copy);
	}

	public boolean hasBought(Identifier part) {
		return this.bought.contains(part);
	}

	/** XP total necessária para sair do nível {@code level} para o próximo. */
	public static int xpForNextLevel(int level) {
		return 20 + (level - 1) * 15 + (level - 1) * (level - 1) * 3;
	}
}
