package com.primordial.journal;

import com.primordial.Primordial;
import com.primordial.data.JarData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class ChallengeRegistry {
	private static final Map<String, Challenge> CHALLENGES = new LinkedHashMap<>();

	private ChallengeRegistry() {
	}

	public static void load() {
		CHALLENGES.clear();
		for (Map.Entry<Identifier, Challenge> entry : JarData.load("challenges", Challenge.CODEC).entrySet()) {
			CHALLENGES.put(entry.getKey().getPath(), entry.getValue());
		}
		Primordial.LOGGER.info("Carreguei {} desafios", CHALLENGES.size());
	}

	public static Map<String, Challenge> all() {
		return Collections.unmodifiableMap(CHALLENGES);
	}
}
