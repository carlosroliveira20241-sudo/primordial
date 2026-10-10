package com.primordial.world;

import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.creature.FeedingService;
import com.primordial.event.MorselDrops;
import com.primordial.creature.CreatureProgress;
import com.primordial.part.PartRarity;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * O que a criatura faz com o mundo no lugar de quebrar e construir: morder algas (comer), escavar areia suspeita e
 * cascalho de fóssil (achar Melhorias), roer coral (DNA) e plantar algas para formar um jardim.
 */
public final class WorldInteractions {
	private static final int SAND_HITS = 4;
	private static final int FOSSIL_HITS = 6;
	private static final Map<Long, Integer> HITS = new HashMap<>();
	private static final Map<UUID, Long> LAST_PLANT = new HashMap<>();

	private WorldInteractions() {
	}

	/** O jogador-criatura "atacou" um bloco: o servidor decide o que a mordida faz. */
	public static void bite(ServerPlayer player, BlockPos pos) {
		if (!Creatures.isRestricted(player) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36.0) {
			return;
		}
		ServerLevel level = player.level();
		BlockState state = level.getBlockState(pos);
		if (FeedingService.isPlant(state)) {
			FeedingService.eatPlant(player, pos);
		} else if (SeabedSeeder.isDigSite(state)) {
			dig(player, level, pos, state);
		} else if (state.is(BlockTags.CORAL_BLOCKS) || state.is(BlockTags.CORALS) || state.is(BlockTags.WALL_CORALS)) {
			gnawCoral(player, level, pos);
		}
	}

	private static void dig(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
		boolean fossil = state.is(Blocks.SUSPICIOUS_GRAVEL);
		if (fossil && Creatures.body(player).stage().number() < 2) {
			player.sendSystemMessage(Component.translatable("msg.primordial.dig_too_small").withStyle(ChatFormatting.RED), true);
			return;
		}
		int needed = fossil ? FOSSIL_HITS : SAND_HITS;
		long key = pos.asLong();
		int hits = HITS.merge(key, 1, Integer::sum);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.05);
		CreatureService.playSound(player, SoundEvents.SAND_BREAK, 0.8F, 0.8F + hits * 0.1F);
		if (hits < needed) {
			player.sendSystemMessage(Component.translatable("msg.primordial.dig_progress", hits, needed).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		HITS.remove(key);
		level.setBlock(pos, fossil ? Blocks.CLAY.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), 3);
		com.primordial.journal.JournalService.bump(player, "dig", 1);
		reward(player, level, pos, fossil);
	}

	private static void reward(ServerPlayer player, ServerLevel level, BlockPos pos, boolean fossil) {
		double roll = player.getRandom().nextDouble();
		CreatureProgress progress = Creatures.progress(player);
		if (roll < (fossil ? 0.8 : 0.55)) {
			List<Identifier> pool = new ArrayList<>();
			for (Identifier id : PartRegistry.droppable()) {
				boolean rare = PartRegistry.get(id).rarity() != PartRarity.COMMON;
				if ((!fossil || rare) && !progress.unlocked(id)) {
					pool.add(id);
				}
			}
			if (pool.isEmpty()) {
				for (Identifier id : PartRegistry.droppable()) {
					if (!fossil || PartRegistry.get(id).rarity() != PartRarity.COMMON) {
						pool.add(id);
					}
				}
			}
			if (!pool.isEmpty()) {
				Identifier part = pool.get(player.getRandom().nextInt(pool.size()));
				MorselDrops.spawnMorsel(level, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, part);
				player.sendSystemMessage(Component.translatable(fossil ? "msg.primordial.dig_found_fossil" : "msg.primordial.dig_found_morsel").withStyle(ChatFormatting.GOLD), true);
				return;
			}
		}
		if (roll < (fossil ? 0.95 : 0.85)) {
			int dna = (fossil ? 30 : 10) + player.getRandom().nextInt(fossil ? 20 : 15);
			CreatureService.addDna(player, dna);
			player.sendSystemMessage(Component.translatable("msg.primordial.dig_found_dna", dna).withStyle(ChatFormatting.AQUA), true);
		} else {
			CreatureService.addXp(player, fossil ? 60 : 25);
			CreatureService.addDna(player, 5);
			player.sendSystemMessage(Component.translatable("msg.primordial.dig_found_xp").withStyle(ChatFormatting.GREEN), true);
		}
		level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
	}

	private static void gnawCoral(ServerPlayer player, ServerLevel level, BlockPos pos) {
		int power = Creatures.passiveLevel(player, "coral_breaker");
		if (power <= 0) {
			player.sendSystemMessage(Component.translatable("msg.primordial.coral_needs_beak").withStyle(ChatFormatting.RED), true);
			return;
		}
		level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
		level.sendParticles(ParticleTypes.BUBBLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.05);
		CreatureService.playSound(player, SoundEvents.CORAL_BLOCK_BREAK, 0.8F, 1.2F);
		com.primordial.journal.JournalService.bump(player, "coral", 1);
		CreatureService.addDna(player, 2 + power);
		CreatureService.addXp(player, 2);
	}

	/** Planta alga (ou kelp, se for fundo) no chão em que você está olhando. Custa 1 DNA. */
	public static void plant(ServerPlayer player) {
		if (!Creatures.isRestricted(player)) {
			return;
		}
		ServerLevel level = player.level();
		long now = level.getGameTime();
		if (now - LAST_PLANT.getOrDefault(player.getUUID(), -100L) < 30) {
			return;
		}
		HitResult hit = player.pick(6.0, 0.0F, false);
		if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockPos base = block.getBlockPos();
		BlockPos target = base.above();
		BlockState ground = level.getBlockState(base);
		boolean soil = ground.is(BlockTags.SAND) || ground.is(Blocks.GRAVEL) || ground.is(Blocks.CLAY) || ground.is(Blocks.MUD) || ground.is(BlockTags.DIRT);
		if (!soil || !level.getBlockState(target).is(Blocks.WATER)) {
			player.sendSystemMessage(Component.translatable("msg.primordial.plant_where").withStyle(ChatFormatting.RED), true);
			return;
		}
		if (Creatures.progress(player).dna() < 1) {
			player.sendSystemMessage(Component.translatable("msg.primordial.not_enough_dna", 1).withStyle(ChatFormatting.RED), true);
			return;
		}
		LAST_PLANT.put(player.getUUID(), now);
		boolean deep = level.getBlockState(target.above()).is(Blocks.WATER) && level.getBlockState(target.above(2)).is(Blocks.WATER);
		level.setBlock(target, (deep ? Blocks.KELP : Blocks.SEAGRASS).defaultBlockState(), 3);
		CreatureService.setProgress(player, Creatures.progress(player).withDna(Creatures.progress(player).dna() - 1));
		com.primordial.journal.JournalService.bump(player, "plant", 1);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
		CreatureService.playSound(player, SoundEvents.WET_GRASS_PLACE, 0.8F, 1.0F);
	}
}
