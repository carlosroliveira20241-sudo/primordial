package com.primordial.synergy;

import com.primordial.Primordial;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.PartRef;
import com.primordial.data.JarData;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class SynergyRegistry {
	private static final Map<Identifier, Synergy> SYNERGIES = new LinkedHashMap<>();

	private SynergyRegistry() {
	}

	public static void load() {
		SYNERGIES.clear();
		SYNERGIES.putAll(JarData.load("synergies", Synergy.CODEC));
		Primordial.LOGGER.info("Carreguei {} sinergias", SYNERGIES.size());
	}

	public static Map<Identifier, Synergy> all() {
		return Collections.unmodifiableMap(SYNERGIES);
	}

	/** Quantas partes equipadas há de cada família. */
	public static Map<String, Integer> familyCounts(CreatureBody body) {
		Map<String, Integer> counts = new HashMap<>();
		for (PartRef ref : body.parts().values()) {
			PartDefinition def = PartRegistry.get(ref.id());
			if (def != null) {
				for (String family : def.families()) {
					counts.merge(family, 1, Integer::sum);
				}
			}
		}
		return counts;
	}

	/** As sinergias ativas agora. */
	public static List<Identifier> active(CreatureBody body) {
		Map<String, Integer> counts = familyCounts(body);
		List<Identifier> result = new ArrayList<>();
		SYNERGIES.forEach((id, synergy) -> {
			if (counts.getOrDefault(synergy.family(), 0) >= synergy.count()) {
				result.add(id);
			}
		});
		return result;
	}
}
