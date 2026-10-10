package com.primordial.part;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.primordial.Primordial;
import com.primordial.creature.SlotType;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Todas as partes do mod. São lidas direto dos arquivos do próprio .jar ({@code data/primordial/parts}),
 * então cliente e servidor sempre têm a mesma lista sem precisar mandar pacote.
 */
public final class PartRegistry {
	private static final Map<Identifier, PartDefinition> PARTS = new LinkedHashMap<>();

	private PartRegistry() {
	}

	public static void load() {
		PARTS.clear();
		ModContainer mod = FabricLoader.getInstance().getModContainer(Primordial.MOD_ID).orElseThrow();
		Path dir = mod.findPath("data/" + Primordial.MOD_ID + "/parts").orElseThrow(() -> new IllegalStateException("Pasta de partes não encontrada"));
		List<Path> files;
		try (Stream<Path> walk = Files.walk(dir)) {
			files = walk.filter(p -> p.toString().endsWith(".json")).sorted().toList();
		} catch (IOException e) {
			throw new IllegalStateException("Não consegui listar as partes", e);
		}

		for (Path file : files) {
			String name = dir.relativize(file).toString().replace('\\', '/');
			name = name.substring(0, name.length() - ".json".length());
			Identifier id = Primordial.id(name);
			try (Reader reader = Files.newBufferedReader(file)) {
				JsonElement json = JsonParser.parseReader(reader);
				PartDefinition def = PartDefinition.CODEC.parse(JsonOps.INSTANCE, json)
					.getOrThrow(msg -> new IllegalStateException("Parte " + id + " inválida: " + msg));
				PARTS.put(id, def);
			} catch (IOException e) {
				throw new IllegalStateException("Não consegui ler a parte " + id, e);
			}
		}
		Primordial.LOGGER.info("Carreguei {} partes do corpo", PARTS.size());
	}

	public static @Nullable PartDefinition get(Identifier id) {
		return PARTS.get(id);
	}

	public static boolean exists(Identifier id) {
		return PARTS.containsKey(id);
	}

	public static Map<Identifier, PartDefinition> all() {
		return Collections.unmodifiableMap(PARTS);
	}

	public static List<Identifier> ofType(SlotType type) {
		List<Identifier> result = new ArrayList<>();
		PARTS.forEach((id, def) -> {
			if (def.slot() == type) {
				result.add(id);
			}
		});
		return result;
	}

	/** Partes que uma espécie pode dropar como Melhoria. */
	public static List<Identifier> droppedBy(Identifier species) {
		List<Identifier> result = new ArrayList<>();
		PARTS.forEach((id, def) -> {
			if (!def.base() && def.source().filter(species::equals).isPresent()) {
				result.add(id);
			}
		});
		return result;
	}
}
