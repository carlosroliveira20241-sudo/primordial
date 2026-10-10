package com.primordial.mutation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Efeitos das mutações que vão além de atributos (campo "passive" do JSON): regeneração, visão no escuro,
 * recarga menor, nado turbinado e a Segunda Vida (sobrevive a um golpe fatal a cada 10 minutos).
 */
public final class MutationEffects {
	private static final Map<UUID, Long> SECOND_LIFE_USED = new HashMap<>();
	private static final long SECOND_LIFE_COOLDOWN = 20L * 60 * 10;

	private MutationEffects() {
	}

	public static boolean has(Player player, String passive) {
		for (Identifier id : MutationService.state(player).chosen()) {
			Mutation mutation = MutationRegistry.get(id);
			if (mutation != null && mutation.passive().filter(passive::equals).isPresent()) {
				return true;
			}
		}
		return false;
	}

	/** Multiplicador da recarga das habilidades (1.0 = normal). */
	public static float cooldownFactor(Player player) {
		float factor = 1.0F;
		if (has(player, "cooldown_down")) {
			factor *= 0.85F;
		}
		if (has(player, "cooldown_down_big")) {
			factor *= 0.7F;
		}
		int quick = com.primordial.creature.Creatures.passiveLevel(player, "quick_mind");
		if (quick > 0) {
			factor *= quick >= 3 ? 0.7F : 0.85F;
		}
		return factor;
	}

	public static void tick(ServerPlayer player) {
		if (player.tickCount % 20 != 0) {
			return;
		}
		if (has(player, "regen")) {
			give(player, MobEffects.REGENERATION, 60, 0);
		}
		if (has(player, "regen_strong")) {
			give(player, MobEffects.REGENERATION, 60, 1);
		}
		if (has(player, "night_vision")) {
			give(player, MobEffects.NIGHT_VISION, 400, 0);
		}
		if (has(player, "water_burst") && player.isInWater()) {
			give(player, MobEffects.DOLPHINS_GRACE, 60, 0);
		}
		if (has(player, "night_regen") && player.level().isDarkOutside()) {
			give(player, MobEffects.REGENERATION, 60, 0);
		}
	}

	/** Segunda Vida: chamada antes da morte. Retorna verdadeiro se a criatura sobreviveu. */
	public static boolean trySecondLife(ServerPlayer player) {
		if (!has(player, "second_life")) {
			return false;
		}
		long now = player.level().getGameTime();
		if (now - SECOND_LIFE_USED.getOrDefault(player.getUUID(), -SECOND_LIFE_COOLDOWN) < SECOND_LIFE_COOLDOWN) {
			return false;
		}
		SECOND_LIFE_USED.put(player.getUUID(), now);
		player.setHealth(Math.max(6.0F, player.getMaxHealth() * 0.3F));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 2));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
		return true;
	}

	private static void give(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
		MobEffectInstance current = player.getEffect(effect);
		if (current == null || current.getDuration() < duration / 2 || current.getAmplifier() < amplifier) {
			player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
		}
	}
}
