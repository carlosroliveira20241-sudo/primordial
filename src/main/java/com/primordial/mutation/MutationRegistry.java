package com.primordial.mutation;

import com.primordial.Primordial;
import com.primordial.data.JarData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class MutationRegistry {
	private static final Map<Identifier, Mutation> MUTATIONS = new LinkedHashMap<>();

	private MutationRegistry() {
	}

	public static void load() {
		MUTATIONS.clear();
		MUTATIONS.putAll(JarData.load("mutations", Mutation.CODEC));
		Primordial.LOGGER.info("Carreguei {} mutações", MUTATIONS.size());
	}

	public static @Nullable Mutation get(Identifier id) {
		return MUTATIONS.get(id);
	}

	public static Map<Identifier, Mutation> all() {
		return Collections.unmodifiableMap(MUTATIONS);
	}
}
