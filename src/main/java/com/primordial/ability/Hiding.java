package com.primordial.ability;

import com.primordial.creature.FeedingService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Esconderijos: quem está escondido não é caçado pelos predadores. Vale para kelp denso, Fingir de Morto e Enterrar-se. */
public final class Hiding {
	private static final Map<UUID, Long> HIDDEN_UNTIL = new HashMap<>();

	private Hiding() {
	}

	public static void hide(ServerPlayer player, int ticks) {
		HIDDEN_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
	}

	public static void reveal(ServerPlayer player) {
		HIDDEN_UNTIL.remove(player.getUUID());
	}

	public static boolean isHidden(Player player) {
		Long until = HIDDEN_UNTIL.get(player.getUUID());
		if (until != null && player.level().getGameTime() < until) {
			return true;
		}
		// Dentro de kelp ou de capim-marinho alto a criatura some do olhar dos predadores.
		BlockPos pos = player.blockPosition();
		return FeedingService.isPlant(player.level().getBlockState(pos)) || FeedingService.isPlant(player.level().getBlockState(pos.above()));
	}
}
