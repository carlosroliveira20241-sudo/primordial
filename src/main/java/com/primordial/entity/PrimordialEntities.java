package com.primordial.entity;

import com.primordial.Primordial;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Criaturas do protótipo. Por enquanto reaproveitam o corpo, a IA e o modelo dos peixes vanilla
 * (bacalhau, salmão e baiacu); o que muda é quem elas são no jogo (nome, spawn, Melhoria que dropam).
 */
public final class PrimordialEntities {
	public static final EntityType<SardinhaPrata> SARDINHA_PRATA = register("sardinha_prata",
		EntityType.Builder.of(SardinhaPrata::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.3F).eyeHeight(0.195F).clientTrackingRange(4));
	public static final EntityType<PeixePapagaio> PEIXE_PAPAGAIO = register("peixe_papagaio",
		EntityType.Builder.of(PeixePapagaio::new, MobCategory.WATER_AMBIENT).sized(0.7F, 0.4F).eyeHeight(0.26F).clientTrackingRange(4));
	public static final EntityType<Baiacu> BAIACU = register("baiacu",
		EntityType.Builder.of(Baiacu::new, MobCategory.WATER_AMBIENT).sized(0.7F, 0.7F).eyeHeight(0.455F).clientTrackingRange(4));

	private PrimordialEntities() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Primordial.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	private static <T extends Mob> void spawn(EntityType<T> type, SpawnPlacements.SpawnPredicate<T> rules, int weight, int min, int max) {
		FabricDefaultAttributeRegistry.register(type, AbstractFish.createAttributes());
		SpawnPlacements.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rules);
		BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), MobCategory.WATER_AMBIENT, type, weight, min, max);
	}

	public static void init() {
		spawn(SARDINHA_PRATA, WaterAnimal::checkSurfaceWaterAnimalSpawnRules, 25, 3, 6);
		spawn(PEIXE_PAPAGAIO, WaterAnimal::checkSurfaceWaterAnimalSpawnRules, 15, 2, 4);
		spawn(BAIACU, WaterAnimal::checkSurfaceWaterAnimalSpawnRules, 8, 1, 3);
	}
}
