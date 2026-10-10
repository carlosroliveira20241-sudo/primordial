package com.primordial.event;

import com.primordial.config.PrimordialConfig;
import com.primordial.creature.CreatureService;
import com.primordial.creature.CreatureStats;
import com.primordial.creature.Creatures;
import com.primordial.item.PrimordialItems;
import com.primordial.net.Payloads;
import com.primordial.part.PartDefinition;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
			} else {
				feedScavengers(entity);
				com.primordial.world.OceanLife.addCarcass(entity);
				if (source.getEntity() instanceof ServerPlayer killer && Creatures.isCreature(killer)) {
					MorselDrops.onKill(killer, entity);
				}
			}
		});

		// Só existem criaturas do mod: bloqueia o nascimento natural dos mobs vanilla.
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fromDisk) -> {
			if (fromDisk || !(entity instanceof Mob) || reason == null) {
				return true;
			}
			if (!fromDisk && entity instanceof Mob mob && com.primordial.entity.CreatureRegistry.of(entity.getType()) != null) {
				makeAlphaMaybe(mob);
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
			net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
			// algas, escavações e corais continuam sendo mordidas; o resto depende do que a criatura aprendeu a quebrar
			boolean bite = com.primordial.creature.FeedingService.isPlant(state) || com.primordial.world.SeabedSeeder.isDigSite(state)
				|| state.is(net.minecraft.tags.BlockTags.CORAL_BLOCKS) || state.is(net.minecraft.tags.BlockTags.CORALS)
				|| state.is(net.minecraft.tags.BlockTags.WALL_CORALS);
			if (!bite && com.primordial.capability.Capabilities.canBreak(player, level, pos, state)) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide() && clientAttackHook != null) {
				clientAttackHook.accept(pos);
			}
			return InteractionResult.FAIL;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!Creatures.isRestricted(player)) {
				return InteractionResult.PASS;
			}
			net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
			net.minecraft.world.level.block.state.BlockState clicked = level.getBlockState(hit.getBlockPos());
			if (clicked.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
				return InteractionResult.PASS;
			}
			// Baú de Areia: botão direito numa areia segurando outra areia.
			if (held.is(net.minecraft.tags.ItemTags.SAND) && clicked.is(net.minecraft.tags.BlockTags.SAND)
				&& com.primordial.capability.Capabilities.has(player, "craft_chest")) {
				if (!level.isClientSide()) {
					level.setBlock(hit.getBlockPos(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()
						.setValue(net.minecraft.world.level.block.ChestBlock.FACING, player.getDirection().getOpposite()), 3);
					if (!player.isCreative()) {
						held.shrink(1);
					}
					level.playSound(null, hit.getBlockPos(), net.minecraft.sounds.SoundEvents.SAND_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
				}
				return InteractionResult.SUCCESS;
			}
			if (held.getItem() instanceof net.minecraft.world.item.BlockItem && com.primordial.capability.Capabilities.has(player, "place")
				&& !interactive(level.getBlockState(hit.getBlockPos()))) {
				return InteractionResult.PASS;
			}
			return InteractionResult.FAIL;
		});
		UseItemCallback.EVENT.register((player, level, hand) -> {
			net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
			if (stack.is(PrimordialItems.MORSEL)) {
				return eatHeld(player, level, stack);
			}
			if (stack.is(PrimordialItems.CAPSULA)) {
				if (!level.isClientSide() && player instanceof ServerPlayer server && Creatures.isCreature(server)) {
					CreatureService.addDna(server, 40);
					CreatureService.addXp(server, 20);
					if (server.getRandom().nextFloat() < 0.35F) {
						java.util.List<Identifier> parts = com.primordial.part.PartRegistry.droppable();
						if (!parts.isEmpty()) {
							MorselDrops.award(server, server.level(), server.getX(), server.getY(), server.getZ(), parts.get(server.getRandom().nextInt(parts.size())));
						}
					}
					if (!player.isCreative()) {
						stack.shrink(1);
					}
				}
				return InteractionResult.SUCCESS;
			}
			if (com.primordial.creature.FeedingService.isFood(stack)) {
				if (!Creatures.isCreature(player)) {
					return InteractionResult.PASS;
				}
				if (!level.isClientSide() && player instanceof ServerPlayer server && com.primordial.creature.FeedingService.eat(server, stack) && !player.isCreative()) {
					stack.shrink(1);
				}
				return InteractionResult.SUCCESS;
			}
			if (!Creatures.isRestricted(player)) {
				return InteractionResult.PASS;
			}
			return com.primordial.capability.Capabilities.kind(stack) != null && com.primordial.capability.Capabilities.canUseTool(player, stack)
				? InteractionResult.PASS : InteractionResult.FAIL;
		});
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> Creatures.isRestricted(player) ? InteractionResult.FAIL : InteractionResult.PASS);

		CombatEffects.init();
		LootInjection.init();
		ServerTickEvents.END_LEVEL_TICK.register(PrimordialEvents::tickLevel);
	}

	/** 2% das criaturas nascem Alfa: brilham, têm o triplo de vida e sempre dropam Melhoria. Zumbis não. */
	private static void makeAlphaMaybe(Mob mob) {
		var def = com.primordial.entity.CreatureRegistry.of(mob.getType());
		if (def == null || def.has("undead") || def.has("still") || mob.getRandom().nextFloat() >= 0.02F) {
			return;
		}
		mob.addTag(MorselDrops.ALPHA_TAG);
		mob.setGlowingTag(true);
		mob.setCustomName(Component.translatable("msg.primordial.alpha", mob.getType().getDescription()));
		mob.setCustomNameVisible(true);
		var health = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
		if (health != null) {
			health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
				com.primordial.Primordial.id("alpha"), 2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			mob.setHealth(mob.getMaxHealth());
		}
	}

	private static void tickLevel(ServerLevel level) {
		com.primordial.world.OceanLife.tickLevel(level);
		com.primordial.world.TimedBlocks.tick(level);
		for (ServerPlayer player : level.players()) {
			if (!Creatures.isCreature(player) || player.isSpectator()) {
				continue;
			}
			eatNearbyMorsels(level, player);
			com.primordial.world.SeabedSeeder.tick(level, player);
			com.primordial.ability.Passives.tick(player);
			com.primordial.world.Geysers.tickPlayer(level, player);
			com.primordial.capability.InventoryRules.enforce(player);
			if (player.tickCount % 40 == 0) {
				com.primordial.journal.JournalService.scanNearby(player);
			}
			tickWave(level, player);
			com.primordial.world.SeabedSeeder.tick(level, player);
			com.primordial.ability.Passives.tick(player);
			tickWave(level, player);
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

	/** Salto de Golfinho desperto: ao voltar para a água, uma onda empurra quem está perto. */
	private static void tickWave(ServerLevel level, ServerPlayer player) {
		Long due = com.primordial.ability.Abilities.WAVE_PENDING.get(player.getUUID());
		if (due == null || level.getGameTime() < due || !player.isInWater()) {
			return;
		}
		com.primordial.ability.Abilities.WAVE_PENDING.remove(player.getUUID());
		for (net.minecraft.world.entity.LivingEntity target : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(4.0), e -> e != player)) {
			Vec3 away = target.position().subtract(player.position()).normalize().scale(1.0);
			target.setDeltaMovement(away.x, 0.3, away.z);
		}
		level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.3, player.getZ(), 60, 1.5, 0.3, 1.5, 0.2);
	}

	/** Necrófago: quando uma criatura morre perto, quem tem a boca necrófaga se alimenta da carcaça. */
	private static void feedScavengers(net.minecraft.world.entity.LivingEntity dead) {
		if (!(dead.level() instanceof ServerLevel level) || com.primordial.entity.CreatureRegistry.of(dead.getType()) == null) {
			return;
		}
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, dead.getBoundingBox().inflate(8.0))) {
			int power = Creatures.passiveLevel(player, "scavenger");
			if (power > 0 && Creatures.isCreature(player)) {
				player.heal(power >= 3 ? 4.0F : 2.0F);
				CreatureService.addDna(player, power >= 3 ? 3 : 1);
				level.sendParticles(ParticleTypes.COMPOSTER, dead.getX(), dead.getY() + 0.3, dead.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
			}
		}
	}

	/** Blocos que têm uso próprio (portas, baús, mesas...): colocar um bloco em cima deles abriria o bloco. */
	private static boolean interactive(net.minecraft.world.level.block.state.BlockState state) {
		net.minecraft.world.level.block.Block block = state.getBlock();
		return state.hasBlockEntity() || block instanceof net.minecraft.world.level.block.DoorBlock
			|| block instanceof net.minecraft.world.level.block.TrapDoorBlock || block instanceof net.minecraft.world.level.block.FenceGateBlock
			|| block instanceof net.minecraft.world.level.block.ButtonBlock || block instanceof net.minecraft.world.level.block.LeverBlock
			|| block instanceof net.minecraft.world.level.block.CraftingTableBlock || block instanceof net.minecraft.world.level.block.AnvilBlock
			|| block instanceof net.minecraft.world.level.block.BedBlock || block instanceof net.minecraft.world.level.block.NoteBlock;
	}

	private static final double EAT_REACH = 3.0;

	/** Botão direito com a Melhoria na mão: come e adquire a parte (o item some). */
	private static InteractionResult eatHeld(net.minecraft.world.entity.player.Player player, Level level, net.minecraft.world.item.ItemStack stack) {
		Identifier part = stack.get(PrimordialItems.PART);
		if (part == null || !Creatures.isCreature(player)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && player instanceof ServerPlayer server) {
			CreatureService.eatMorsel(server, part);
			if (!player.isCreative()) {
				stack.shrink(1);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Tecla C perto de comida no chão: come uma unidade direto do chão. */
	private static boolean eatFloorFood(ServerPlayer player) {
		for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(EAT_REACH), e -> com.primordial.creature.FeedingService.isFood(e.getItem()))) {
			if (com.primordial.creature.FeedingService.eat(player, item.getItem())) {
				item.getItem().shrink(1);
				if (item.getItem().isEmpty()) {
					item.discard();
				}
			}
			return true;
		}
		return false;
	}

	/** Come a primeira Melhoria do inventário (a da mão, se houver). */
	private static void eatFromInventory(ServerPlayer player) {
		net.minecraft.world.item.ItemStack held = player.getMainHandItem();
		net.minecraft.world.item.ItemStack chosen = held.is(PrimordialItems.MORSEL) ? held : null;
		if (chosen == null) {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(PrimordialItems.MORSEL)) {
					chosen = stack;
					break;
				}
			}
		}
		if (chosen == null) {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
				if (com.primordial.creature.FeedingService.isFood(stack)) {
					if (com.primordial.creature.FeedingService.eat(player, stack) && !player.isCreative()) {
						stack.shrink(1);
					}
					return;
				}
			}
			return;
		}
		Identifier part = chosen.get(PrimordialItems.PART);
		if (part != null) {
			CreatureService.eatMorsel(player, part);
			if (!player.isCreative()) {
				chosen.shrink(1);
			}
		}
	}

	/** Melhorias ficam paradas e brilhando. Comer é uma escolha: por padrão o jogador aperta C (ou ligue autoEatMorsels). */
	private static void eatNearbyMorsels(ServerLevel level, ServerPlayer player) {
		boolean auto = PrimordialConfig.get().autoEatMorsels;
		ItemEntity closest = null;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(20.0), e -> e.getItem().is(PrimordialItems.MORSEL))) {
			Identifier part = item.getItem().get(PrimordialItems.PART);
			if (part == null) {
				continue;
			}
			item.setDeltaMovement(Vec3.ZERO);
			if (item.tickCount % 10 == 0) {
				level.sendParticles(ParticleTypes.GLOW, item.getX(), item.getY() + 0.3, item.getZ(), 2, 0.15, 0.15, 0.15, 0.0);
			}
			if (auto && item.getBoundingBox().intersects(player.getBoundingBox().inflate(0.8))) {
				CreatureService.eatMorsel(player, part);
				item.discard();
			} else if (!auto && closeEnough(player, item) && (closest == null || item.distanceToSqr(player) < closest.distanceToSqr(player))) {
				closest = item;
			}
		}
		if (closest != null && player.tickCount % 10 == 0) {
			Identifier part = closest.getItem().get(PrimordialItems.PART);
			player.sendSystemMessage(Component.translatable("msg.primordial.eat_hint", PartDefinition.name(part)).withStyle(ChatFormatting.YELLOW), true);
		}
	}

	private static boolean closeEnough(ServerPlayer player, ItemEntity item) {
		return item.getBoundingBox().intersects(player.getBoundingBox().inflate(EAT_REACH));
	}

	/** O jogador apertou C: come a Melhoria mais próxima, se houver uma ao alcance. */
	public static void eatNearestMorsel(ServerPlayer player) {
		if (!Creatures.isCreature(player) || player.isSpectator()) {
			return;
		}
		ServerLevel level = player.level();
		ItemEntity closest = null;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(EAT_REACH), e -> e.getItem().is(PrimordialItems.MORSEL))) {
			if (item.getItem().get(PrimordialItems.PART) != null && (closest == null || item.distanceToSqr(player) < closest.distanceToSqr(player))) {
				closest = item;
			}
		}
		if (closest == null) {
			if (eatFloorFood(player)) {
				return;
			}
			if (!com.primordial.entity.Symbionts.tryFeed(player)) {
				eatFromInventory(player);
			}
			return;
		}
		CreatureService.eatMorsel(player, closest.getItem().get(PrimordialItems.PART));
		closest.discard();
	}
}
