package com.primordial.creature;

import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import com.primordial.registry.PrimordialAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Atalhos para ler o estado de criatura de um jogador (funciona no cliente e no servidor). */
public final class Creatures {
	private Creatures() {
	}

	public static CreatureBody body(Player player) {
		return player.getAttachedOrElse(PrimordialAttachments.BODY, CreatureBody.DEFAULT);
	}

	/** No cliente, só existe para o próprio jogador. */
	public static CreatureProgress progress(Player player) {
		return player.getAttachedOrElse(PrimordialAttachments.PROGRESS, CreatureProgress.DEFAULT);
	}

	/** É uma criatura (já escolheu a origem)? */
	public static boolean isCreature(Player player) {
		return body(player).active();
	}

	/** Criatura aquática: respira na água e se sufoca fora dela. */
	public static boolean isAquatic(Player player) {
		CreatureBody body = body(player);
		return body.active() && body.stage().aquatic && !player.isSpectator();
	}

	/** Criativo e espectador ignoram as travas de criatura (útil para construir e testar). */
	public static boolean ignoresRestrictions(Player player) {
		return player.isCreative() || player.isSpectator();
	}

	/** Uma criatura que está sujeita às travas (não quebra blocos, não usa itens...). */
	public static boolean isRestricted(Player player) {
		return isCreature(player) && !ignoresRestrictions(player);
	}

	public static Diet diet(Player player) {
		PartRef mouth = body(player).part(BodySlot.MOUTH);
		if (mouth != null) {
			PartDefinition def = PartRegistry.get(mouth.id());
			if (def != null && def.diet().isPresent()) {
				return def.diet().get();
			}
		}
		return Diet.OMNIVORE;
	}

	/** Alguma parte equipada tem esse efeito passivo? Retorna o nível da parte (0 = não tem). */
	public static int passiveLevel(Player player, String passive) {
		CreatureBody body = body(player);
		if (!body.active()) {
			return 0;
		}
		int best = 0;
		for (PartRef ref : body.parts().values()) {
			PartDefinition def = PartRegistry.get(ref.id());
			if (def != null && def.hasPassive(passive)) {
				best = Math.max(best, ref.level());
			}
		}
		return best;
	}

	public static boolean hasPassive(Player player, String passive) {
		return passiveLevel(player, passive) > 0;
	}

	public static @Nullable PartDefinition partIn(Player player, BodySlot slot) {
		PartRef ref = body(player).part(slot);
		return ref == null ? null : PartRegistry.get(ref.id());
	}

	public static boolean hasPart(Player player, Identifier part) {
		return body(player).has(part);
	}
}
