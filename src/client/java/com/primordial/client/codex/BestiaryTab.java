package com.primordial.client.codex;

import com.primordial.creature.CreatureProgress;
import com.primordial.creature.Creatures;
import com.primordial.entity.CreatureDef;
import com.primordial.entity.CreatureRegistry;
import com.primordial.item.PrimordialItems;
import com.primordial.journal.JournalService;
import com.primordial.journal.JournalState;
import com.primordial.look.Look;
import com.primordial.look.LookRegistry;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Aba Bestiário: todas as criaturas do mundo. As que você nunca viu aparecem como "?". */
final class BestiaryTab implements CodexTab {
	private static final int CARD = 44;
	private static final int GAP = 6;

	private record Card(int x, int y, Identifier id) {
	}

	private final List<Card> cards = new ArrayList<>();
	private Identifier selected;
	private int scroll;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.bestiary");
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		this.cards.clear();
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		JournalState journal = JournalService.state(s.player());
		CreatureProgress progress = Creatures.progress(s.player());
		int detailW = 210;
		int gridRight = x1 - detailW - 8;
		int perRow = Math.max(1, (gridRight - x0 - 8) / (CARD + GAP));

		int seen = 0;
		for (Identifier id : CreatureRegistry.defs().keySet()) {
			if (journal.hasSeen(id)) {
				seen++;
			}
		}
		g.text(s.font(), Component.translatable("screen.primordial.bestiary.count", seen, CreatureRegistry.defs().size()), x0 + 8, y0 + 4, UiKit.GOLD);

		g.enableScissor(x0, y0 + 16, gridRight, y1);
		int index = 0;
		for (Map.Entry<Identifier, CreatureDef> entry : CreatureRegistry.defs().entrySet()) {
			Identifier id = entry.getKey();
			int x = x0 + 8 + (index % perRow) * (CARD + GAP);
			int y = y0 + 22 + (index / perRow) * (CARD + GAP) - this.scroll;
			index++;
			boolean known = journal.hasSeen(id);
			Look look = LookRegistry.get(entry.getValue().look());
			int tint = look.tint();
			int bg = known ? ARGB.color(255, ARGB.red(tint) / 2, ARGB.green(tint) / 2, ARGB.blue(tint) / 2) : 0xFF141A24;
			int frame = id.equals(this.selected) ? UiKit.GOLD : known ? (journal.kills(id) > 0 ? UiKit.GOOD : UiKit.ACCENT) : UiKit.LOCKED;
			UiKit.box(g, x, y, x + CARD, y + CARD, bg, frame);
			if (known) {
				// carinha simples: olhos
				g.fill(x + 12, y + 14, x + 17, y + 19, 0xFF101010);
				g.fill(x + CARD - 17, y + 14, x + CARD - 12, y + 19, 0xFF101010);
				g.fill(x + 14, y + 28, x + CARD - 14, y + 31, 0x80000000);
				if (journal.kills(id) > 0) {
					g.text(s.font(), String.valueOf(journal.kills(id)), x + 3, y + CARD - 11, UiKit.TEXT);
				}
			} else {
				g.centeredText(s.font(), "?", x + CARD / 2, y + CARD / 2 - 4, UiKit.DIM);
			}
			this.cards.add(new Card(x, y, id));
			if (known && UiKit.inside(mx, my, x, y, CARD, CARD) && UiKit.inside(mx, my, x0, y0 + 16, gridRight - x0, y1 - y0 - 16)) {
				List<UiKit.Line> tip = UiKit.newLines();
				UiKit.line(tip, Component.translatable("entity." + id.getNamespace() + "." + id.getPath()), UiKit.TEXT);
				s.tooltip(tip);
			}
		}
		g.disableScissor();

		// painel de detalhes
		int dx = x1 - detailW;
		UiKit.box(g, dx, y0 + 4, x1 - 2, y1 - 2, UiKit.PANEL, UiKit.BORDER);
		if (this.selected == null) {
			g.centeredText(s.font(), Component.translatable("screen.primordial.bestiary.pick"), dx + detailW / 2, (y0 + y1) / 2, UiKit.DIM);
			return;
		}
		CreatureDef def = CreatureRegistry.get(this.selected);
		boolean known = journal.hasSeen(this.selected);
		int ty = y0 + 12;
		if (!known || def == null) {
			g.text(s.font(), "???", dx + 8, ty, UiKit.DIM);
			g.text(s.font(), Component.translatable("screen.primordial.bestiary.unknown"), dx + 8, ty + 14, UiKit.DIM);
			return;
		}
		g.text(s.font(), Component.translatable("entity." + this.selected.getNamespace() + "." + this.selected.getPath()).copy().withStyle(net.minecraft.ChatFormatting.BOLD),
			dx + 8, ty, UiKit.TEXT);
		ty += 14;
		g.text(s.font(), Component.translatable("screen.primordial.bestiary.kills", journal.kills(this.selected)), dx + 8, ty, UiKit.GOOD);
		ty += 12;
		g.text(s.font(), Component.translatable("screen.primordial.bestiary.stats", (int) def.health(), String.format("%.0f", def.damage())), dx + 8, ty, UiKit.TEXT);
		ty += 12;
		g.text(s.font(), Component.translatable("screen.primordial.bestiary.habitat", Component.translatable("biome.primordial." + def.spawn().biomes())), dx + 8, ty, UiKit.DIM);
		ty += 12;
		if (def.spawn().nightOnly()) {
			g.text(s.font(), Component.translatable("screen.primordial.bestiary.night"), dx + 8, ty, 0xFFB39DDB);
			ty += 12;
		}
		for (String trait : def.traits()) {
			for (var seq : s.font().split(Component.translatable("trait.primordial." + trait), detailW - 16)) {
				g.text(s.font(), seq, dx + 8, ty, 0xFF9AD0FF);
				ty += 10;
			}
		}
		ty += 6;
		List<Identifier> drops = PartRegistry.droppedBy(this.selected);
		if (def.has("treasure")) {
			g.text(s.font(), Component.translatable("screen.primordial.bestiary.treasure"), dx + 8, ty, UiKit.GOLD);
			ty += 12;
		} else if (def.has("undead")) {
			g.text(s.font(), Component.translatable("screen.primordial.bestiary.no_drops"), dx + 8, ty, UiKit.BAD);
			ty += 12;
		} else if (!drops.isEmpty()) {
			g.text(s.font(), Component.translatable("screen.primordial.bestiary.drops"), dx + 8, ty, UiKit.GOLD);
			ty += 12;
			for (Identifier part : drops) {
				boolean have = progress.unlocked(part);
				g.item(PrimordialItems.morsel(part), dx + 8, ty - 3);
				g.text(s.font(), PartDefinition.name(part), dx + 28, ty + 1, have ? UiKit.GOOD : UiKit.TEXT);
				ty += 18;
			}
		}
	}

	@Override
	public boolean click(CodexScreen s, double mx, double my, int button) {
		for (Card card : this.cards) {
			if (UiKit.inside(mx, my, card.x(), card.y(), CARD, CARD) && my >= s.cy0() + 16) {
				this.selected = card.id();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.scroll = (int) Math.max(0, Math.min(800, this.scroll - amount * 24));
		return true;
	}
}
