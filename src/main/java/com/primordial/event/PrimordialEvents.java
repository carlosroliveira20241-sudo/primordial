package com.primordial.event;

import com.primordial.config.PrimordialConfig;
import com.primordial.creature.CreatureService;
import com.primordial.creature.CreatureStats;
import com.primordial.creature.Creatures;
import com.primordial.item.PrimordialItems;
import com.primordial.net.Payloads;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Ligações do mod com os eventos do jogo (servidor e travas que valem nos dois lados). */
public final class PrimordialEvents {
	/** Definido pelo cliente: chamado quando uma criatura "ataca" um bloco (para mandar o pedido de comer planta). */
	public static @Nullable Consumer<BlockPos> clientAttackHook;

	private PrimordialEvents() {
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			CreatureStats.apply(player);
			if (!Creatures.progress(player).originChosen()) {
				ServerPlayNetworking.send(player, new Payloads.OpenOrigin());
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> CreatureService.onRespawn(newPlayer));

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer victim) {
				if (!Creatures.isCreature(victim)) {
					return;
				}
				int dnaBefore = Creatures.progress(victim).dna();
				CreatureService.applyDeathPenalty(victim);
				if (PrimordialConfig.get().competitive && source.getEntity() instanceof ServerPlayer killer
					&& killer != victim && Creatures.isCreature(killer)) {
					CreatureService.addDna(killer, CreatureService.stealAmount(killer, victim, dnaBefore));
				}
			} else if (source.getEntity() instanceof ServerPlayer killer && Creatures.isCreature(killer)) {
				MorselDrops.onKill(killer, entity);
			}
		});

		// Só existem criaturas do mod: bloqueia o nascimento natural dos mobs vanilla.
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fromDisk) -> {
			if (fromDisk || !(entity instanceof Mob) || reason == null) {
				return true;
			}
			boolean overworld = level.dimension() == Level.OVERWORLD;
			PrimordialConfig config = PrimordialConfig.get();
			if (overworld ? !config.blockVanillaMobsInOverworld : !config.blockVanillaMobsInOtherDimensions) {
				return true;
			}
			if (!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("minecraft")) {
				return true;
			}
			return !(reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION || reason == EntitySpawnReason.STRUCTURE);
		});

		// Criaturas não quebram nem usam blocos e itens do Minecraft (por enquanto); morder planta é o único gesto.
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!Creatures.isRestricted(player)) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide() && clientAttackHook != null) {
				clientAttackHook.accept(pos);
			}
			return InteractionResult.FAIL;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> Creatures.isRestricted(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, level, hand) -> Creatures.isRestricted(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> Creatures.isRestricted(player) ? InteractionResult.FAIL : InteractionResult.PASS);

		ServerTickEvents.END_LEVEL_TICK.register(PrimordialEvents::tickLevel);
	}

	private static void tickLevel(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (!Creatures.isCreature(player) || player.isSpectator()) {
				continue;
			}
			eatNearbyMorsels(level, player);
			if (Creatures.hasPassive(player, "abyssal_sight")) {
				MobEffectInstance sight = player.getEffect(MobEffects.NIGHT_VISION);
				if (sight == null || sight.getDuration() < 240) {
					player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
				}
			}
			if (!Creatures.isAquatic(player) || player.isCreative()) {
				continue;
			}
			if (player.isInWater()) {
				MobEffectInstance current = player.getEffect(MobEffects.WATER_BREATHING);
				if (current == null || current.getDuration() < 20) {
					player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, false, false, false));
				}
			} else {
				MobEffectInstance slow = player.getEffect(MobEffects.SLOWNESS);
				if (slow == null || slow.getDuration() < 10) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2, false, false, false));
				}
				// O vanilla devolve +4 de ar por tick fora d'água; tirando 5 sobra -1 por tick (~15 s de fôlego).
				int air = player.getAirSupply() - 5;
				if (air <= -20) {
					player.hurtServer(level, player.damageSources().dryOut(), 2.0F);
					air = 0;
				}
				player.setAirSupply(air);
			}
		}
	}

	/** Melhorias ficam paradas e brilhando na água; a criatura as come ao encostar. */
	private static void eatNearbyMorsels(ServerLevel level, ServerPlayer player) {
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(20.0), e -> e.getItem().is(PrimordialItems.MORSEL))) {
			Identifier part = item.getItem().get(PrimordialItems.PART);
			if (part == null) {
				continue;
			}
			if (item.getBoundingBox().intersects(player.getBoundingBox().inflate(0.8))) {
				CreatureService.eatMorsel(player, part);
				item.discard();
				continue;
			}
			item.setDeltaMovement(Vec3.ZERO);
			if (item.tickCount % 10 == 0) {
				level.sendParticles(ParticleTypes.GLOW, item.getX(), item.getY() + 0.3, item.getZ(), 2, 0.15, 0.15, 0.15, 0.0);
			}
		}
	}
}
