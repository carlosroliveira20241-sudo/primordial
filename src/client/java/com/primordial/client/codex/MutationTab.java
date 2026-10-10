package com.primordial.client.codex;

import com.primordial.creature.CreatureBody;
import com.primordial.creature.Creatures;
import com.primordial.mutation.Mutation;
import com.primordial.mutation.MutationRegistry;
import com.primordial.mutation.MutationService;
import com.primordial.mutation.MutationState;
import com.primordial.synergy.Synergy;
import com.primordial.synergy.SynergyRegistry;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Aba Mutações: as que você já tem e as sinergias de família (ativas ou quanto falta). */
final class MutationTab implements CodexTab {
	private int scroll;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.mutations");
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		MutationState state = MutationService.state(s.player());
		CreatureBody body = Creatures.body(s.player());

		g.enableScissor(x0, y0, x1, y1);
		int y = y0 + 8 - this.scroll;
		g.text(s.font(), Component.translatable("screen.primordial.mutations.title", state.chosen().size()), x0 + 8, y, UiKit.GOLD);
		y += 14;
		if (state.chosen().isEmpty()) {
			g.text(s.font(), Component.translatable("screen.primordial.mutations.none"), x0 + 8, y, UiKit.DIM);
			y += 16;
		}
		int colW = (x1 - x0 - 24) / 2;
		int col = 0;
		int rowTop = y;
		for (Identifier id : state.chosen()) {
			Mutation mutation = MutationRegistry.get(id);
			if (mutation == null) {
				continue;
			}
			int x = x0 + 8 + col * (colW + 8);
			UiKit.box(g, x, rowTop, x + colW, rowTop + 46, UiKit.PANEL, 0xFF000000 | mutation.rarity().color);
			g.text(s.font(), Mutation.name(id).copy().withStyle(net.minecraft.ChatFormatting.BOLD), x + 6, rowTop + 6, 0xFF000000 | mutation.rarity().color);
			int ty = rowTop + 18;
			for (var seq : s.font().split(Mutation.description(id), colW - 12)) {
				g.text(s.font(), seq, x + 6, ty, 0xFFC8D4E0);
				ty += 10;
			}
			col++;
			if (col == 2) {
				col = 0;
				rowTop += 52;
			}
		}
		y = rowTop + (col == 0 ? 0 : 52) + 10;

		g.text(s.font(), Component.translatable("screen.primordial.synergies.title"), x0 + 8, y, UiKit.GOLD);
		y += 14;
		Map<String, Integer> counts = SynergyRegistry.familyCounts(body);
		for (Map.Entry<Identifier, Synergy> entry : SynergyRegistry.all().entrySet()) {
			Synergy synergy = entry.getValue();
			int have = counts.getOrDefault(synergy.family(), 0);
			boolean active = have >= synergy.count();
			UiKit.box(g, x0 + 8, y, x1 - 8, y + 30, active ? 0xFF16301F : UiKit.PANEL, active ? UiKit.GOOD : UiKit.BORDER);
			g.text(s.font(), Synergy.name(entry.getKey()).copy().withStyle(net.minecraft.ChatFormatting.BOLD), x0 + 14, y + 5, active ? UiKit.GOOD : UiKit.TEXT);
			g.text(s.font(), Component.translatable("screen.primordial.synergies.progress", Component.translatable("family.primordial." + synergy.family()), have, synergy.count()),
				x0 + 14, y + 17, UiKit.DIM);
			var seq = s.font().split(Synergy.description(entry.getKey()), x1 - x0 - 220);
			if (!seq.isEmpty()) {
				g.text(s.font(), seq.get(0), x0 + 210, y + 11, 0xFFC8D4E0);
			}
			y += 34;
		}
		g.disableScissor();
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.scroll = (int) Math.max(0, Math.min(600, this.scroll - amount * 24));
		return true;
	}
}
