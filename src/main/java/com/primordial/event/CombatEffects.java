package com.primordial.event;

import com.primordial.creature.Creatures;
import com.primordial.registry.PrimordialAttributes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Efeitos de combate que vêm das partes: espinhos (reflete), roubo de vida e choque nos golpes. */
public final class CombatEffects {
	private static boolean inside;

	private CombatEffects() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamage, damage, blocked) -> {
			if (inside || blocked || damage <= 0 || !(victim.level() instanceof ServerLevel level)) {
				return;
			}
			inside = true;
			try {
				// Espinhos: quem bate em você leva de volta.
				if (victim instanceof ServerPlayer defender && Creatures.isCreature(defender)
					&& source.getEntity() instanceof LivingEntity attacker && attacker != defender) {
					double thorns = defender.getAttributeValue(PrimordialAttributes.THORNS);
					if (thorns > 0) {
						attacker.hurtServer(level, defender.damageSources().thorns(defender), (float) thorns);
					}
				}
				// Tinta reflexa (Glândula de Tinta desperta): às vezes, ao ser atingido, solta tinta e cega o agressor.
				if (victim instanceof ServerPlayer inked && Creatures.passiveLevel(inked, "ink_immunity") >= 3
					&& source.getEntity() instanceof LivingEntity culprit && inked.getRandom().nextFloat() < 0.25F) {
					culprit.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0));
					level.sendParticles(ParticleTypes.SQUID_INK, inked.getX(), inked.getY() + 0.3, inked.getZ(), 30, 0.6, 0.4, 0.6, 0.05);
				}
				// Golpes da criatura: roubo de vida e choque extra.
				if (source.getEntity() instanceof ServerPlayer hitter && Creatures.isCreature(hitter) && victim != hitter) {
					double steal = hitter.getAttributeValue(PrimordialAttributes.LIFESTEAL);
					if (steal > 0) {
						hitter.heal((float) (damage * steal));
					}
					// Sangue de Magma (Glândula de Brasa desperta): os golpes queimam o alvo por dentro.
					if (Creatures.passiveLevel(hitter, "heat_adapt") >= 3) {
						victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 40, 0));
					}
					double shock = hitter.getAttributeValue(PrimordialAttributes.SHOCK_DAMAGE);
					if (shock > 0) {
						victim.hurtServer(level, hitter.damageSources().playerAttack(hitter), (float) shock);
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 0.4, victim.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
					}
				}
			} finally {
				inside = false;
			}
		});
	}
}
