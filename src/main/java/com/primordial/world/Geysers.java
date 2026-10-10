package com.primordial.world;

import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Gêiseres hidrotermais no fundo do mar fundo: um respiro de areia de almas rodeado de magma, que lança a criatura
 * para cima numa coluna de bolhas. O calor machuca quem não aguenta; quem tem um Órgão Térmico colhe DNA raro.
 * Aparecem sozinhos (1 a cada ~50 trechos de mar fundo), sorteados só pela posição e pela semente.
 */
public final class Geysers {
	private static final int ODDS = 50;
	private static final Map<Identifier, List<BlockPos>> KNOWN = new HashMap<>();

	private Geysers() {
	}

	/** Roda ao varrer o trecho (cx, cz): constrói um respiro se o sorteio mandar e anota os que já existem. */
	public static void scan(ServerLevel level, int cx, int cz) {
		Identifier dim = level.dimension().identifier();
		int x = (cx << 4) + 5 + Math.floorMod(cx * 7 + cz * 3, 6);
		int z = (cz << 4) + 5 + Math.floorMod(cx * 5 + cz * 11, 6);
		int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
		BlockPos vent = new BlockPos(x, floor, z);
		if (level.getBlockState(vent).is(Blocks.SOUL_SAND) && level.getBlockState(vent.north()).is(Blocks.MAGMA_BLOCK)) {
			KNOWN.computeIfAbsent(dim, k -> new ArrayList<>()).add(vent);
			return;
		}
		long h = x * 341873128712L + z * 132897987541L + level.getSeed() * 17L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		if (Math.floorMod(h, (long) ODDS) != 0 || level.getSeaLevel() - floor < 20) {
			return;
		}
		for (int i = 1; i <= 14; i++) {
			if (!level.getFluidState(vent.above(i)).is(FluidTags.WATER)) {
				return;
			}
		}
		level.setBlock(vent, Blocks.SOUL_SAND.defaultBlockState(), 2);
		for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			level.setBlock(vent.relative(side), Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
		}
		for (int i = 1; i <= 14; i++) {
			level.setBlock(vent.above(i), Blocks.BUBBLE_COLUMN.defaultBlockState(), 2);
		}
		KNOWN.computeIfAbsent(dim, k -> new ArrayList<>()).add(vent);
	}

	/** O calor da água em volta: machuca quem não aguenta; com Órgão Térmico, rende DNA raro. */
	public static void tickPlayer(ServerLevel level, ServerPlayer player) {
		if (player.tickCount % 20 != 0) {
			return;
		}
		List<BlockPos> vents = KNOWN.get(level.dimension().identifier());
		if (vents == null) {
			return;
		}
		for (BlockPos vent : vents) {
			double dx = player.getX() - (vent.getX() + 0.5);
			double dz = player.getZ() - (vent.getZ() + 0.5);
			if (dx * dx + dz * dz < 9.0 && player.getY() > vent.getY() && player.getY() < vent.getY() + 12) {
				level.sendParticles(ParticleTypes.BUBBLE_POP, player.getX(), player.getY(), player.getZ(), 6, 0.4, 0.4, 0.4, 0.02);
				boolean resists = Creatures.hasPassive(player, "heat_adapt") || player.hasEffect(MobEffects.FIRE_RESISTANCE);
				if (resists) {
					CreatureService.addDna(player, 2);
				} else {
					player.hurtServer(level, player.damageSources().hotFloor(), 1.0F);
				}
				return;
			}
		}
	}
}
