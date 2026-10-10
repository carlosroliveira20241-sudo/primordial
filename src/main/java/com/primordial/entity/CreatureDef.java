package com.primordial.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Uma criatura do mod, lida de {@code data/primordial/creatures/<nome>.json}. Para criar uma nova basta o JSON
 * (mais o nome no idioma e, se quiser que ela dropa uma parte, a parte com {@code "source"}).
 *
 * @param behavior   qual comportamento usar: "cod" (cardume), "salmon", "puffer", "fish" (peixe comum) ou "predator"
 * @param traits     poderes extras: "glow", "ink" (solta tinta ao apanhar), "shock_touch" (choca quem encosta), "shock_hit" (ataque choca)
 * @param power      predador: escala de corpo máxima dos jogadores que ele caça (ver Stage.scale)
 * @param look       aparência (looks/*.json)
 * @param big        conta como "predador grande" para evoluir de Peixe para Predador
 */
public record CreatureDef(
	String behavior,
	List<Float> size,
	double health,
	double damage,
	double speed,
	float power,
	List<String> traits,
	Identifier look,
	boolean big,
	Spawn spawn
) {
	public static final Codec<CreatureDef> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("behavior").forGetter(CreatureDef::behavior),
		Codec.FLOAT.listOf().optionalFieldOf("size", List.of(0.5F, 0.4F)).forGetter(CreatureDef::size),
		Codec.DOUBLE.optionalFieldOf("health", 3.0).forGetter(CreatureDef::health),
		Codec.DOUBLE.optionalFieldOf("damage", 2.0).forGetter(CreatureDef::damage),
		Codec.DOUBLE.optionalFieldOf("speed", 0.5).forGetter(CreatureDef::speed),
		Codec.FLOAT.optionalFieldOf("power", 0.5F).forGetter(CreatureDef::power),
		Codec.STRING.listOf().optionalFieldOf("traits", List.of()).forGetter(CreatureDef::traits),
		Identifier.CODEC.fieldOf("look").forGetter(CreatureDef::look),
		Codec.BOOL.optionalFieldOf("big", false).forGetter(CreatureDef::big),
		Spawn.CODEC.fieldOf("spawn").forGetter(CreatureDef::spawn)
	).apply(i, CreatureDef::new));

	/** @param biomes "ocean" ou "deep_ocean"; @param category "ambient" ou "creature" */
	public record Spawn(String biomes, String category, int weight, int min, int max) {
		public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("biomes", "ocean").forGetter(Spawn::biomes),
			Codec.STRING.optionalFieldOf("category", "ambient").forGetter(Spawn::category),
			Codec.INT.optionalFieldOf("weight", 10).forGetter(Spawn::weight),
			Codec.INT.optionalFieldOf("min", 1).forGetter(Spawn::min),
			Codec.INT.optionalFieldOf("max", 3).forGetter(Spawn::max)
		).apply(i, Spawn::new));
	}

	public float width() {
		return this.size.size() > 0 ? this.size.get(0) : 0.5F;
	}

	public float height() {
		return this.size.size() > 1 ? this.size.get(1) : 0.4F;
	}

	public boolean has(String trait) {
		return this.traits.contains(trait);
	}
}
