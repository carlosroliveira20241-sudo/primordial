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
	private static final String[] KEYS = {"Z", "X", "B", "H"};

	private static java.util.List<net.minecraft.core.BlockPos> danger = java.util.List.of();
	private static long dangerTime;

	private PrimordialHud() {
	}

	public static void setDanger(java.util.List<net.minecraft.core.BlockPos> hunters) {
		danger = hunters;
		dangerTime = System.currentTimeMillis();
	}

	/** Marcadores vermelhos na borda da tela mostrando de onde vêm os predadores que estão te caçando. */
	private static void drawDanger(GuiGraphicsExtractor graphics, Minecraft mc, Player player) {
		if (danger.isEmpty() || System.currentTimeMillis() - dangerTime > 1500) {
			return;
		}
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();
		float radius = Math.min(w, h) * 0.36F;
		for (net.minecraft.core.BlockPos pos : danger) {
			double dx = pos.getX() + 0.5 - player.getX();
			double dz = pos.getZ() + 0.5 - player.getZ();
			double bearing = Math.toDegrees(Math.atan2(-dx, dz)) - player.getYRot();
			double rad = Math.toRadians(bearing);
			int px = (int) (w / 2.0 + Math.sin(-rad) * radius);
			int py = (int) (h / 2.0 - Math.cos(rad) * radius);
			graphics.fill(px - 8, py - 8, px + 8, py + 8, 0xC0600000);
			graphics.outline(px - 8, py - 8, 16, 16, 0xFFFF4040);
			graphics.centeredText(mc.font, "!", px, py - 4, 0xFFFFFFFF);
		}
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.gui.hud.isHidden() || !Creatures.isCreature(player)) {
			return;
		}
		drawDanger(graphics, mc, player);
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
		com.primordial.intellect.IntellectState brain = com.primordial.intellect.IntellectService.state(player);
		for (int i = 0; i < brain.slotCount(); i++) {
			Identifier slotAbility = brain.slot(i);
			if (slotAbility != null) {
				y = line(graphics, mc, Component.literal("- ").append(Component.translatable("ability." + slotAbility.getNamespace() + "." + slotAbility.getPath())), y, 0xFFE1A8FF);
			}
		}
		y = line(graphics, mc, Component.translatable("hud.primordial.inventory", com.primordial.capability.InventoryRules.slots(player)), y, 0xFFE3D5A1);
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
