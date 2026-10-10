package com.primordial.intellect;

import com.primordial.Primordial;
import com.primordial.data.JarData;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class IntellectRegistry {
	private static final Map<Identifier, IntellectDef> ALL = new LinkedHashMap<>();

	private IntellectRegistry() {
	}

	public static void load() {
		ALL.clear();
		Map<Identifier, IntellectDef> loaded = JarData.load("intellect", IntellectDef.CODEC);
		loaded.entrySet().stream()
			.sorted(Comparator.comparingInt((Map.Entry<Identifier, IntellectDef> e) -> e.getValue().brain()).thenComparingInt(e -> e.getValue().cost()))
			.forEach(e -> ALL.put(e.getKey(), e.getValue()));
		Primordial.LOGGER.info("Carreguei {} habilidades de inteligência", ALL.size());
	}

	public static @Nullable IntellectDef get(Identifier id) {
		return ALL.get(id);
	}

	public static Map<Identifier, IntellectDef> all() {
		return Collections.unmodifiableMap(ALL);
	}
}
