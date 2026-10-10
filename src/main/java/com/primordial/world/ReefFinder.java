package com.primordial.world;

import com.mojang.datafixers.util.Pair;
import com.primordial.Primordial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/** Acha o "berçário": o recife de coral (oceano quente) mais próximo, para a criatura nascer. */
public final class ReefFinder {
	private static final int SEARCH_RADIUS = 6400;

	private ReefFinder() {
	}

	public static @Nullable BlockPos findReef(ServerLevel level, BlockPos origin) {
		BlockPos found = find(level, origin, b -> b.is(Biomes.WARM_OCEAN));
		if (found == null) {
			found = find(level, origin, b -> b.is(Biomes.LUKEWARM_OCEAN) || b.is(Biomes.DEEP_LUKEWARM_OCEAN));
		}
		if (found == null) {
			found = find(level, origin, b -> b.is(BiomeTags.IS_OCEAN));
		}
		if (found == null) {
			Primordial.LOGGER.warn("Nenhum oceano encontrado perto de {}", origin);
			return null;
		}
		return waterSpot(level, found);
	}

	private static @Nullable BlockPos find(ServerLevel level, BlockPos origin, java.util.function.Predicate<Holder<Biome>> test) {
		Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(test, origin, SEARCH_RADIUS, 32, 64);
		return result == null ? null : result.getFirst();
	}

	/** Um bloco de água logo acima do fundo, sem passar da superfície. */
	public static BlockPos waterSpot(ServerLevel level, BlockPos column) {
		level.getChunk(column.getX() >> 4, column.getZ() >> 4);
		int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, column.getX(), column.getZ());
		int sea = level.getSeaLevel();
		int y = Math.min(floor + 2, sea - 2);
		BlockPos pos = new BlockPos(column.getX(), Math.max(y, floor), column.getZ());
		if (!level.getFluidState(pos).is(FluidTags.WATER)) {
			pos = new BlockPos(column.getX(), sea - 1, column.getZ());
		}
		return pos;
	}
}
