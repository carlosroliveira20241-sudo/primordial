package com.primordial.intellect;

import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.registry.PrimordialAttachments;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/** Regras da inteligência: melhorar o cérebro, comprar habilidades e colocá-las nos slots. Tudo validado no servidor. */
public final class IntellectService {
	private IntellectService() {
	}

	public static IntellectState state(Player player) {
		return player.getAttachedOrElse(PrimordialAttachments.INTELLECT, IntellectState.DEFAULT);
	}

	/** O teto do cérebro depende do estágio (plano: 3, 3, 8, 12... limitado a 8 por enquanto). */
	public static int brainCap(Player player) {
		return Math.min(IntellectState.MAX_BRAIN, Creatures.body(player).stage().intelligenceCap);
	}

	/** O teto da força por estágio. */
	public static int strengthCap(Player player) {
		int[] byStage = {3, 5, 8, 9, 10, 10};
		return byStage[Math.min(byStage.length - 1, Creatures.body(player).stage().ordinal())];
	}

	public static int strengthCost(int strength) {
		return 40 * strength;
	}

	public static void upgradeStrength(ServerPlayer player) {
		IntellectState state = state(player);
		if (!Creatures.isCreature(player)) {
			return;
		}
		if (state.strength() >= strengthCap(player)) {
			player.sendSystemMessage(Component.translatable("msg.primordial.strength_cap").withStyle(ChatFormatting.RED), true);
			return;
		}
		int cost = strengthCost(state.strength());
		if (Creatures.progress(player).dna() < cost) {
			player.sendSystemMessage(Component.translatable("msg.primordial.not_enough_dna", cost).withStyle(ChatFormatting.RED), true);
			return;
		}
		CreatureService.addDna(player, -cost);
		player.setAttached(PrimordialAttachments.INTELLECT, state.withStrength(state.strength() + 1));
		player.sendSystemMessage(Component.translatable("msg.primordial.strength_up", com.primordial.capability.InventoryRules.slotsForStrength(state.strength() + 1)).withStyle(ChatFormatting.GOLD), true);
		CreatureService.playSound(player, SoundEvents.PLAYER_LEVELUP, 0.7F, 0.9F);
	}

	public static int upgradeCost(int brain) {
		return 50 * brain;
	}

	/** Nível de poder das habilidades de inteligência: 1 (cérebro 1-2), 2 (3-5) ou 3 (6+, "desperta"). */
	public static int power(Player player) {
		return Math.min(3, 1 + state(player).brain() / 3);
	}

	public static void upgradeBrain(ServerPlayer player) {
		IntellectState state = state(player);
		if (!Creatures.isCreature(player)) {
			return;
		}
		if (state.brain() >= brainCap(player)) {
			player.sendSystemMessage(Component.translatable("msg.primordial.brain_cap").withStyle(ChatFormatting.RED), true);
			return;
		}
		int cost = upgradeCost(state.brain());
		if (Creatures.progress(player).dna() < cost) {
			player.sendSystemMessage(Component.translatable("msg.primordial.not_enough_dna", cost).withStyle(ChatFormatting.RED), true);
			return;
		}
		CreatureService.addDna(player, -cost);
		player.setAttached(PrimordialAttachments.INTELLECT, state.withBrain(state.brain() + 1));
		player.sendSystemMessage(Component.translatable("msg.primordial.brain_up", state.brain() + 1).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		CreatureService.playSound(player, SoundEvents.PLAYER_LEVELUP, 0.7F, 0.6F);
	}

	public static void buy(ServerPlayer player, Identifier ability) {
		IntellectState state = state(player);
		IntellectDef def = IntellectRegistry.get(ability);
		if (def == null || state.owned().contains(ability) || !Creatures.isCreature(player)) {
			return;
		}
		if (state.brain() < def.brain()) {
			player.sendSystemMessage(Component.translatable("msg.primordial.brain_needed", def.brain()).withStyle(ChatFormatting.RED), true);
			return;
		}
		if (Creatures.progress(player).dna() < def.cost()) {
			player.sendSystemMessage(Component.translatable("msg.primordial.not_enough_dna", def.cost()).withStyle(ChatFormatting.RED), true);
			return;
		}
		CreatureService.addDna(player, -def.cost());
		state = state.withOwned(ability);
		// vai sozinha para o primeiro slot livre
		for (int i = 0; i < state.slotCount(); i++) {
			if (state.slot(i) == null) {
				state = state.withSlot(i, ability.toString());
				break;
			}
		}
		player.setAttached(PrimordialAttachments.INTELLECT, state);
		player.sendSystemMessage(Component.translatable("msg.primordial.intellect_bought", Component.translatable("ability." + ability.getNamespace() + "." + ability.getPath()))
			.withStyle(ChatFormatting.GREEN), true);
		CreatureService.playSound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.8F, 1.2F);
	}

	public static void setSlot(ServerPlayer player, int index, Optional<Identifier> ability) {
		IntellectState state = state(player);
		if (index < 0 || index >= state.slotCount()) {
			return;
		}
		if (ability.isEmpty()) {
			player.setAttached(PrimordialAttachments.INTELLECT, state.withSlot(index, ""));
			return;
		}
		Identifier id = ability.get();
		if (!state.owned().contains(id)) {
			return;
		}
		// uma habilidade só pode estar em um slot
		for (int i = 0; i < IntellectState.MAX_SLOTS; i++) {
			if (id.toString().equals(i < state.slots().size() ? state.slots().get(i) : "")) {
				state = state.withSlot(i, "");
			}
		}
		player.setAttached(PrimordialAttachments.INTELLECT, state.withSlot(index, id.toString()));
	}
}
