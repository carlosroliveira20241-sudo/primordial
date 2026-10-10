package com.primordial.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Espalha "areia suspeita" e "cascalho de fóssil" pelo fundo do mar perto dos jogadores-criatura. O sorteio de onde
 * fica cada um depende só da posição e da semente do mundo; depois de escavado o bloco vira cascalho/argila e nunca é
 * sorteado de novo, então nada precisa ser guardado em disco.
 */
public final class SeabedSeeder {
	private static final int SAND_ODDS = 780;
	private static final int FOSSIL_ODDS = 1260;
	private static final int CHUNK_RADIUS = 3;

	private static final Map<Identifier, LongSet> SCANNED = new HashMap<>();
	private static final Map<Identifier, Long2ObjectOpenHashMap<List<BlockPos>>> KNOWN = new HashMap<>();

	private SeabedSeeder() {
	}

	public static boolean isDigSite(BlockState state) {
		return state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL);
	}

	/** Chamado por tick pelo jogador: varre 1 chunk novo por vez e espalha os sítios de escavação (sem brilho nenhum). */
	public static void tick(ServerLevel level, ServerPlayer player) {
		Identifier dim = level.dimension().identifier();
		if (player.tickCount % 10 == 0) {
			scanNextChunk(level, player, dim);
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
		List<BlockPos> found = new ArrayList<>();
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = (cx << 4) + dx;
				int z = (cz << 4) + dz;
				int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
				BlockPos pos = new BlockPos(x, y, z);
				BlockState state = level.getBlockState(pos);
				if (isDigSite(state)) {
					found.add(pos);
					continue;
				}
				if (!level.getFluidState(pos.above()).is(FluidTags.WATER) || !level.getFluidState(pos.above(2)).is(FluidTags.WATER)) {
					continue;
				}
				if (state.is(Blocks.SAND) && roll(level, pos, SAND_ODDS)) {
					level.setBlock(pos, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
					found.add(pos);
				} else if (state.is(Blocks.GRAVEL) && roll(level, pos, FOSSIL_ODDS)) {
					level.setBlock(pos, Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(), 2);
					found.add(pos);
				}
			}
		}
		if (!found.isEmpty()) {
			KNOWN.computeIfAbsent(dim, k -> new Long2ObjectOpenHashMap<>()).put(chunkKey, found);
		}
	}

	private static boolean roll(ServerLevel level, BlockPos pos, int odds) {
		long h = pos.getX() * 341873128712L + pos.getZ() * 132897987541L + pos.getY() * 4568135L + level.getSeed();
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return Math.floorMod(h, (long) odds) == 0;
	}
}
