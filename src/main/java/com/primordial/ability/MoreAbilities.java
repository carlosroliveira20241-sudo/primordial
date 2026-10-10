package com.primordial.ability;

import com.primordial.world.TimedBlocks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Habilidades de ambiente e de caça: luz, esconderijo, gelo, flores do mar, sonar de minérios, marca e frenesi. */
public final class MoreAbilities {
	private static final Map<UUID, Long> AMBUSH = new HashMap<>();
	private static final Map<UUID, Long[]> MARKS = new HashMap<>();
	private static final Map<UUID, UUID> MARK_OWNER = new HashMap<>();

	private MoreAbilities() {
	}

	/** Se o jogador estava enterrado, o primeiro golpe dele dá dano dobrado e o esconderijo termina. */
	public static boolean consumeAmbush(ServerPlayer player) {
		Long until = AMBUSH.remove(player.getUUID());
		if (until != null && player.level().getGameTime() < until) {
			Hiding.reveal(player);
			player.removeEffect(MobEffects.INVISIBILITY);
			player.removeEffect(MobEffects.SLOWNESS);
			return true;
		}
		return false;
	}

	/** Bônus de dano (fração extra) quando o alvo está marcado por este jogador. */
	public static float markBonus(ServerPlayer hitter, LivingEntity victim) {
		UUID owner = MARK_OWNER.get(victim.getUUID());
		Long[] expires = MARKS.get(victim.getUUID());
		if (owner != null && expires != null && owner.equals(hitter.getUUID()) && victim.level().getGameTime() < expires[0]) {
			return 0.3F;
		}
		return 0.0F;
	}

	public static void init() {
		// Sinalizador: uma esfera de luz presa onde você olha ilumina o local por 2 minutos.
		Abilities.register("sinalizador", 400, (player, level) -> {
			ServerLevel world = player.level();
			BlockPos spot = lookedWaterSpot(player, world, 24.0);
			if (spot == null) {
				message(player, "msg.primordial.look_water");
				return;
			}
			BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15).setValue(BlockStateProperties.WATERLOGGED, true);
			if (TimedBlocks.place(world, spot, light, Ability.awakened(level) ? 4800 : 2400)) {
				world.sendParticles(ParticleTypes.GLOW, spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.05);
				Abilities.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
			} else {
				message(player, "msg.primordial.cant_here");
			}
		});

		// Enterrar-se: some no fundo; o próximo golpe sai de surpresa com dano dobrado.
		Abilities.register("enterrar_se", 300, (player, level) -> {
			int ticks = Ability.awakened(level) ? 240 : 160;
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 9, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 1, false, false, true));
			Hiding.hide(player, ticks);
			AMBUSH.put(player.getUUID(), player.level().getGameTime() + ticks);
			Abilities.burst(player.level(), ParticleTypes.POOF, player, 30, 0.05);
			Abilities.sound(player, SoundEvents.SAND_BREAK, 1.0F, 0.6F);
		});

		// Fingir de Morto: os bichos perdem o interesse; desperto, você ainda levanta regenerando.
		Abilities.register("fingir_de_morto", 500, (player, level) -> {
			ServerLevel world = player.level();
			int ticks = 200;
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 9, false, false, true));
			Hiding.hide(player, ticks);
			for (Mob mob : world.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0))) {
				if (mob.getTarget() == player) {
					mob.setTarget(null);
				}
			}
			if (Ability.awakened(level)) {
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks + 100, 1));
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks + 100, 1));
			}
			Abilities.burst(world, ParticleTypes.SMOKE, player, 30, 0.02);
			Abilities.sound(player, SoundEvents.WARDEN_SNIFF, 0.6F, 1.5F);
		});

		// Sopro Congelante: cone gelado que machuca e quase para quem for atingido.
		Abilities.register("sopro_congelante", 180, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 look = player.getLookAngle();
			double reach = Ability.awakened(level) ? 8.0 : 6.0;
			for (LivingEntity target : Abilities.around(world, player, reach)) {
				Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(player.getEyePosition()).normalize();
				if (to.dot(look) > 0.6) {
					target.hurtServer(world, player.damageSources().playerAttack(player), 2.0F + level);
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, Ability.awakened(level) ? 5 : 3));
					target.setTicksFrozen(200);
				}
			}
			for (int i = 1; i <= 6; i++) {
				Vec3 at = player.getEyePosition().add(look.scale(i * reach / 6.0));
				world.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 8, 0.3 + i * 0.12, 0.3 + i * 0.12, 0.3 + i * 0.12, 0.02);
			}
			Abilities.sound(player, SoundEvents.POWDER_SNOW_PLACE, 1.0F, 0.8F);
		});

		// Florescer: faz algas e capim-marinho brotarem em volta de você (comida e esconderijo).
		Abilities.register("florescer", 600, (player, level) -> {
			ServerLevel world = player.level();
			int limit = Ability.awakened(level) ? 30 : 14;
			int planted = 0;
			BlockPos origin = player.blockPosition();
			for (int dx = -6; dx <= 6 && planted < limit; dx++) {
				for (int dz = -6; dz <= 6 && planted < limit; dz++) {
					for (int dy = -6; dy <= 3; dy++) {
						BlockPos floor = origin.offset(dx, dy, dz);
						BlockPos above = floor.above();
						BlockState ground = world.getBlockState(floor);
						boolean soil = ground.is(BlockTags.SAND) || ground.is(Blocks.GRAVEL) || ground.is(Blocks.CLAY) || ground.is(Blocks.MUD) || ground.is(BlockTags.DIRT);
						if (soil && world.getBlockState(above).is(Blocks.WATER) && player.getRandom().nextFloat() < 0.35F) {
							boolean deep = world.getBlockState(above.above()).is(Blocks.WATER) && world.getBlockState(above.above(2)).is(Blocks.WATER);
							world.setBlock(above, (deep ? Blocks.KELP : Blocks.SEAGRASS).defaultBlockState(), 3);
							world.sendParticles(ParticleTypes.HAPPY_VILLAGER, above.getX() + 0.5, above.getY() + 0.5, above.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
							planted++;
							break;
						}
					}
				}
			}
			if (planted == 0) {
				message(player, "msg.primordial.cant_here");
				return;
			}
			Abilities.sound(player, SoundEvents.WET_GRASS_PLACE, 1.0F, 1.0F);
		});

		// Ecolocalização: colunas de luz mostram minérios e baús num raio de 16 blocos (24 desperto).
		Abilities.register("ecolocalizacao", 300, (player, level) -> {
			ServerLevel world = player.level();
			int radius = Ability.awakened(level) ? 24 : 16;
			BlockPos origin = player.blockPosition();
			int found = 0;
			for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
				if (found >= 40) {
					break;
				}
				BlockState state = world.getBlockState(pos);
				String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
				if (id.endsWith("_ore") || id.equals("ancient_debris") || state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
					world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 12, 0.1, 0.6, 0.1, 0.01);
					found++;
				}
			}
			player.sendSystemMessage(Component.translatable("msg.primordial.echo", found).withStyle(ChatFormatting.AQUA), true);
			Abilities.sound(player, SoundEvents.WARDEN_SONIC_CHARGE, 0.7F, 1.6F);
		});

		// Bote Venenoso: avança e envenena quem estiver no caminho.
		Abilities.register("bote_venenoso", 140, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 look = player.getLookAngle();
			player.setDeltaMovement(look.scale(1.5));
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
			for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.2).move(look.scale(1.6)), e -> e != player)) {
				target.hurtServer(world, player.damageSources().playerAttack(player), 2.0F + level);
				target.addEffect(new MobEffectInstance(MobEffects.POISON, Ability.awakened(level) ? 200 : 100, Ability.awakened(level) ? 1 : 0));
			}
			Abilities.burst(world, ParticleTypes.ITEM_SLIME, player, 20, 0.1);
			Abilities.sound(player, SoundEvents.SPIDER_HURT, 0.8F, 1.4F);
		});

		// Chicotada Giratória: golpe em volta; desperto, puxa todos para perto antes de bater.
		Abilities.register("chicotada_giratoria", 140, (player, level) -> {
			ServerLevel world = player.level();
			for (LivingEntity target : Abilities.around(world, player, 4.5)) {
				if (Ability.awakened(level)) {
					Vec3 pull = player.position().subtract(target.position()).normalize().scale(0.8);
					target.setDeltaMovement(pull.x, 0.1, pull.z);
				}
				target.hurtServer(world, player.damageSources().playerAttack(player), 3.0F + level);
				Vec3 away = target.position().subtract(player.position()).normalize().scale(0.5);
				target.setDeltaMovement(target.getDeltaMovement().add(away.x, 0.15, away.z));
			}
			Abilities.burst(world, ParticleTypes.SWEEP_ATTACK, player, 6, 0.0);
			Abilities.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
		});

		// Intimidar: bichos perto desistem de você e fogem; outros jogadores ficam fracos.
		Abilities.register("intimidar", 300, (player, level) -> {
			ServerLevel world = player.level();
			for (Mob mob : world.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12.0))) {
				if (mob.getBbWidth() < player.getBbWidth() * 1.6F) {
					mob.setTarget(null);
					Vec3 away = mob.position().subtract(player.position()).normalize().scale(0.7);
					mob.setDeltaMovement(away.x, 0.1, away.z);
					if (Ability.awakened(level)) {
						player.heal(1.0F);
					}
				}
			}
			for (ServerPlayer other : world.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(10.0), p -> p != player)) {
				other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
			}
			Abilities.burst(world, ParticleTypes.ANGRY_VILLAGER, player, 10, 0.2);
			Abilities.sound(player, SoundEvents.RAVAGER_ROAR, 0.8F, 1.3F);
		});

		// Frenesi: mais dano e velocidade por alguns segundos, mas dá fome.
		Abilities.register("frenesi", 600, (player, level) -> {
			int ticks = Ability.awakened(level) ? 200 : 120;
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 1));
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1));
			player.addEffect(new MobEffectInstance(MobEffects.HASTE, ticks, 1));
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, 1));
			Abilities.burst(player.level(), ParticleTypes.ANGRY_VILLAGER, player, 14, 0.2);
			Abilities.sound(player, SoundEvents.RAVAGER_ROAR, 0.8F, 1.6F);
		});

		// Marcar Presa: o alvo na mira brilha e leva 30% a mais de dano seu por 15 segundos.
		Abilities.register("marcar_presa", 240, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 start = player.getEyePosition();
			Vec3 look = player.getLookAngle();
			for (int step = 1; step <= 28; step++) {
				Vec3 at = start.add(look.scale(step));
				for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(0.8), e -> e != player && !e.isSpectator())) {
					target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
					MARKS.put(target.getUUID(), new Long[] {world.getGameTime() + 300});
					MARK_OWNER.put(target.getUUID(), player.getUUID());
					Abilities.burst(world, ParticleTypes.CRIT, target, 12, 0.1);
					Abilities.sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 0.6F);
					return;
				}
			}
			message(player, "msg.primordial.no_target");
		});
	}

	/** Onde o jogador olha, dentro d'água: o bloco de água colado no que ele mira (ou no vazio da frente). */
	private static BlockPos lookedWaterSpot(ServerPlayer player, ServerLevel world, double range) {
		HitResult hit = player.pick(range, 0.0F, false);
		if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = block.getBlockPos().relative(block.getDirection());
			return world.getFluidState(pos).is(FluidTags.WATER) ? pos : null;
		}
		BlockPos ahead = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(6.0)));
		return world.getFluidState(ahead).is(FluidTags.WATER) ? ahead : null;
	}

	private static void message(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
	}

}
