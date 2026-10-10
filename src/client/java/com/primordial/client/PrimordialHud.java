package com.primordial.client;

import com.primordial.ability.AbilityManager;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureProgress;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Painel simples no canto da tela: nível, DNA, estágio e habilidades. */
public final class PrimordialHud {
	private static final String[] KEYS = {"Z", "X"};

	private PrimordialHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.gui.hud.isHidden() || !Creatures.isCreature(player)) {
			return;
		}
		CreatureBody body = Creatures.body(player);
		CreatureProgress progress = Creatures.progress(player);

		int y = 6;
		y = line(graphics, mc, Component.translatable("hud.primordial.stage", Component.translatable(body.stage().translationKey())), y, 0xFFFFD54F);
		y = line(graphics, mc, Component.translatable("hud.primordial.level", progress.level(), progress.xp(), CreatureProgress.xpForNextLevel(progress.level())), y, 0xFFFFFFFF);
		y = line(graphics, mc, Component.translatable("hud.primordial.dna", progress.dna()), y, 0xFF80DEEA);

		List<Identifier> abilities = AbilityManager.abilitiesOf(player);
		for (int i = 0; i < abilities.size() && i < KEYS.length; i++) {
			y = line(graphics, mc, Component.literal("[" + KEYS[i] + "] ").append(Component.translatable("ability.primordial." + abilities.get(i).getPath())), y, 0xFFB2FF59);
		}
		if (com.primordial.mutation.MutationService.state(player).pending() > 0) {
			y = line(graphics, mc, Component.translatable("hud.primordial.mutation_ready"), y, 0xFFE1A8FF);
		}
		if (CreatureService.evolveProblem(body, progress) == null) {
			line(graphics, mc, Component.translatable("hud.primordial.evolve_ready"), y, 0xFFFFAB40);
		}
	}

	private static int line(GuiGraphicsExtractor graphics, Minecraft mc, Component text, int y, int color) {
		graphics.text(mc.font, text, 6, y, color);
		return y + 11;
	}
}
