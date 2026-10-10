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
		if (has("still") || has("whale")) {
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
		if (this.level() instanceof ServerLevel world) {
			if (has("anemone") && this.tickCount % 20 == 0) {
				// Anêmona: cura quem é criatura e machuca quem a incomoda.
				for (LivingEntity near : world.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.5), e -> e != this)) {
					if (near instanceof net.minecraft.server.level.ServerPlayer p && com.primordial.creature.Creatures.isCreature(p)) {
						p.heal(1.0F);
						world.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 0.6, p.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
					} else if (near instanceof com.primordial.entity.Predador || near instanceof net.minecraft.world.entity.monster.Monster) {
						near.hurtServer(world, this.damageSources().mobAttack(this), 2.0F);
						near.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
					}
				}
			}
			if (has("whale")) {
				// Baleia-Colosso: segue em linha reta e abençoa quem nada na esteira dela.
				net.minecraft.world.phys.Vec3 heading = net.minecraft.world.phys.Vec3.directionFromRotation(0.0F, this.getYRot());
				this.move(net.minecraft.world.entity.MoverType.SELF, heading.scale(0.14));
				if (this.tickCount > 6000) {
					this.discard();
				}
				if (this.tickCount % 20 == 0) {
					for (net.minecraft.server.level.ServerPlayer p : world.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, this.getBoundingBox().inflate(14.0))) {
						p.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1));
						p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
					}
					world.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, this.getX(), this.getY(), this.getZ(), 20, 3.0, 1.5, 3.0, 0.02);
				}
			}
		}
		if (has("symbiont")) {
			Symbionts.tick(this);
		}
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
