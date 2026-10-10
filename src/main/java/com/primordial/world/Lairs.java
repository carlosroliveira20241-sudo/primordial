package com.primordial.world;

import com.primordial.Primordial;
import com.primordial.entity.CreatureRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Covis do fundo do mar: ruínas de prismarinho com um chefe de guarda, um altar com luz e quatro fósseis raros que
 * brilham. Aparecem sozinhos perto dos jogadores, 1 a cada ~36 trechos de mar fundo. O sorteio depende só da posição
 * e da semente; o altar (bloco de prismarinho escuro no centro) marca que o covil já foi construído, então o chefe
 * nunca nasce duas vezes.
 */
public final class Lairs {
	private static final int ODDS = 36;
	private static final int R = 5;

	private Lairs() {
	}

	public static boolean isLairFossil(ServerLevel level, BlockPos fossil) {
		return level.getBlockState(fossil.below()).is(Blocks.DARK_PRISMARINE);
	}

	/** Tenta construir um covil no centro do trecho (cx, cz). Deve rodar ANTES de varrer o trecho. */
	public static void maybeBuild(ServerLevel level, int cx, int cz) {
		int x = (cx << 4) + 8;
		int z = (cz << 4) + 8;
		long h = x * 341873128712L + z * 132897987541L + level.getSeed() * 31L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		if (Math.floorMod(h, (long) ODDS) != 0) {
			return;
		}
		int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
		BlockPos center = new BlockPos(x, floor, z);
		if (level.getBlockState(center).is(Blocks.DARK_PRISMARINE)) {
			return;
		}
		for (int i = 1; i <= 10; i++) {
			if (!level.getFluidState(center.above(i)).is(FluidTags.WATER)) {
				return;
			}
		}
		build(level, center, Math.floorMod(h >>> 8, 10L) < 6);
	}

	private static void build(ServerLevel level, BlockPos c, boolean coralGuardian) {
		BlockState water = Blocks.WATER.defaultBlockState();
		for (int dx = -R; dx <= R; dx++) {
			for (int dz = -R; dz <= R; dz++) {
				BlockPos base = c.offset(dx, 0, dz);
				int edge = Math.max(Math.abs(dx), Math.abs(dz));
				level.setBlock(base, edge >= R - 1 ? Blocks.DARK_PRISMARINE.defaultBlockState() : Blocks.PRISMARINE_BRICKS.defaultBlockState(), 2);
				for (int up = 1; up <= 7; up++) {
					level.setBlock(base.above(up), water, 2);
				}
				// muralha com portões nos quatro lados
				if (edge == R && dx != 0 && dz != 0) {
					for (int up = 1; up <= 3; up++) {
						level.setBlock(base.above(up), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 2);
					}
				}
			}
		}
		// pilares nos cantos, com lanternas
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				BlockPos corner = c.offset(sx * R, 0, sz * R);
				for (int up = 1; up <= 6; up++) {
					level.setBlock(corner.above(up), Blocks.DARK_PRISMARINE.defaultBlockState(), 2);
				}
				level.setBlock(corner.above(7), Blocks.SEA_LANTERN.defaultBlockState(), 2);
			}
		}
		// altar no centro (o bloco do meio é a marca de "covil construído")
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(c.offset(dx, 0, dz), Blocks.DARK_PRISMARINE.defaultBlockState(), 2);
			}
		}
		level.setBlock(c.above(), Blocks.SEA_LANTERN.defaultBlockState(), 2);

		// quatro fósseis raros, que brilham, sobre prismarinho escuro
		int light = 12;
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				BlockPos site = c.offset(sx * 3, 0, sz * 3);
				level.setBlock(site, Blocks.DARK_PRISMARINE.defaultBlockState(), 2);
				level.setBlock(site.above(), Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), 2);
				level.setBlock(site.above(2), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, light)
					.setValue(BlockStateProperties.WATERLOGGED, true), 2);
			}
		}

		// o chefe guardião
		EntityType<?> type = CreatureRegistry.types().get(Primordial.id(coralGuardian ? "guardiao_de_coral" : "dunkleosteus"));
		if (type != null) {
			Entity boss = type.create(level, EntitySpawnReason.STRUCTURE);
			if (boss != null) {
				boss.setPos(c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5);
				level.addFreshEntity(boss);
			}
		}
	}
}
