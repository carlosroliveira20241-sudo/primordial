package com.primordial.ability;

import net.minecraft.server.level.ServerPlayer;

/**
 * Uma habilidade ativa. Para criar uma nova: escreva uma implementação em {@link Abilities}, registre lá com um nome
 * e aponte para esse nome no campo {@code "ability"} do JSON de uma parte. Mais nada precisa mudar.
 */
public interface Ability {
	/** Recarga em ticks (20 = 1 segundo). */
	int cooldownTicks();

	/**
	 * @param level nível da parte que dá a habilidade (1 a 3). No nível 3 a parte está "desperta":
	 *              use {@link #awakened(int)} para dar o efeito extra do Despertar.
	 */
	void use(ServerPlayer player, int level);

	@FunctionalInterface
	interface Effect {
		void apply(ServerPlayer player, int level);
	}

	static boolean awakened(int level) {
		return level >= 3;
	}
}
