package com.primordial.entity;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Cria um tipo de entidade para cada arquivo de {@code data/primordial/creatures}. O JSON escolhe o comportamento
 * (a classe), o tamanho, a vida e onde nasce; a aparência vem do arquivo de {@code looks}.
 */
public final class PrimordialEntities {
	private PrimordialEntities() {
	}

	public static void init() {
		CreatureRegistry.defs().forEach(PrimordialEntities::create);
	}

	private static void create(Identifier id, CreatureDef def) {
		switch (def.behavior()) {
			case "cod" -> register(id, def, Cardume::new, plain(def));
			case "salmon" -> register(id, def, CardumeSalmao::new, plain(def));
			case "puffer" -> register(id, def, Baiacu::new, plain(def));
			case "fish" -> register(id, def, PeixeComum::new, plain(def));
			case "predator" -> register(id, def, Predador::new, Predador.attributes(def));
			default -> throw new IllegalStateException("Criatura " + id + ": comportamento desconhecido " + def.behavior());
		}
	}

	private static AttributeSupplier.Builder plain(CreatureDef def) {
		return AbstractFish.createAttributes().add(Attributes.MAX_HEALTH, def.health());
	}

	private static <T extends WaterAnimal> void register(Identifier id, CreatureDef def, EntityType.EntityFactory<T> factory, AttributeSupplier.Builder attributes) {
		MobCategory category = def.spawn().category().equals("creature") ? MobCategory.WATER_CREATURE : MobCategory.WATER_AMBIENT;
		if (def.spawn().biomes().equals("cave")) {
			category = MobCategory.UNDERGROUND_WATER_CREATURE;
		}
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.of(factory, category).sized(def.width(), def.height()).eyeHeight(def.height() * 0.65F)
				.clientTrackingRange(def.big() ? 8 : 5).build(key));
		CreatureRegistry.bind(id, type);

		FabricDefaultAttributeRegistry.register(type, attributes);
		boolean nightOnly = def.spawn().nightOnly();
		boolean cave = def.spawn().biomes().equals("cave");
		SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			(t, level, reason, pos, random) -> (!nightOnly || level.getLevel().isDarkOutside())
				&& (cave ? level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) && pos.getY() < level.getSeaLevel() - 10 && !level.canSeeSky(pos)
					: WaterAnimal.checkSurfaceWaterAnimalSpawnRules(t, level, reason, pos, random)));
		if (cave) {
			BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.UNDERGROUND_WATER_CREATURE, type,
				def.spawn().weight(), def.spawn().min(), def.spawn().max());
			return;
		}
		var biomes = def.spawn().biomes().equals("deep_ocean") ? BiomeTags.IS_DEEP_OCEAN : BiomeTags.IS_OCEAN;
		if (def.spawn().weight() <= 0 || def.has("event_only")) {
			return;
		}
		BiomeModifications.addSpawn(BiomeSelectors.tag(biomes), category, type, def.spawn().weight(), def.spawn().min(), def.spawn().max());
	}
}
