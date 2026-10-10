package com.primordial.entity;

import com.primordial.creature.Creatures;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Comportamento "predator": caça por tamanho (regra do plano). Ataca peixes menores e jogadores-criatura cujo
 * estágio é menor que o {@code power} do JSON; quem já cresceu mais que ele passa a ser respeitado.
 * Com a característica shock_hit o ataque dá choque e deixa o alvo lento.
 */
public class Predador extends AbstractFish {
	private final CreatureDef def;
	private net.minecraft.server.level.ServerBossEvent bossBar;

	public Predador(EntityType<? extends AbstractFish> type, Level level) {
		super(type, level);
		this.def = CreatureRegistry.of(type);
		if (this.def != null && this.def.has("boss")) {
			this.bossBar = new net.minecraft.server.level.ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(),
				net.minecraft.world.BossEvent.BossBarColor.RED, net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
			this.setPersistenceRequired();
		}
	}

	@Override
	public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
		super.startSeenByPlayer(player);
		if (this.bossBar != null) {
			this.bossBar.addPlayer(player);
		}
	}

	@Override
	public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer player) {
		super.stopSeenByPlayer(player);
		if (this.bossBar != null) {
			this.bossBar.removePlayer(player);
		}
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		if (this.bossBar != null) {
			this.bossBar.removeAllPlayers();
		}
		super.remove(reason);
	}

	public static AttributeSupplier.Builder attributes(CreatureDef def) {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, def.health())
			.add(Attributes.ATTACK_DAMAGE, def.damage())
			.add(Attributes.MOVEMENT_SPEED, def.speed())
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal || goal instanceof AvoidEntityGoal);
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
			(target, level) -> target instanceof Player player && canHunt(player)));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractFish.class, true,
			(target, level) -> !(target instanceof Predador) && target.getBbWidth() < this.getBbWidth()));
	}

	private boolean canHunt(Player player) {
		if (player.isCreative() || player.isSpectator()) {
			return false;
		}
		if (com.primordial.ability.Hiding.isHidden(player)) {
			return false;
		}
		float power = this.def != null ? this.def.power() : 0.5F;
		return !Creatures.isCreature(player) || Creatures.body(player).stage().scale < power;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.getTarget() == null && this.tickCount % 30 == 0 && this.level() instanceof ServerLevel world) {
			net.minecraft.world.phys.Vec3 carcass = com.primordial.world.OceanLife.nearestCarcass(world, this.position(), 32.0);
			if (carcass != null) {
				this.getNavigation().moveTo(carcass.x, carcass.y, carcass.z, 1.1);
			}
		}
		if (this.bossBar != null) {
			this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
		}
		// O Leviatã que ninguém derrota some depois de 10 minutos.
		if (this.def != null && this.def.has("event_only") && this.tickCount > 12000) {
			this.discard();
		}
		// Zumbis são criaturas da noite: quando o dia nasce, eles se desfazem.
		if (this.def != null && this.def.has("undead") && !this.def.spawn().biomes().equals("cave") && this.level() instanceof ServerLevel level
			&& this.tickCount % 40 == 0 && !level.isDarkOutside()) {
			level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.3, this.getZ(), 20, 0.4, 0.3, 0.4, 0.02);
			this.discard();
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && this.def != null && this.def.has("shock_hit") && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 0.3, target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
		}
		return hit;
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
