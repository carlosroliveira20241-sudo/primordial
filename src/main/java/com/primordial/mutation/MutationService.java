package com.primordial.mutation;

import com.primordial.creature.CreatureStats;
import com.primordial.registry.PrimordialAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public final class MutationService {
	public static final int LEVELS_PER_CHOICE = 3;

	private MutationService() {
	}

	public static MutationState state(Player player) {
		return player.getAttachedOrElse(PrimordialAttachments.MUTATIONS, MutationState.DEFAULT);
	}

	/** O jogador subiu de nível: a cada 3 níveis ganha uma escolha de mutação. */
	public static void onLevels(ServerPlayer player, int oldLevel, int newLevel) {
		int gained = 0;
		for (int level = oldLevel + 1; level <= newLevel; level++) {
			if (level % LEVELS_PER_CHOICE == 0) {
				gained++;
			}
		}
		if (gained == 0) {
			return;
		}
		MutationState state = state(player);
		List<Identifier> offer = state.offer().isEmpty() ? roll(player.getRandom(), state) : state.offer();
		player.setAttached(PrimordialAttachments.MUTATIONS, state.withPending(state.pending() + gained, offer));
		player.sendSystemMessage(Component.translatable("msg.primordial.mutation_ready").withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** Escolhe a carta {@code id} entre as oferecidas. */
	public static void choose(ServerPlayer player, Identifier id) {
		MutationState state = state(player);
		if (state.pending() <= 0 || !state.offer().contains(id) || MutationRegistry.get(id) == null) {
			return;
		}
		MutationState next = state.withChosen(id, state.pending() - 1, List.of());
		if (next.pending() > 0) {
			next = next.withPending(next.pending(), roll(player.getRandom(), next));
		}
		player.setAttached(PrimordialAttachments.MUTATIONS, next);
		CreatureStats.apply(player);
		com.primordial.journal.JournalService.bump(player, "mutation", 1);
		player.sendSystemMessage(Component.translatable("msg.primordial.mutation_chosen", Mutation.name(id)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
	}

	/** Sorteia até 3 cartas que o jogador ainda não tem; as mais raras aparecem menos. */
	private static List<Identifier> roll(RandomSource random, MutationState state) {
		List<Identifier> pool = new ArrayList<>();
		MutationRegistry.all().forEach((id, mutation) -> {
			if (!state.chosen().contains(id)) {
				int weight = switch (mutation.rarity()) {
					case COMMON -> 6;
					case RARE -> 3;
					default -> 1;
				};
				for (int i = 0; i < weight; i++) {
					pool.add(id);
				}
			}
		});
		List<Identifier> offer = new ArrayList<>();
		while (offer.size() < 3 && !pool.isEmpty()) {
			Identifier pick = pool.get(random.nextInt(pool.size()));
			offer.add(pick);
			pool.removeIf(pick::equals);
		}
		return offer;
	}
}
