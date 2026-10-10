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
				case "abyssal_sight", "lantern" -> give(player, MobEffects.NIGHT_VISION, 400, 0);
				// Imune a tinta.
				case "ink_immunity" -> player.removeEffect(MobEffects.BLINDNESS);
				// Brilho: à noite, na água, a luz do corpo cura devagar.
				case "glow" -> {
					if (water && dark && player.tickCount % 80 == 0) {
						give(player, MobEffects.REGENERATION, 60, 0);
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
