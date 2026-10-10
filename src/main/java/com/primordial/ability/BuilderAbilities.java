package com.primordial.ability;

import com.primordial.builder.Builder;
import com.primordial.builder.TempBlocks;
import com.primordial.creature.CreatureService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Habilidades de construtor: pegar blocos moles do fundo para a bolsa ("baú de areia") e usá-los para erguer
 * coisas. As construções somem sozinhas depois de um tempo.
 */
final class BuilderAbilities {
	private BuilderAbilities() {
	}

	static void init() {
		registerMore();
		// Pegar Bloco: leva 1 bloco de areia, cascalho, argila, lama ou terra do que você olha para a bolsa.
		Abilities.register("pegar_areia", 15, (player, level) -> {
			ServerLevel world = player.level();
			BlockHitResult hit = Builder.looking(player, 5.5);
			if (hit == null || !Builder.canCarry(world.getBlockState(hit.getBlockPos()))) {
				message(player, "msg.primordial.builder_nothing");
				return;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = world.getBlockState(pos);
			int amount = Ability.awakened(level) ? 2 : 1;
			int got = Builder.add(player, state.getBlock(), amount);
			if (got == 0) {
				message(player, "msg.primordial.builder_full");
				return;
			}
			boolean wet = world.getFluidState(pos.above()).is(FluidTags.WATER) || world.getFluidState(pos.north()).is(FluidTags.WATER)
				|| world.getFluidState(pos.south()).is(FluidTags.WATER) || world.getFluidState(pos.east()).is(FluidTags.WATER) || world.getFluidState(pos.west()).is(FluidTags.WATER);
			world.setBlock(pos, wet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
			world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
			CreatureService.playSound(player, SoundEvents.SAND_BREAK, 0.8F, 1.1F);
		});

		// Moldar: coloca 1 bloco da bolsa onde você olha (some sozinho depois de 3 minutos; desperto, 10).
		Abilities.register("moldar_areia", 10, (player, level) -> {
			ServerLevel world = player.level();
			Block block = Builder.mostStocked(player);
			BlockHitResult hit = Builder.looking(player, 6.0);
			if (block == null) {
				message(player, "msg.primordial.builder_empty");
				return;
			}
			if (hit == null) {
				message(player, "msg.primordial.builder_nothing");
				return;
			}
			BlockPos target = hit.getBlockPos().relative(hit.getDirection());
			if (target.distSqr(player.blockPosition()) < 2 || !TempBlocks.place(world, target, Builder.buildState(block), Ability.awakened(level) ? 12000 : 3600)) {
				message(player, "msg.primordial.builder_blocked");
				return;
			}
			Builder.take(player, block);
			CreatureService.playSound(player, SoundEvents.SAND_PLACE, 0.8F, 1.0F);
		});

		// Erguer Muro: parede de 5x3 à sua frente (15 blocos).
		Abilities.register("erguer_muro", 200, (player, level) -> {
			ServerLevel world = player.level();
			Direction facing = player.getDirection();
			Direction side = facing.getClockWise();
			BlockPos center = player.blockPosition().relative(facing, 3);
			int ticks = Ability.awakened(level) ? 9000 : 1800;
			int placed = 0;
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = 0; dy < 3; dy++) {
					placed += tryPlace(player, world, center.relative(side, dx).above(dy), ticks);
				}
			}
			finish(player, placed, 15);
		});

		// Ponte: fileira de 8 blocos para frente, na altura dos seus pés.
		Abilities.register("ponte_de_areia", 160, (player, level) -> {
			ServerLevel world = player.level();
			Direction facing = player.getDirection();
			BlockPos start = player.blockPosition().below();
			int ticks = Ability.awakened(level) ? 6000 : 1200;
			int placed = 0;
			for (int i = 1; i <= 8; i++) {
				placed += tryPlace(player, world, start.relative(facing, i), ticks);
			}
			finish(player, placed, 8);
		});

		// Casulo: uma casca em volta de você (cerca de 40 blocos); protege por um tempo.
		Abilities.register("casulo_de_areia", 400, (player, level) -> {
			ServerLevel world = player.level();
			BlockPos center = player.blockPosition();
			int ticks = Ability.awakened(level) ? 800 : 400;
			int placed = 0;
			for (int dx = -2; dx <= 2; dx++) {
				for (int dy = -2; dy <= 3; dy++) {
					for (int dz = -2; dz <= 2; dz++) {
						int d2 = dx * dx + dz * dz + (dy - 1) * (dy - 1);
						if (d2 >= 3 && d2 <= 6) {
							placed += tryPlace(player, world, center.offset(dx, dy, dz), ticks);
						}
					}
				}
			}
			finish(player, placed, 20);
		});
	}

	private static void registerMore() {
		// Solidificar Areia: transforma a areia em volta de onde você olha em arenito, para sempre.
		Abilities.register("solidificar_areia", 100, (player, level) -> {
			ServerLevel world = player.level();
			BlockHitResult hit = Builder.looking(player, 6.5);
			if (hit == null) {
				message(player, "msg.primordial.builder_nothing");
				return;
			}
			int radius = Ability.awakened(level) ? 3 : 2;
			int changed = 0;
			for (BlockPos pos : BlockPos.betweenClosed(hit.getBlockPos().offset(-radius, -radius, -radius), hit.getBlockPos().offset(radius, radius, radius))) {
				BlockState state = world.getBlockState(pos);
				BlockState solid = state.is(Blocks.SAND) ? Blocks.SANDSTONE.defaultBlockState() : state.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState() : null;
				if (solid != null && changed < 60) {
					world.setBlock(pos, solid, 3);
					world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, solid), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
					changed++;
				}
			}
			if (changed == 0) {
				message(player, "msg.primordial.builder_no_sand");
				return;
			}
			CreatureService.playSound(player, SoundEvents.STONE_PLACE, 0.9F, 0.8F);
		});

		// Cavar Toca: abre um túnel de 3x3 e 5 de fundo nos blocos moles à frente; o material vai para a bolsa.
		Abilities.register("cavar_toca", 200, (player, level) -> {
			ServerLevel world = player.level();
			Direction facing = player.getDirection();
			Direction side = facing.getClockWise();
			BlockPos start = player.blockPosition().relative(facing, 1);
			int length = Ability.awakened(level) ? 8 : 5;
			int dug = 0;
			for (int i = 0; i < length; i++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = 0; dy < 3; dy++) {
						BlockPos pos = start.relative(facing, i).relative(side, dx).above(dy);
						BlockState state = world.getBlockState(pos);
						if (Builder.canCarry(state)) {
							Builder.add(player, state.getBlock(), 1);
							boolean wet = world.getFluidState(pos.above()).is(FluidTags.WATER);
							world.setBlock(pos, wet ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
							dug++;
						}
					}
				}
			}
			if (dug == 0) {
				message(player, "msg.primordial.builder_nothing");
				return;
			}
			world.sendParticles(ParticleTypes.POOF, start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5, 30, 0.8, 0.6, 0.8, 0.05);
			CreatureService.playSound(player, SoundEvents.GRAVEL_BREAK, 1.0F, 0.7F);
		});

		// Rede Pegajosa: uma rede de fios no chão onde você olha prende quem passar por 15 segundos.
		Abilities.register("rede_pegajosa", 160, (player, level) -> {
			ServerLevel world = player.level();
			BlockHitResult hit = Builder.looking(player, 8.0);
			if (hit == null) {
				message(player, "msg.primordial.builder_nothing");
				return;
			}
			BlockPos center = hit.getBlockPos().relative(hit.getDirection());
			int placed = 0;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (TempBlocks.place(world, center.offset(dx, 0, dz), Blocks.COBWEB.defaultBlockState(), Ability.awakened(level) ? 600 : 300)) {
						placed++;
					}
				}
			}
			if (placed == 0) {
				message(player, "msg.primordial.builder_blocked");
				return;
			}
			CreatureService.playSound(player, SoundEvents.SPIDER_AMBIENT, 0.8F, 1.2F);
		});

		// Farejar Tesouros: mostra com colunas de luz as escavações e Melhorias num raio de 24 blocos.
		Abilities.register("farejar_tesouros", 600, (player, level) -> {
			ServerLevel world = player.level();
			BlockPos origin = player.blockPosition();
			int found = 0;
			int radius = Ability.awakened(level) ? 40 : 24;
			for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -14, -radius), origin.offset(radius, 14, radius))) {
				if (found < 30 && com.primordial.world.SeabedSeeder.isDigSite(world.getBlockState(pos))) {
					world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 25, 0.15, 1.4, 0.15, 0.01);
					found++;
				}
			}
			for (net.minecraft.world.entity.item.ItemEntity item : world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(radius),
				e -> e.getItem().is(com.primordial.item.PrimordialItems.MORSEL))) {
				world.sendParticles(ParticleTypes.END_ROD, item.getX(), item.getY() + 0.6, item.getZ(), 20, 0.1, 1.0, 0.1, 0.01);
				found++;
			}
			player.sendSystemMessage(Component.translatable("msg.primordial.sniffed", found).withStyle(ChatFormatting.AQUA), true);
			CreatureService.playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.6F);
		});

		// Calmaria Mental: inimigos perto esquecem de você e ficam lentos por um momento.
		Abilities.register("calmaria_mental", 400, (player, level) -> {
			ServerLevel world = player.level();
			for (net.minecraft.world.entity.Mob mob : world.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(Ability.awakened(level) ? 16.0 : 12.0))) {
				mob.setTarget(null);
				mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100, 1));
			}
			world.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 0.8, player.getZ(), 20, 1.0, 0.6, 1.0, 1.0);
			CreatureService.playSound(player, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F, 0.8F);
		});
	}

	private static int tryPlace(ServerPlayer player, ServerLevel world, BlockPos pos, int ticks) {
		Block block = Builder.mostStocked(player);
		if (block == null || !TempBlocks.place(world, pos, Builder.buildState(block), ticks)) {
			return 0;
		}
		Builder.take(player, block);
		return 1;
	}

	private static void finish(ServerPlayer player, int placed, int wanted) {
		if (placed == 0) {
			message(player, Builder.mostStocked(player) == null ? "msg.primordial.builder_empty" : "msg.primordial.builder_blocked");
			return;
		}
		CreatureService.playSound(player, SoundEvents.SAND_PLACE, 1.0F, 0.8F);
		if (placed < wanted) {
			player.sendSystemMessage(Component.translatable("msg.primordial.builder_partial", placed, wanted).withStyle(ChatFormatting.YELLOW), true);
		}
	}

	private static void message(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
	}
}
