package com.primordial.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.primordial.Primordial;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Configurações do host, em {@code config/primordial.json}. O arquivo é criado na primeira vez
 * com os valores padrão; é só editar e reiniciar o mundo.
 */
public final class PrimordialConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static PrimordialConfig instance = new PrimordialConfig();

	/** Modo competitivo: matar um jogador rouba DNA e pode derrubar uma Melhoria dele. */
	public boolean competitive = true;
	/** Fração do DNA não gasto que se perde ao morrer. */
	public double deathDnaLoss = 0.25;
	/** Fração do DNA não gasto da vítima que o assassino rouba. */
	public double pvpDnaSteal = 0.20;
	/** Chance de a vítima derrubar a Melhoria de uma das partes equipadas. */
	public double geneticTheftChance = 0.15;
	/** Segundos de invulnerabilidade ao renascer. */
	public int respawnProtectionSeconds = 10;
	/** Raio (blocos) da zona segura sem PvP em volta do ninho. */
	public int nestSafeRadius = 8;
	/** Chance base de uma criatura comum dropar a Melhoria. */
	public double morselDropChance = 0.35;
	/** Abates seguidos sem Melhoria até a próxima ser garantida. */
	public int pityKills = 6;
	/** Bloqueia os mobs vanilla no Overworld (só existem criaturas do mod). */
	public boolean blockVanillaMobsInOverworld = true;
	/** Bloqueia também no Nether e no End (deixe falso até existirem criaturas próprias lá). */
	public boolean blockVanillaMobsInOtherDimensions = false;
	/** Modo leve para PC fraco: menos criaturas e menos partículas. */
	public boolean lightMode = false;

	public static PrimordialConfig get() {
		return instance;
	}

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(Primordial.MOD_ID + ".json");
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				PrimordialConfig loaded = GSON.fromJson(reader, PrimordialConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				Primordial.LOGGER.error("Config inválida em {}, usando os valores padrão", file, e);
			}
		}
		try (Writer writer = Files.newBufferedWriter(file)) {
			GSON.toJson(instance, writer);
		} catch (IOException e) {
			Primordial.LOGGER.warn("Não consegui salvar {}", file, e);
		}
	}
}
