package com.primordial.look;

import com.primordial.Primordial;
import com.primordial.data.JarData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class LookRegistry {
	private static final Map<Identifier, Look> LOOKS = new LinkedHashMap<>();

	private LookRegistry() {
	}

	public static void load() {
		LOOKS.clear();
		LOOKS.putAll(JarData.load("looks", Look.CODEC));
		if (!LOOKS.containsKey(Look.DEFAULT_ID)) {
			throw new IllegalStateException("Falta a aparência padrão " + Look.DEFAULT_ID);
		}
		Primordial.LOGGER.info("Carreguei {} aparências", LOOKS.size());
	}

	/** Nunca devolve nulo: id desconhecido cai na aparência padrão. */
	public static Look get(Identifier id) {
		Look look = LOOKS.get(id);
		return look != null ? look : LOOKS.get(Look.DEFAULT_ID);
	}

	public static boolean exists(Identifier id) {
		return LOOKS.containsKey(id);
	}

	public static Map<Identifier, Look> all() {
		return Collections.unmodifiableMap(LOOKS);
	}

	public static List<Identifier> selectable() {
		List<Identifier> result = new ArrayList<>();
		LOOKS.forEach((id, look) -> {
			if (look.selectable()) {
				result.add(id);
			}
		});
		return result;
	}
}
