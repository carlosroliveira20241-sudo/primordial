package com.primordial.creature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Comer plantas do fundo do mar (herbívoros e onívoros). */
public final class FeedingService {
	private FeedingService() {
	}

	public static boolean isPlant(BlockState state) {
		return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
	}

	public static void eatPlant(ServerPlayer player, BlockPos pos) {
		if (!Creatures.isRestricted(player) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36.0) {
			return;
		}
		ServerLevel level = player.level();
		if (!isPlant(level.getBlockState(pos))) {
			return;
		}
		Diet diet = Creatures.diet(player);
		if (!diet.eatsPlants()) {
			return;
		}
		level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
		level.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.0);
		CreatureService.playSound(player, SoundEvents.DOLPHIN_EAT, 0.5F, 1.4F);
		player.getFoodData().eat(1, 0.2F);
		CreatureService.addDna(player, Math.max(1, Mth.floor(2 * diet.plantEfficiency)));
		CreatureService.addXp(player, 2);
	}
}
