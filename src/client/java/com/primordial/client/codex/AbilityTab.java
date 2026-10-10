package com.primordial.client.codex;

import com.primordial.ability.Abilities;
import com.primordial.ability.AbilityManager;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.item.PrimordialItems;
import com.primordial.net.Payloads;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Aba Habilidades: as habilidades das partes equipadas, na ordem das teclas. As setas trocam a ordem. */
final class AbilityTab implements CodexTab {
	private static final String[] KEYS = {"Z", "X"};
	private static final int ROW_H = 64;

	private record Arrow(int x, int y, int index, int direction) {
	}

	private final List<Arrow> arrows = new ArrayList<>();
	private int scroll;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.abilities");
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		this.arrows.clear();
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		List<Identifier> abilities = AbilityManager.abilitiesOf(s.player());
		CreatureBody body = Creatures.body(s.player());

		if (abilities.isEmpty()) {
			g.centeredText(s.font(), Component.translatable("screen.primordial.abilities.none"), (x0 + x1) / 2, (y0 + y1) / 2, UiKit.DIM);
			return;
		}
		g.text(s.font(), Component.translatable("screen.primordial.abilities.help"), x0 + 8, y0 + 6, UiKit.DIM);

		g.enableScissor(x0, y0 + 20, x1, y1);
		for (int i = 0; i < abilities.size(); i++) {
			Identifier id = abilities.get(i);
			int y = y0 + 24 + i * ROW_H - this.scroll;
			boolean bound = i < KEYS.length;
			UiKit.box(g, x0 + 6, y, x1 - 6, y + ROW_H - 6, UiKit.PANEL, bound ? UiKit.ACCENT : UiKit.BORDER);

			// tecla
			UiKit.box(g, x0 + 14, y + 10, x0 + 46, y + 42, bound ? 0xFF16324A : 0xFF1B2433, bound ? UiKit.ACCENT : UiKit.LOCKED);
			g.centeredText(s.font(), bound ? KEYS[i] : "-", x0 + 30, y + 22, bound ? 0xFFFFFFFF : UiKit.DIM);

			// ícone da parte que dá a habilidade
			PartDefinition source = null;
			Identifier sourceId = null;
			for (PartRef ref : body.parts().values()) {
				PartDefinition def = PartRegistry.get(ref.id());
				if (def != null && def.ability().filter(id::equals).isPresent()) {
					source = def;
					sourceId = ref.id();
				}
			}
			if (sourceId != null) {
				g.item(PrimordialItems.morsel(sourceId), x0 + 54, y + 10);
				g.text(s.font(), PartDefinition.name(sourceId), x0 + 54, y + 30, UiKit.DIM);
			}

			int textX = x0 + 160;
			int color = source != null ? 0xFF000000 | source.rarity().color : UiKit.TEXT;
			g.text(s.font(), Component.translatable("ability." + id.getNamespace() + "." + id.getPath()).copy().withStyle(net.minecraft.ChatFormatting.BOLD), textX, y + 8, color);
			String name = id.getPath();
			String seconds = Abilities.exists(name) ? String.format(Locale.ROOT, "%.0f", Abilities.get(name).cooldownTicks() / 20.0) : "?";
			g.text(s.font(), Component.translatable("screen.primordial.abilities.cooldown", seconds), textX + 150, y + 8, UiKit.ACCENT);
			int ty = y + 20;
			for (var seq : s.font().split(Component.translatable("ability." + id.getNamespace() + "." + id.getPath() + ".desc"), x1 - textX - 60)) {
				g.text(s.font(), seq, textX, ty, 0xFFC8D4E0);
				ty += 10;
			}

			// setas para trocar a ordem
			int ax = x1 - 38;
			drawArrow(g, s, mx, my, ax, y + 8, "▲", i, -1, i > 0);
			drawArrow(g, s, mx, my, ax, y + 28, "▼", i, 1, i < abilities.size() - 1);
		}
		g.disableScissor();
	}

	private void drawArrow(GuiGraphicsExtractor g, CodexScreen s, int mx, int my, int x, int y, String glyph, int index, int dir, boolean enabled) {
		boolean hover = enabled && UiKit.inside(mx, my, x, y, 22, 16);
		UiKit.box(g, x, y, x + 22, y + 16, hover ? 0xFF2B3A52 : UiKit.PANEL_LIGHT, enabled ? UiKit.ACCENT : UiKit.LOCKED);
		g.centeredText(s.font(), glyph, x + 11, y + 4, enabled ? 0xFFFFFFFF : UiKit.LOCKED);
		if (enabled) {
			this.arrows.add(new Arrow(x, y, index, dir));
		}
	}

	@Override
	public boolean click(CodexScreen s, double mx, double my, int button) {
		for (Arrow arrow : this.arrows) {
			if (UiKit.inside(mx, my, arrow.x(), arrow.y(), 22, 16)) {
				List<Identifier> order = new ArrayList<>(AbilityManager.abilitiesOf(s.player()));
				int to = arrow.index() + arrow.direction();
				if (to >= 0 && to < order.size()) {
					Identifier moved = order.remove(arrow.index());
					order.add(to, moved);
					ClientPlayNetworking.send(new Payloads.SetAbilityOrder(order));
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.scroll = (int) Math.max(0, Math.min(400, this.scroll - amount * 20));
		return true;
	}
}
