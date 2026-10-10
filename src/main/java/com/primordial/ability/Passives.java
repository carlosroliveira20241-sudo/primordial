package com.primordial.ability;

import com.primordial.creature.CreatureBody;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Efeitos passivos das partes equipadas (campo "passives" do JSON). Rodam todo tick enquanto a parte está no corpo.
 * O nível da parte chega junto: no nível III (desperta) o efeito melhora.
 */
public final class Passives {
	private static final Map<UUID, Integer> STILL = new HashMap<>();
	private static final Map<UUID, Integer> MOVING = new HashMap<>();

	private Passives() {
	}

	public static void tick(ServerPlayer player) {
		com.primordial.mutation.MutationEffects.tick(player);
		CreatureBody body = Creatures.body(player);
		Map<String, Integer> active = new HashMap<>();
		for (PartRef ref : body.parts().values()) {
			PartDefinition def = PartRegistry.get(ref.id());
			if (def != null) {
				for (String passive : def.passives()) {
					active.merge(passive, ref.level(), Math::max);
				}
			}
		}
		boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.004;
		boolean still = player.getDeltaMovement().horizontalDistanceSqr() < 0.0004;
		if (still) {
			STILL.merge(player.getUUID(), 1, Integer::sum);
			MOVING.remove(player.getUUID());
		} else {
			STILL.remove(player.getUUID());
			if (moving) {
				MOVING.merge(player.getUUID(), 1, Integer::sum);
			}
		}
		boolean water = player.isInWater();
		boolean dark = player.level().isDarkOutside();

		for (Map.Entry<String, Integer> entry : active.entrySet()) {
			int level = entry.getValue();
			switch (entry.getKey()) {
				// Parado, você some; desperta, some bem mais rápido.
				case "camouflage_still" -> {
					if (STILL.getOrDefault(player.getUUID(), 0) >= (level >= 3 ? 10 : 40)) {
						give(player, MobEffects.INVISIBILITY, 25, 0);
					}
				}
				// Brânquias despertas: na água, regenera devagar.
				case "gills" -> {
					if (level >= 3 && water && player.tickCount % 60 == 0) {
						give(player, MobEffects.REGENERATION, 80, 0);
					}
				}
				// Muco desperto: na água, nada com a graça do golfinho.
				case "slippery" -> {
					if (level >= 3 && water) {
						give(player, MobEffects.DOLPHINS_GRACE, 60, 0);
					}
				}
				// Enxerga no escuro das profundezas.
				// Visão abissal: enxerga no escuro; desperta (Olhar do Abismo), quem você encara de perto fica lento.
				case "abyssal_sight" -> {
					give(player, MobEffects.NIGHT_VISION, 400, 0);
					if (level >= 3 && player.tickCount % 20 == 0 && player.level() instanceof net.minecraft.server.level.ServerLevel world) {
						net.minecraft.world.phys.Vec3 look = player.getLookAngle();
						for (net.minecraft.world.entity.LivingEntity target : world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(8.0), e -> e != player)) {
							net.minecraft.world.phys.Vec3 to = target.position().subtract(player.position()).normalize();
							if (to.dot(look) > 0.85) {
								target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
							}
						}
					}
				}
				// Lanterna: sua luz atrai peixes pequenos (isca viva); desperta, de mais longe.
				case "lantern" -> {
					if (player.tickCount % 20 == 0 && player.level() instanceof net.minecraft.server.level.ServerLevel world) {
						double radius = level >= 3 ? 22.0 : 12.0;
						int pulled = 0;
						for (net.minecraft.world.entity.animal.fish.AbstractFish fish : world.getEntitiesOfClass(net.minecraft.world.entity.animal.fish.AbstractFish.class,
							player.getBoundingBox().inflate(radius), f -> !(f instanceof com.primordial.entity.Predador) && f.distanceTo(player) > 3.0F)) {
							if (pulled++ < 6) {
								fish.getNavigation().moveTo(player, 1.2);
							}
						}
					}
				}
				// Imune a tinta.
				case "ink_immunity" -> player.removeEffect(MobEffects.BLINDNESS);
				// Brilho: à noite, na água, cura devagar; desperto, também de dia e cura os amigos perto.
				case "glow" -> {
					if (water && (dark || level >= 3) && player.tickCount % 80 == 0) {
						give(player, MobEffects.REGENERATION, 60, 0);
						if (level >= 3 && player.level() instanceof net.minecraft.server.level.ServerLevel world) {
							for (ServerPlayer friend : world.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(8.0), p -> p != player)) {
								friend.heal(1.0F);
							}
						}
					}
				}
				// Bexiga: um colchão de ar que absorve dano de vez em quando.
				case "buoyancy" -> {
					if (player.tickCount % 600 == 0) {
						give(player, MobEffects.ABSORPTION, 400, level >= 3 ? 1 : 0);
					}
				}
				// Nadando sem parar, ganha embalo.
				case "slipstream" -> {
					if (water && MOVING.getOrDefault(player.getUUID(), 0) >= 60) {
						give(player, MobEffects.SPEED, 40, level >= 3 ? 1 : 0);
					}
				}
				// Hibernação: parado por 5 segundos, regenera muito rápido.
				case "hibernation" -> {
					if (STILL.getOrDefault(player.getUUID(), 0) >= 100) {
						give(player, MobEffects.REGENERATION, 40, level >= 3 ? 3 : 2);
					}
				}
				// Calor do corpo: imune a fogo e lava.
				case "heat_adapt" -> give(player, MobEffects.FIRE_RESISTANCE, 100, 0);
				default -> {
				}
			}
		}
	}

	private static void give(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
		MobEffectInstance current = player.getEffect(effect);
		if (current == null || current.getDuration() < duration / 2 || current.getAmplifier() < amplifier) {
			player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
		}
	}
}
