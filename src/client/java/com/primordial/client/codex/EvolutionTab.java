package com.primordial.client.codex;

import com.primordial.creature.BodySlot;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureProgress;
import com.primordial.creature.Creatures;
import com.primordial.creature.Stage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Aba Evolução: a linha dos 6 estágios, onde você está e o que cada um exige e libera. */
final class EvolutionTab implements CodexTab {
	private int scroll;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.evolution");
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		CreatureBody body = Creatures.body(s.player());
		CreatureProgress progress = Creatures.progress(s.player());
		Stage current = body.stage();

		g.enableScissor(x0, y0, x1, y1);
		int y = y0 + 8 - this.scroll;
		Stage[] stages = Stage.values();
		for (int i = 0; i < stages.length; i++) {
			Stage stage = stages[i];
			boolean reached = stage.ordinal() <= current.ordinal();
			boolean here = stage == current;
			int frame = here ? UiKit.GOLD : reached ? UiKit.GOOD : stage.available ? UiKit.BORDER : UiKit.LOCKED;
			// linha que liga os estágios, como numa árvore de conquistas
			if (i < stages.length - 1) {
				g.fill(x0 + 29, y + 46, x0 + 31, y + 78, reached && stage.ordinal() < current.ordinal() ? UiKit.GOOD : UiKit.BORDER);
			}
			UiKit.box(g, x0 + 12, y, x0 + 48, y + 46, here ? 0xFF2A2310 : UiKit.PANEL, frame);
			g.centeredText(s.font(), String.valueOf(stage.number()), x0 + 30, y + 19, here ? UiKit.GOLD : reached ? UiKit.GOOD : UiKit.DIM);

			UiKit.box(g, x0 + 58, y, x1 - 8, y + 46, UiKit.PANEL, frame);
			Component name = Component.translatable(stage.translationKey()).copy().withStyle(ChatFormatting.BOLD);
			g.text(s.font(), name, x0 + 66, y + 5, here ? UiKit.GOLD : UiKit.TEXT);
			if (here) {
				g.text(s.font(), Component.translatable("screen.primordial.evolution.here"), x0 + 72 + s.font().width(name), y + 5, UiKit.GOOD);
			} else if (!stage.available) {
				g.text(s.font(), Component.translatable("screen.primordial.evolution.soon"), x0 + 72 + s.font().width(name), y + 5, UiKit.DIM);
			}
			g.text(s.font(), Component.translatable("screen.primordial.evolution.stats", String.format("%.0f", stage.maxHealth),
				String.format("%.1f", stage.attackDamage), stage.abilitySlots), x0 + 66, y + 17, UiKit.DIM);
			StringBuilder slots = new StringBuilder();
			for (BodySlot slot : stage.slots) {
				if (slots.length() > 0) {
					slots.append(", ");
				}
				slots.append(Component.translatable(slot.translationKey()).getString());
			}
			var lines = s.font().split(Component.translatable("screen.primordial.evolution.slots", slots.toString()), x1 - x0 - 90);
			int ty = y + 28;
			for (var line : lines.subList(0, Math.min(2, lines.size()))) {
				g.text(s.font(), line, x0 + 66, ty, 0xFFC8D4E0);
				ty += 9;
			}
			if (stage.ordinal() + 1 < stages.length && here) {
				g.text(s.font(), Component.translatable("screen.primordial.evolution.next", stage.levelToEvolve, progress.level()), x1 - 8 - 150, y + 5, UiKit.ACCENT);
			}
			y += 66;
		}
		g.disableScissor();
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.scroll = (int) Math.max(0, Math.min(200, this.scroll - amount * 24));
		return true;
	}
}
