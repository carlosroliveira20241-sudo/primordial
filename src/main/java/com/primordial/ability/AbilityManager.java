package com.primordial.ability;

import com.primordial.creature.BodySlot;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Liga as partes equipadas às habilidades registradas em {@link Abilities}: lista, recarga e uso. */
public final class AbilityManager {
	private static final Map<UUID, Map<String, Long>> LAST_USED = new HashMap<>();

	private AbilityManager() {
	}

	/** Habilidades que a criatura tem agora, na ordem dos slots do corpo (a posição = tecla Z, X...). */
	public static List<Identifier> abilitiesOf(Player player) {
		CreatureBody body = Creatures.body(player);
		List<Identifier> result = new ArrayList<>();
		for (BodySlot slot : body.stage().slots) {
			PartRef ref = body.part(slot);
			if (ref == null) {
				continue;
			}
			PartDefinition def = PartRegistry.get(ref.id());
			if (def != null && def.ability().isPresent() && Abilities.exists(def.ability().get().getPath())
				&& !result.contains(def.ability().get())) {
				result.add(def.ability().get());
			}
		}
		List<Identifier> order = player.getAttachedOrElse(com.primordial.registry.PrimordialAttachments.ABILITY_ORDER, AbilityOrder.DEFAULT).order();
		result.sort(java.util.Comparator.comparingInt(id -> order.contains(id) ? order.indexOf(id) : Integer.MAX_VALUE));
		return result;
	}

	/** Guarda a ordem que o jogador escolheu (só aceita habilidades que ele realmente tem). */
	public static void setOrder(ServerPlayer player, List<Identifier> wanted) {
		List<Identifier> owned = abilitiesOf(player);
		List<Identifier> clean = new ArrayList<>();
		for (Identifier id : wanted) {
			if (owned.contains(id) && !clean.contains(id)) {
				clean.add(id);
			}
		}
		player.setAttached(com.primordial.registry.PrimordialAttachments.ABILITY_ORDER, new AbilityOrder(clean));
	}

	/** Nível da parte equipada que dá essa habilidade. */
	private static int levelOf(Player player, Identifier ability) {
		CreatureBody body = Creatures.body(player);
		for (PartRef ref : body.parts().values()) {
			PartDefinition def = PartRegistry.get(ref.id());
			if (def != null && def.ability().filter(ability::equals).isPresent()) {
				return ref.level();
			}
		}
		return 1;
	}

	public static void use(ServerPlayer player, int index) {
		List<Identifier> abilities = abilitiesOf(player);
		if (!Creatures.isCreature(player) || index < 0 || index >= abilities.size()) {
			return;
		}
		Identifier id = abilities.get(index);
		String name = id.getPath();
		Ability ability = Abilities.get(name);
		long now = player.level().getGameTime();
		Map<String, Long> used = LAST_USED.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		long readyAt = used.getOrDefault(name, 0L) + (long) (ability.cooldownTicks() * com.primordial.mutation.MutationEffects.cooldownFactor(player));
		if (now < readyAt) {
			player.sendSystemMessage(Component.translatable("msg.primordial.cooldown", (readyAt - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		used.put(name, now);
		ability.use(player, levelOf(player, id));
	}

	public static void makeNest(ServerPlayer player) {
		CreatureService.setNest(player);
	}
}
