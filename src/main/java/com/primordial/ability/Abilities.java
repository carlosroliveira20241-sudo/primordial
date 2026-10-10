package com.primordial.ability;

import com.primordial.creature.CreatureService;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Todas as habilidades ativas do mod, cada uma com seu nome e sua recarga. */
public final class Abilities {
	private static final Map<String, Ability> ALL = new HashMap<>();
	/** Jogadores que pularam com o Salto desperto: quando o tempo passa e caem na água, soltam a onda. */
	public static final Map<java.util.UUID, Long> WAVE_PENDING = new HashMap<>();

	private Abilities() {
	}

	public static boolean exists(String name) {
		return ALL.containsKey(name);
	}

	public static Ability get(String name) {
		return ALL.get(name);
	}

	private static void register(String name, int cooldownTicks, Ability.Effect effect) {
		ALL.put(name, new Ability() {
			@Override
			public int cooldownTicks() {
				return cooldownTicks;
			}

			@Override
			public void use(ServerPlayer player, int level) {
				effect.apply(player, level);
			}
		});
	}

	public static void init() {
		register("investida", 50, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 look = player.getLookAngle();
			player.setDeltaMovement(look.scale(1.6));
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
			for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.2).move(look.scale(1.5)), e -> e != player)) {
				target.hurtServer(world, player.damageSources().playerAttack(player), 2.0F + level);
			}
			burst(world, ParticleTypes.BUBBLE, player, 25, 0.1);
			sound(player, SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.6F, 1.6F);
		});

		register("inflar", 300, (player, level) -> {
			ServerLevel world = player.level();
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1, false, true, true));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1, false, false, true));
			for (LivingEntity target : around(world, player, 2.5)) {
				Vec3 away = target.position().subtract(player.position()).normalize().scale(0.8);
				target.setDeltaMovement(away.x, 0.2, away.z);
			}
			burst(world, ParticleTypes.POOF, player, 20, 0.05);
			sound(player, SoundEvents.PUFFER_FISH_BLOW_UP, 0.8F, 1.0F);
		});

		// Choque: descarga em volta; desperta, o choque pula para mais um alvo mais longe (corrente em cadeia).
		register("choque", 100, (player, level) -> {
			ServerLevel world = player.level();
			float damage = 3.0F + level;
			for (LivingEntity target : around(world, player, 4.0)) {
				target.hurtServer(world, player.damageSources().playerAttack(player), damage);
				burst(world, ParticleTypes.ELECTRIC_SPARK, target, 12, 0.1);
			}
			if (Ability.awakened(level)) {
				for (LivingEntity target : around(world, player, 8.0)) {
					if (target.distanceTo(player) > 4.0F) {
						target.hurtServer(world, player.damageSources().playerAttack(player), damage - 1.0F);
						burst(world, ParticleTypes.ELECTRIC_SPARK, target, 12, 0.1);
						break;
					}
				}
			}
			burst(world, ParticleTypes.ELECTRIC_SPARK, player, 40, 0.3);
			sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4F, 2.0F);
		});

		// Nuvem de Tinta: cega quem está perto e dá velocidade para fugir; desperta, a tinta também envenena.
		register("nuvem_de_tinta", 200, (player, level) -> {
			ServerLevel world = player.level();
			for (LivingEntity target : around(world, player, 4.0)) {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100 + 20 * level, 0));
				if (Ability.awakened(level)) {
					target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
				}
			}
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
			burst(world, ParticleTypes.SQUID_INK, player, 90, 0.08);
			sound(player, SoundEvents.SQUID_SQUIRT, 1.0F, 0.9F);
		});

		// Rajada de Espinhos: dano em todos em volta; desperta, os espinhos ficam e envenenam (farpas).
		register("rajada_de_espinhos", 120, (player, level) -> {
			ServerLevel world = player.level();
			for (LivingEntity target : around(world, player, 3.5)) {
				target.hurtServer(world, player.damageSources().playerAttack(player), 2.0F + level);
				if (Ability.awakened(level)) {
					target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
				}
			}
			burst(world, ParticleTypes.CRIT, player, 60, 0.4);
			sound(player, SoundEvents.PUFFER_FISH_STING, 1.0F, 1.2F);
		});

		// Pulso Luminoso: clarão que atordoa (lentidão forte) e revela; desperta, também revela quem está invisível.
		register("pulso_luminoso", 160, (player, level) -> {
			ServerLevel world = player.level();
			for (LivingEntity target : around(world, player, 6.0)) {
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30 + 10 * level, 4));
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
				if (Ability.awakened(level)) {
					target.removeEffect(MobEffects.INVISIBILITY);
				}
			}
			burst(world, ParticleTypes.GLOW, player, 60, 0.5);
			sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.5F);
		});

		// Sonar: revela criaturas num raio grande através das paredes (brilho) e mostra onde estão.
		register("sonar", 100, (player, level) -> {
			ServerLevel world = player.level();
			double radius = Ability.awakened(level) ? 48.0 : 32.0;
			for (LivingEntity target : around(world, player, radius)) {
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
			}
			burst(world, ParticleTypes.SONIC_BOOM, player, 1, 0.0);
			sound(player, SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.8F);
		});

		// Salto de Golfinho: sai da água num pulo alto; desperta, a queda levanta uma onda (empurra quem está perto).
		register("salto_de_golfinho", 140, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 look = player.getLookAngle();
			player.setDeltaMovement(look.x * 1.1, 1.0 + 0.1 * level, look.z * 1.1);
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false));
			if (Ability.awakened(level)) {
				WAVE_PENDING.put(player.getUUID(), world.getGameTime() + 20);
			}
			burst(world, ParticleTypes.SPLASH, player, 40, 0.3);
			sound(player, SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
		});

		// Grudar e Sugar: morde o alvo à frente e cura você com a vida roubada; desperta, rouba o dobro.
		register("grudar_e_sugar", 160, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 look = player.getLookAngle();
			for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.0).move(look.scale(1.8)), e -> e != player)) {
				float stolen = (4.0F + level) * (Ability.awakened(level) ? 2.0F : 1.0F);
				target.hurtServer(world, player.damageSources().playerAttack(player), stolen);
				player.heal(stolen * 0.8F);
				burst(world, ParticleTypes.DAMAGE_INDICATOR, target, 10, 0.2);
				break;
			}
			sound(player, SoundEvents.GENERIC_DRINK.value(), 1.0F, 0.6F);
		});

		// Recolher no Casco: quase imune, mas lento; desperta, dura mais.
		register("recolher_no_casco", 300, (player, level) -> {
			int ticks = Ability.awakened(level) ? 100 : 60;
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 4, false, true, true));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 5, false, false, true));
			burst(player.level(), ParticleTypes.CRIT, player, 30, 0.2);
			sound(player, SoundEvents.TURTLE_SHAMBLE, 1.0F, 0.6F);
		});

		// Investida Perfurante: dispara e machuca TODOS numa linha de 7 blocos.
		register("investida_perfurante", 110, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 look = player.getLookAngle();
			player.setDeltaMovement(look.scale(2.0));
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
			for (int step = 1; step <= 7; step++) {
				Vec3 at = player.position().add(look.scale(step)).add(0, player.getBbHeight() * 0.5, 0);
				for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(1.0), e -> e != player)) {
					target.hurtServer(world, player.damageSources().playerAttack(player), 4.0F + level);
				}
			}
			burst(world, ParticleTypes.BUBBLE, player, 40, 0.2);
			sound(player, SoundEvents.TRIDENT_RIPTIDE_2.value(), 0.7F, 1.4F);
		});

		// Mordida Dilacerante: mordida forte no alvo na frente; sangra (veneno do tipo "sangramento"); desperta, sangra mais.
		register("mordida_dilacerante", 80, (player, level) -> {
			ServerLevel world = player.level();
			Vec3 look = player.getLookAngle();
			for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.8).move(look.scale(1.6)), e -> e != player)) {
				target.hurtServer(world, player.damageSources().playerAttack(player), 4.0F + 1.5F * level);
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, Ability.awakened(level) ? 120 : 60, 0));
				burst(world, ParticleTypes.DAMAGE_INDICATOR, target, 10, 0.2);
				break;
			}
			sound(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
		});
	}

	private static Iterable<LivingEntity> around(ServerLevel world, ServerPlayer player, double radius) {
		return world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> e != player && e.distanceTo(player) <= radius);
	}

	private static void burst(ServerLevel world, ParticleOptions particle, LivingEntity at, int count, double speed) {
		world.sendParticles(particle, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), count, 0.4, 0.3, 0.4, speed);
	}

	private static void sound(ServerPlayer player, SoundEvent event, float volume, float pitch) {
		CreatureService.playSound(player, event, volume, pitch);
	}
}
