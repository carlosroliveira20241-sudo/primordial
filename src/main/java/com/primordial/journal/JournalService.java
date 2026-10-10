package com.primordial.journal;

import com.primordial.creature.CreatureService;
import com.primordial.entity.CreatureRegistry;
import com.primordial.registry.PrimordialAttachments;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Mantém o diário: conta o que o jogador faz, registra criaturas vistas e entrega as recompensas dos desafios. */
public final class JournalService {
	private JournalService() {
	}

	public static JournalState state(Player player) {
		return player.getAttachedOrElse(PrimordialAttachments.JOURNAL, JournalState.DEFAULT);
	}

	private static void save(ServerPlayer player, JournalState state) {
		player.setAttached(PrimordialAttachments.JOURNAL, state);
	}

	/** Soma {@code amount} ao contador e confere os desafios. */
	public static void bump(ServerPlayer player, String key, int amount) {
		JournalState state = state(player);
		set(player, state, key, state.counter(key) + amount);
	}

	/** Para valores que só sobem (nível, estágio). */
	public static void raise(ServerPlayer player, String key, int value) {
		JournalState state = state(player);
		if (value > state.counter(key)) {
			set(player, state, key, value);
		}
	}

	private static void set(ServerPlayer player, JournalState state, String key, int value) {
		save(player, state.withCounter(key, value));
		check(player);
	}

	public static void recordKill(ServerPlayer player, Identifier species) {
		save(player, state(player).withKill(species));
		bump(player, "kills", 1);
		bump(player, "kill:" + species.getPath(), 1);
	}

	/** Anota as criaturas do mod que estão por perto no bestiário. */
	public static void scanNearby(ServerPlayer player) {
		JournalState state = state(player);
		for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24.0), e -> CreatureRegistry.of(e.getType()) != null)) {
			Identifier species = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
			if (!state.hasSeen(species)) {
				state = state.withSeen(species);
				save(player, state);
				player.sendSystemMessage(Component.translatable("msg.primordial.bestiary_new", mob.getType().getDescription()).withStyle(ChatFormatting.YELLOW), true);
			}
		}
	}

	private static void check(ServerPlayer player) {
		JournalState state = state(player);
		for (Map.Entry<String, Challenge> entry : ChallengeRegistry.all().entrySet()) {
			Challenge challenge = entry.getValue();
			if (state.done().contains(entry.getKey()) || state.counter(challenge.counter()) < challenge.goal()) {
				continue;
			}
			save(player, state.withDone(entry.getKey()));
			CreatureService.addDna(player, challenge.dna());
			CreatureService.addXp(player, challenge.xp());
			player.sendSystemMessage(Component.translatable("msg.primordial.challenge_done", Challenge.name(entry.getKey()), challenge.dna())
				.withStyle(ChatFormatting.GOLD));
			CreatureService.playSound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
			state = state(player);
		}
	}
}
