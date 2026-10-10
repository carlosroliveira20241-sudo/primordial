package com.primordial.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.primordial.Primordial;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;

/**
 * Lê uma pasta inteira de JSON de dentro do .jar do mod ({@code data/primordial/<pasta>}). Cliente e servidor
 * leem os mesmos arquivos, então o conteúdo (partes, aparências, criaturas) é igual dos dois lados sem pacote.
 * Para criar conteúdo novo basta soltar um arquivo na pasta.
 */
public final class JarData {
	private JarData() {
	}

	public static <T> Map<Identifier, T> load(String folder, Codec<T> codec) {
		Map<Identifier, T> result = new LinkedHashMap<>();
		ModContainer mod = FabricLoader.getInstance().getModContainer(Primordial.MOD_ID).orElseThrow();
		Path dir = mod.findPath("data/" + Primordial.MOD_ID + "/" + folder)
			.orElseThrow(() -> new IllegalStateException("Pasta de dados não encontrada: " + folder));
		List<Path> files;
		try (Stream<Path> walk = Files.walk(dir)) {
			files = walk.filter(p -> p.toString().endsWith(".json")).sorted().toList();
		} catch (IOException e) {
			throw new IllegalStateException("Não consegui listar " + folder, e);
		}
		for (Path file : files) {
			String name = dir.relativize(file).toString().replace('\\', '/');
			name = name.substring(0, name.length() - ".json".length());
			Identifier id = Primordial.id(name);
			try (Reader reader = Files.newBufferedReader(file)) {
				JsonElement json = JsonParser.parseReader(reader);
				result.put(id, codec.parse(JsonOps.INSTANCE, json)
					.getOrThrow(msg -> new IllegalStateException(folder + "/" + id.getPath() + " inválido: " + msg)));
			} catch (IOException e) {
				throw new IllegalStateException("Não consegui ler " + folder + "/" + id.getPath(), e);
			}
		}
		return result;
	}
}
