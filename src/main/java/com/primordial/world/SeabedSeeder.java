package com.primordial.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Espalha os sítios de escavação perto dos jogadores-criatura:
 * <ul>
 * <li>"areia suspeita" e "cascalho de fóssil" no fundo do mar (sem brilho nenhum);</li>
 * <li>"fósseis de caverna" dentro das cavernas submersas: quanto mais fundo, mais raros e melhores,
 * e eles brilham (um bloco de luz invisível na água em cima e partículas).</li>
 * </ul>
 * O sorteio depende só da posição e da semente do mundo; depois de escavado o bloco vira cascalho/argila e nunca é
 * sorteado de novo, então nada precisa ser guardado em disco.
 */
public final class SeabedSeeder {
	private static final int SAND_ODDS = 780;
	private static final int FOSSIL_ODDS = 1260;
	private static final int CHUNK_RADIUS = 3;
	/** Profundidade mínima abaixo do nível do mar para contar como caverna. */
	private static final int CAVE_DEPTH = 10;

	private static final Map<Identifier, LongSet> SCANNED = new HashMap<>();
	private static final Map<Identifier, Long2ObjectOpenHashMap<List<BlockPos>>> GLOWING = new HashMap<>();

	private SeabedSeeder() {
	}

	public static boolean isDigSite(BlockState state) {
		return state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL);
	}

	/** Fóssil de caverna = escavação com um bloco de luz invisível em cima. */
	public static boolean isCaveFossil(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).is(Blocks.SUSPICIOUS_GRAVEL) && level.getBlockState(pos.above()).is(Blocks.LIGHT);
	}

	/** 0 = fundo do mar; 1 a 3 = caverna, cada vez mais fundo (e melhor). */
	public static int caveTier(ServerLevel level, BlockPos pos) {
		if (!isCaveFossil(level, pos)) {
			return 0;
		}
		if (Lairs.isLairFossil(level, pos)) {
			return 3;
		}
		int depth = level.getSeaLevel() - pos.getY();
		return depth >= 60 ? 3 : depth >= 30 ? 2 : 1;
	}

	/** Chamado por tick pelo jogador: varre 1 chunk novo por vez; só os fósseis de caverna soltam brilhos. */
	public static void tick(ServerLevel level, ServerPlayer player) {
		Identifier dim = level.dimension().identifier();
		if (player.tickCount % 10 == 0) {
			scanNextChunk(level, player, dim);
		}
		if (player.tickCount % 20 == 0) {
			glow(level, player, dim);
		}
	}

	private static void scanNextChunk(ServerLevel level, ServerPlayer player, Identifier dim) {
		LongSet scanned = SCANNED.computeIfAbsent(dim, k -> new LongOpenHashSet());
		int cx = player.blockPosition().getX() >> 4;
		int cz = player.blockPosition().getZ() >> 4;
		for (int r = 0; r <= CHUNK_RADIUS; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					long key = ChunkPos.pack(cx + dx, cz + dz);
					if (scanned.contains(key) || level.getChunkSource().getChunkNow(cx + dx, cz + dz) == null) {
						continue;
					}
					scanned.add(key);
					scanChunk(level, dim, cx + dx, cz + dz, key);
					return;
				}
			}
		}
	}

	private static void scanChunk(ServerLevel level, Identifier dim, int cx, int cz, long chunkKey) {
		Lairs.maybeBuild(level, cx, cz);
		List<BlockPos> glowing = new ArrayList<>();
		int sea = level.getSeaLevel();
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = (cx << 4) + dx;
				int z = (cz << 4) + dz;
				int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
				BlockPos pos = new BlockPos(x, y, z);
				BlockState state = level.getBlockState(pos);
				if (isDigSite(state)) {
					if (isCaveFossil(level, pos)) {
						glowing.add(pos);
					}
				} else if (level.getFluidState(pos.above()).is(FluidTags.WATER) && level.getFluidState(pos.above(2)).is(FluidTags.WATER)) {
					if (state.is(Blocks.SAND) && roll(level, pos, SAND_ODDS)) {
						level.setBlock(pos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
					} else if (state.is(Blocks.GRAVEL) && roll(level, pos, FOSSIL_ODDS)) {
						level.setBlock(pos, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), 2);
					}
				}
				scanCaves(level, x, z, y, sea, glowing);
			}
		}
		if (!glowing.isEmpty()) {
			GLOWING.computeIfAbsent(dim, k -> new Long2ObjectOpenHashMap<>()).put(chunkKey, glowing);
		}
	}

	/** Procura o chão de cavernas cheias de água nesta coluna. Quanto mais fundo, mais raro o fóssil. */
	private static void scanCaves(ServerLevel level, int x, int z, int seabed, int sea, List<BlockPos> glowing) {
		if (!roll(level, new BlockPos(x, 0, z), 4)) {
			return;
		}
		int minY = level.getMinY() + 6;
		int found = 0;
		for (int y = Math.min(seabed - 6, sea - CAVE_DEPTH); y > minY && found < 2; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState floor = level.getBlockState(pos);
			if (floor.isAir() || !floor.getFluidState().isEmpty() || !floor.isCollisionShapeFullBlock(level, pos)) {
				continue;
			}
			if (!level.getFluidState(pos.above()).is(FluidTags.WATER) || !level.getFluidState(pos.above(2)).is(FluidTags.WATER)
				|| level.canSeeSky(pos.above())) {
				continue;
			}
			found++;
			int depth = sea - y;
			if (roll(level, pos, 60 + (int) (depth * 1.5))) {
				level.setBlock(pos, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), 2);
				int light = Math.min(12, 6 + depth / 20);
				level.setBlock(pos.above(), Blocks.LIGHT.defaultBlockState()
					.setValue(LightBlock.LEVEL, light).setValue(BlockStateProperties.WATERLOGGED, true), 2);
				glowing.add(pos);
			}
		}
	}

	private static boolean roll(ServerLevel level, BlockPos pos, int odds) {
		long h = pos.getX() * 341873128712L + pos.getZ() * 132897987541L + pos.getY() * 4568135L + level.getSeed();
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return Math.floorMod(h, (long) odds) == 0;
	}

	/** Só os fósseis de caverna brilham: partículas por perto, para dar vontade de explorar. */
	private static void glow(ServerLevel level, ServerPlayer player, Identifier dim) {
		Long2ObjectOpenHashMap<List<BlockPos>> known = GLOWING.get(dim);
		if (known == null) {
			return;
		}
		int cx = player.blockPosition().getX() >> 4;
		int cz = player.blockPosition().getZ() >> 4;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				List<BlockPos> list = known.get(ChunkPos.pack(cx + dx, cz + dz));
				if (list == null) {
					continue;
				}
				for (BlockPos pos : list) {
					if (isCaveFossil(level, pos) && pos.distSqr(player.blockPosition()) < 32 * 32) {
						level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 2, 0.35, 0.3, 0.35, 0.0);
					}
				}
			}
		}
	}
}
