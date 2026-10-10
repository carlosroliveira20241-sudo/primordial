package com.primordial.entity;

import com.primordial.Primordial;
import com.primordial.data.JarData;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/** Todas as criaturas do mod (lidas de {@code data/primordial/creatures}) e os tipos de entidade criados para elas. */
public final class CreatureRegistry {
	private static final Map<Identifier, CreatureDef> DEFS = new LinkedHashMap<>();
	private static final Map<Identifier, EntityType<?>> TYPES = new LinkedHashMap<>();

	private CreatureRegistry() {
	}

	public static void load() {
		DEFS.clear();
		DEFS.putAll(JarData.load("creatures", CreatureDef.CODEC));
		Primordial.LOGGER.info("Carreguei {} criaturas", DEFS.size());
	}

	static void bind(Identifier id, EntityType<?> type) {
		TYPES.put(id, type);
	}

	public static Map<Identifier, CreatureDef> defs() {
		return Collections.unmodifiableMap(DEFS);
	}

	public static Map<Identifier, EntityType<?>> types() {
		return Collections.unmodifiableMap(TYPES);
	}

	public static @Nullable CreatureDef get(Identifier id) {
		return DEFS.get(id);
	}

	public static @Nullable CreatureDef of(EntityType<?> type) {
		return DEFS.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
	}

	/** Criatura "grande" (conta para evoluir de Peixe para Predador). */
	public static boolean isBig(EntityType<?> type) {
		CreatureDef def = of(type);
		return def != null && def.big();
	}
}
