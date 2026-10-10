package com.primordial.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Comportamento "fish": peixe pacífico que foge de jogadores. Os poderes vêm do campo {@code traits} do JSON:
 * glow (brilha), ink (solta tinta ao apanhar) e shock_touch (choca quem encosta).
 */
public class PeixeComum extends AbstractFish {
	private final CreatureDef def;

	public PeixeComum(EntityType<? extends AbstractFish> type, Level level) {
		super(type, level);
		this.def = CreatureRegistry.of(type);
		if (has("still")) {
			this.setNoAi(true);
		}
		if (has("glow")) {
			this.setGlowingTag(true);
		}
	}

	private boolean has(String trait) {
		return this.def != null && this.def.has(trait);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && this.isAlive() && has("ink")) {
			level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.3, this.getZ(), 40, 0.8, 0.5, 0.8, 0.05);
			for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0), e -> e != this)) {
				near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
			}
			this.playSound(SoundEvents.SQUID_SQUIRT, 1.0F, 1.0F);
		}
		return hurt;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (has("shock_touch") && this.level() instanceof ServerLevel level && this.tickCount % 20 == 0) {
			for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6), e -> e != this && !(e instanceof AbstractFish))) {
				near.hurtServer(level, this.damageSources().mobAttack(this), 1.0F);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, near.getX(), near.getY() + 0.5, near.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
			}
		}
	}

	/** Criaturas do mod não vão para balde (mobInteract devolve PASS); isto só satisfaz a interface. */
	@Override
	public ItemStack getBucketItemStack() {
		return new ItemStack(Items.COD_BUCKET);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	@Override
	protected SoundEvent getFlopSound() {
		return SoundEvents.COD_FLOP;
	}
}
