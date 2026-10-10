package com.primordial.creature;

import com.primordial.config.PrimordialConfig;
import com.primordial.net.Payloads;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import com.primordial.registry.PrimordialAttachments;
import com.primordial.world.ReefFinder;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.primordial.look.LookRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jspecify.annotations.Nullable;

/** Regras da evolução, sempre executadas no servidor (o cliente só pede). */
public final class CreatureService {
	/** Partes que toda criatura já tem desde o começo. */
	private static final Set<String> STARTER_PARTS = Set.of("cauda_simples", "branquias", "olhos_simples", "barbatanas_simples");
	public static final int LOOK_CHANGE_COST = 5;

	private CreatureService() {
	}

	// ------------------------------------------------------------------ leitura/escrita

	public static void setBody(ServerPlayer player, CreatureBody body) {
		player.setAttached(PrimordialAttachments.BODY, body);
		CreatureStats.apply(player);
	}

	public static void setProgress(ServerPlayer player, CreatureProgress progress) {
		player.setAttached(PrimordialAttachments.PROGRESS, progress);
	}

	// ------------------------------------------------------------------ origem

	public static void chooseOrigin(ServerPlayer player, Origin origin, String rawName, int primary, int secondary, Pattern pattern, Identifier look) {
		CreatureProgress progress = Creatures.progress(player);
		if (progress.originChosen()) {
			return;
		}

		String name = ChatFormatting.stripFormatting(rawName == null ? "" : rawName).trim();
		if (name.length() > 24) {
			name = name.substring(0, 24);
		}
		if (name.isEmpty()) {
			name = player.getGameProfile().name() + "us";
		}

		progress = progress.withOrigin(origin);
		progress = progress.withEaten(origin.startingMouth(), 1).withBought(origin.startingMouth());
		for (String starter : STARTER_PARTS) {
			Identifier id = com.primordial.Primordial.id(starter);
			progress = progress.withEaten(id, 1).withBought(id);
		}

		CreatureBody body = CreatureBody.DEFAULT.withActive(true).withStage(Stage.LARVA)
			.withLook(primary & 0xFFFFFF, secondary & 0xFFFFFF, CreatureBody.DEFAULT_DETAIL, pattern)
			.withBodyLook(validLook(look))
			.withSpeciesName(name)
			.withPart(BodySlot.MOUTH, new PartRef(origin.startingMouth(), 1))
			.withPart(BodySlot.TAIL, new PartRef(com.primordial.Primordial.id("cauda_simples"), 1))
			.withPart(BodySlot.ORGAN_1, new PartRef(com.primordial.Primordial.id("branquias"), 1));

		setProgress(player, progress);
		setBody(player, body);
		player.getInventory().clearContent();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0F);

		ServerLevel level = player.level();
		BlockPos reef = ReefFinder.findReef(level, player.blockPosition());
		if (reef != null) {
			teleport(player, level, reef);
			setProgress(player, Creatures.progress(player).withNest(Optional.of(GlobalPos.of(level.dimension(), reef))));
		}

		player.sendSystemMessage(Component.translatable("msg.primordial.welcome", name).withStyle(ChatFormatting.AQUA));
		playSound(player, SoundEvents.CONDUIT_ACTIVATE, 1.0F, 1.2F);
	}

	// ------------------------------------------------------------------ DNA e XP

	public static void addDna(ServerPlayer player, int amount) {
		if (amount == 0) {
			return;
		}
		CreatureProgress progress = Creatures.progress(player);
		setProgress(player, progress.withDna(progress.dna() + amount));
	}

	public static void addXp(ServerPlayer player, int amount) {
		if (amount <= 0) {
			return;
		}
		CreatureProgress progress = Creatures.progress(player);
		int xp = progress.xp() + amount;
		int level = progress.level();
		boolean leveled = false;
		while (xp >= CreatureProgress.xpForNextLevel(level)) {
			xp -= CreatureProgress.xpForNextLevel(level);
			level++;
			leveled = true;
		}
		setProgress(player, progress.withXpAndLevel(xp, level));
		if (leveled) {
			player.sendSystemMessage(Component.translatable("msg.primordial.level_up", level).withStyle(ChatFormatting.GREEN), true);
			playSound(player, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
			if (canEvolve(player)) {
				player.sendSystemMessage(Component.translatable("msg.primordial.evolve_ready").withStyle(ChatFormatting.GOLD));
			}
		}
	}

	// ------------------------------------------------------------------ Melhorias

	/** O jogador comeu a Melhoria de uma parte: desbloqueia ou aumenta a afinidade. */
	public static void eatMorsel(ServerPlayer player, Identifier partId) {
		PartDefinition def = PartRegistry.get(partId);
		if (def == null || !Creatures.isCreature(player)) {
			return;
		}
		CreatureProgress progress = Creatures.progress(player);
		int before = progress.partLevel(partId);
		progress = progress.withEaten(partId, progress.eatenCount(partId) + 1);
		int after = progress.partLevel(partId);
		setProgress(player, progress);

		player.getFoodData().eat(2, 0.3F);
		addXp(player, 6);

		if (before == 0) {
			ServerPlayNetworking.send(player, new Payloads.PartToast(partId, after, Payloads.PartToast.Kind.UNLOCKED));
			autoEquip(player, partId);
			playSound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1.6F);
		} else if (after > before) {
			refreshEquippedLevel(player, partId, after);
			ServerPlayNetworking.send(player, new Payloads.PartToast(partId, after,
				after >= 3 ? Payloads.PartToast.Kind.AWAKENED : Payloads.PartToast.Kind.LEVEL_UP));
			if (after >= 3) {
				playSound(player, SoundEvents.TOTEM_USE, 0.4F, 1.5F);
				player.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 0.3, player.getZ(), 40, 0.4, 0.4, 0.4, 0.3);
			} else {
				playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.3F);
			}
		} else {
			playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.0F);
		}
	}

	private static void refreshEquippedLevel(ServerPlayer player, Identifier partId, int level) {
		CreatureBody body = Creatures.body(player);
		for (var entry : body.parts().entrySet()) {
			if (entry.getValue().id().equals(partId)) {
				body = body.withPart(entry.getKey(), new PartRef(partId, level));
			}
		}
		setBody(player, body);
	}

	// ------------------------------------------------------------------ Editor

	/** Motivo de não poder equipar, ou null se pode. Usado também pelo cliente para mostrar a dica. */
	public static @Nullable Component equipProblem(CreatureBody body, CreatureProgress progress, BodySlot slot, Identifier partId) {
		PartDefinition def = PartRegistry.get(partId);
		if (def == null) {
			return Component.translatable("msg.primordial.unknown_part");
		}
		if (!body.stage().slots.contains(slot)) {
			return Component.translatable("msg.primordial.slot_locked");
		}
		if (def.slot() != slot.type()) {
			return Component.translatable("msg.primordial.wrong_slot");
		}
		if (!progress.unlocked(partId)) {
			return Component.translatable("msg.primordial.part_locked");
		}
		if (body.stage().number() < def.minStage()) {
			return Component.translatable("msg.primordial.needs_stage", Component.translatable(Stage.values()[def.minStage() - 1].translationKey()));
		}
		for (var entry : body.parts().entrySet()) {
			if (entry.getKey() != slot && entry.getValue().id().equals(partId)) {
				return Component.translatable("msg.primordial.already_equipped");
			}
		}
		if (!progress.hasBought(partId) && progress.dna() < def.cost()) {
			return Component.translatable("msg.primordial.not_enough_dna", def.cost());
		}
		return null;
	}

	public static void equip(ServerPlayer player, BodySlot slot, Optional<Identifier> part) {
		CreatureBody body = Creatures.body(player);
		CreatureProgress progress = Creatures.progress(player);
		if (!body.active()) {
			return;
		}
		if (part.isEmpty()) {
			// A boca e o primeiro órgão nunca ficam vazios.
			if (slot != BodySlot.MOUTH && slot != BodySlot.ORGAN_1) {
				setBody(player, body.withPart(slot, null));
			}
			return;
		}

		Identifier id = part.get();
		Component problem = equipProblem(body, progress, slot, id);
		if (problem != null) {
			player.sendSystemMessage(problem.copy().withStyle(ChatFormatting.RED), true);
			return;
		}
		PartDefinition def = PartRegistry.get(id);
		if (!progress.hasBought(id)) {
			progress = progress.withDna(progress.dna() - def.cost()).withBought(id);
			setProgress(player, progress);
		}
		setBody(player, body.withPart(slot, new PartRef(id, Math.max(1, progress.partLevel(id)))));
		playSound(player, SoundEvents.ALLAY_ITEM_TAKEN, 0.8F, 1.2F);
	}

	/** Sem editor no protótipo: a parte desbloqueada vai sozinha para um slot livre (ou no lugar de uma parte inicial). */
	public static void autoEquip(ServerPlayer player, Identifier partId) {
		PartDefinition def = PartRegistry.get(partId);
		CreatureBody body = Creatures.body(player);
		if (def == null || def.base() || body.has(partId) || body.stage().number() < def.minStage()) {
			return;
		}
		BodySlot target = null;
		for (BodySlot slot : body.stage().slots) {
			if (slot.type() != def.slot()) {
				continue;
			}
			PartRef current = body.part(slot);
			if (current == null) {
				target = slot;
				break;
			}
			PartDefinition currentDef = PartRegistry.get(current.id());
			if (target == null && currentDef != null && currentDef.base()) {
				target = slot;
			}
		}
		if (target == null) {
			return;
		}
		CreatureProgress progress = Creatures.progress(player);
		setProgress(player, progress.withBought(partId));
		setBody(player, body.withPart(target, new PartRef(partId, Math.max(1, progress.partLevel(partId)))));
		player.sendSystemMessage(Component.translatable("msg.primordial.equipped", PartDefinition.name(partId)).withStyle(ChatFormatting.AQUA));
	}

	/** Só aceita aparências marcadas como escolhíveis; qualquer outra vira a padrão (o cliente não manda o que quiser). */
	private static Identifier validLook(Identifier look) {
		return look != null && LookRegistry.selectable().contains(look) ? look : com.primordial.look.Look.DEFAULT_ID;
	}

	public static void setLook(ServerPlayer player, int primary, int secondary, int detail, Pattern pattern, Identifier look) {
		CreatureBody body = Creatures.body(player);
		CreatureProgress progress = Creatures.progress(player);
		if (!body.active()) {
			return;
		}
		if (progress.dna() < LOOK_CHANGE_COST) {
			player.sendSystemMessage(Component.translatable("msg.primordial.not_enough_dna", LOOK_CHANGE_COST).withStyle(ChatFormatting.RED), true);
			return;
		}
		setProgress(player, progress.withDna(progress.dna() - LOOK_CHANGE_COST));
		setBody(player, body.withLook(primary & 0xFFFFFF, secondary & 0xFFFFFF, detail & 0xFFFFFF, pattern).withBodyLook(validLook(look)));
	}

	// ------------------------------------------------------------------ evolução

	/** O que falta para evoluir (null = pode evoluir). Funciona no cliente também. */
	public static @Nullable Component evolveProblem(CreatureBody body, CreatureProgress progress) {
		Stage stage = body.stage();
		if (!stage.hasNext()) {
			return Component.translatable("msg.primordial.final_stage");
		}
		if (!stage.next().available) {
			return Component.translatable("msg.primordial.stage_unavailable");
		}
		if (progress.level() < stage.levelToEvolve) {
			return Component.translatable("msg.primordial.needs_level", stage.levelToEvolve);
		}
		if (stage == Stage.PEIXE && progress.bigKills() < 1) {
			return Component.translatable("msg.primordial.needs_big_kill");
		}
		return null;
	}

	public static boolean canEvolve(ServerPlayer player) {
		return Creatures.isCreature(player) && evolveProblem(Creatures.body(player), Creatures.progress(player)) == null;
	}

	public static void evolve(ServerPlayer player) {
		CreatureBody body = Creatures.body(player);
		Component problem = evolveProblem(body, Creatures.progress(player));
		if (!body.active() || problem != null) {
			if (problem != null) {
				player.sendSystemMessage(problem.copy().withStyle(ChatFormatting.RED), true);
			}
			return;
		}
		Stage next = body.stage().next();
		body = body.withStage(next);
		// Slots novos já vêm com a peça simples, para o bicho não ficar "pelado".
		if (next.slots.contains(BodySlot.EYES) && body.part(BodySlot.EYES) == null) {
			body = body.withPart(BodySlot.EYES, new PartRef(com.primordial.Primordial.id("olhos_simples"), 1));
		}
		if (next.slots.contains(BodySlot.FINS) && body.part(BodySlot.FINS) == null) {
			body = body.withPart(BodySlot.FINS, new PartRef(com.primordial.Primordial.id("barbatanas_simples"), 1));
		}
		setBody(player, body);
		for (Identifier id : Creatures.progress(player).eaten().keySet()) {
			autoEquip(player, id);
		}
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);

		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 0.3, player.getZ(), 60, 0.6, 0.6, 0.6, 0.05);
		level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, player.getX(), player.getY(), player.getZ(), 40, 0.5, 0.3, 0.5, 0.2);
		playSound(player, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.0F);
		ServerPlayNetworking.send(player, new Payloads.Evolved(next.number()));
		Component stageName = Component.translatable(next.translationKey());
		level.getServer().getPlayerList().broadcastSystemMessage(
			Component.translatable("msg.primordial.evolved_broadcast", player.getDisplayName(), stageName).withStyle(ChatFormatting.GOLD), false);
	}

	// ------------------------------------------------------------------ ninho

	public static void setNest(ServerPlayer player) {
		if (!Creatures.isCreature(player)) {
			return;
		}
		ServerLevel level = player.level();
		setProgress(player, Creatures.progress(player).withNest(Optional.of(GlobalPos.of(level.dimension(), player.blockPosition()))));
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.2, player.getZ(), 15, 0.4, 0.3, 0.4, 0.0);
		playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
		player.sendSystemMessage(Component.translatable("msg.primordial.nest_set").withStyle(ChatFormatting.GREEN), true);
	}

	public static boolean isNearOwnNest(ServerPlayer player, int radius) {
		Optional<GlobalPos> nest = Creatures.progress(player).nest();
		return nest.isPresent() && nest.get().dimension() == player.level().dimension()
			&& nest.get().pos().closerToCenterThan(player.position(), radius);
	}

	// ------------------------------------------------------------------ morte e PvP

	/** Chamado quando uma criatura morre. Retorna o DNA perdido. */
	public static int applyDeathPenalty(ServerPlayer victim) {
		CreatureProgress progress = Creatures.progress(victim);
		int loss = Mth.floor(progress.dna() * PrimordialConfig.get().deathDnaLoss);
		if (loss > 0) {
			setProgress(victim, progress.withDna(progress.dna() - loss));
			victim.sendSystemMessage(Component.translatable("msg.primordial.death_dna", loss).withStyle(ChatFormatting.RED));
		}
		return loss;
	}

	/** DNA roubado por um jogador ao matar outro, considerando a diferença de estágio. */
	public static int stealAmount(ServerPlayer killer, ServerPlayer victim, int victimDnaBefore) {
		int killerStage = Creatures.body(killer).stage().number();
		int victimStage = Creatures.body(victim).stage().number();
		double multiplier = victimStage > killerStage ? 2.0 : victimStage < killerStage ? 0.25 : 1.0;
		return Mth.floor(victimDnaBefore * PrimordialConfig.get().pvpDnaSteal * multiplier);
	}

	public static void onRespawn(ServerPlayer player) {
		CreatureStats.apply(player);
		if (!Creatures.isCreature(player)) {
			return;
		}
		player.setHealth(player.getMaxHealth());
		int protection = PrimordialConfig.get().respawnProtectionSeconds * 20;
		if (protection > 0) {
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, protection, 4, false, false, true));
		}

		Optional<GlobalPos> nest = Creatures.progress(player).nest();
		ServerLevel target = nest.map(n -> player.level().getServer().getLevel(n.dimension())).orElse(null);
		if (target != null) {
			teleport(player, target, nest.get().pos());
		} else {
			BlockPos reef = ReefFinder.findReef(player.level(), player.blockPosition());
			if (reef != null) {
				teleport(player, player.level(), reef);
			}
		}
	}

	// ------------------------------------------------------------------ util

	public static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos) {
		player.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
	}

	public static void playSound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}
}
