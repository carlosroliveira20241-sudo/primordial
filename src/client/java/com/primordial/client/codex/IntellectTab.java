package com.primordial.client.codex;

import com.primordial.capability.InventoryRules;
import com.primordial.creature.Creatures;
import com.primordial.intellect.IntellectDef;
import com.primordial.intellect.IntellectRegistry;
import com.primordial.intellect.IntellectService;
import com.primordial.intellect.IntellectState;
import com.primordial.net.Payloads;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Aba Inteligência. O cérebro (DNA) libera slots; a força (DNA) libera espaços de inventário. As habilidades de
 * inteligência são compradas com DNA e, equipadas num slot, libertam ações do Minecraft: quebrar, colocar blocos,
 * usar e criar ferramentas (mouse esquerdo/direito) e atirar (tecla R).
 */
final class IntellectTab implements CodexTab {
	private static final int CARD_H = 52;

	private enum Kind {
		BRAIN, STRENGTH, SLOT, BUY, EQUIP, REMOVE
	}

	private record Hit(int x, int y, int w, int h, Kind kind, int slot, Identifier ability) {
	}

	private final List<Hit> hits = new ArrayList<>();
	private int selectedSlot;
	private int scroll;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.intellect");
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		this.hits.clear();
		Player player = s.player();
		IntellectState state = IntellectService.state(player);
		int dna = Creatures.progress(player).dna();
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		int mid = (x0 + x1) / 2;

		// ---- cartão do cérebro (esquerda)
		int brainCap = IntellectService.brainCap(player);
		UiKit.box(g, x0 + 6, y0 + 6, mid - 3, y0 + 76, UiKit.PANEL, 0xFFE1A8FF);
		int bs = 12 + state.brain() * 2;
		UiKit.box(g, x0 + 14, y0 + 30 - bs / 2, x0 + 14 + bs, y0 + 30 + bs / 2, 0xFF5B2A66, 0xFFE1A8FF);
		g.text(s.font(), Component.translatable("screen.primordial.intellect.brain", state.brain(), IntellectState.MAX_BRAIN).copy().withStyle(ChatFormatting.BOLD), x0 + 50, y0 + 11, 0xFFE1A8FF);
		UiKit.bar(g, x0 + 50, y0 + 24, mid - x0 - 66, 7, (double) state.brain() / IntellectState.MAX_BRAIN, 0xFFB06AD0);
		g.text(s.font(), Component.translatable("screen.primordial.intellect.cap", brainCap), x0 + 50, y0 + 35, UiKit.DIM);
		if (state.brain() < brainCap) {
			int cost = IntellectService.upgradeCost(state.brain());
			button(g, s, mx, my, x0 + 12, y0 + 50, mid - x0 - 24, 20, Component.translatable("screen.primordial.intellect.upgrade", cost), dna >= cost, Kind.BRAIN, 0, null);
		} else {
			g.text(s.font(), Component.translatable("screen.primordial.intellect.maxed"), x0 + 12, y0 + 56, UiKit.DIM);
		}

		// ---- cartão da força (direita)
		int strengthCap = IntellectService.strengthCap(player);
		UiKit.box(g, mid + 3, y0 + 6, x1 - 6, y0 + 76, UiKit.PANEL, 0xFFFFB74D);
		g.text(s.font(), Component.translatable("screen.primordial.strength.title", state.strength(), IntellectState.MAX_STRENGTH).copy().withStyle(ChatFormatting.BOLD), mid + 12, y0 + 11, 0xFFFFB74D);
		UiKit.bar(g, mid + 12, y0 + 24, x1 - mid - 28, 7, (double) state.strength() / IntellectState.MAX_STRENGTH, 0xFFFF9800);
		g.text(s.font(), Component.translatable("screen.primordial.strength.inventory", InventoryRules.slots(player)), mid + 12, y0 + 35, UiKit.TEXT);
		if (state.strength() < strengthCap) {
			int cost = IntellectService.strengthCost(state.strength());
			button(g, s, mx, my, mid + 12, y0 + 50, x1 - mid - 24, 20, Component.translatable("screen.primordial.strength.upgrade", cost), dna >= cost, Kind.STRENGTH, 0, null);
		} else {
			g.text(s.font(), Component.translatable("screen.primordial.strength.cap", strengthCap), mid + 12, y0 + 56, UiKit.DIM);
		}

		// ---- slots
		g.text(s.font(), Component.translatable("screen.primordial.intellect.slots"), x0 + 8, y0 + 82, UiKit.DIM);
		int slotW = (x1 - x0 - 30) / 4;
		for (int i = 0; i < IntellectState.MAX_SLOTS; i++) {
			int sx = x0 + 8 + i * (slotW + 4);
			int sy = y0 + 94;
			boolean unlocked = i < state.slotCount();
			boolean selected = unlocked && i == this.selectedSlot;
			UiKit.box(g, sx, sy, sx + slotW, sy + 34, unlocked ? UiKit.PANEL_LIGHT : 0xFF10151C, selected ? UiKit.GOLD : unlocked ? UiKit.BORDER : UiKit.LOCKED);
			g.text(s.font(), Component.translatable("screen.primordial.intellect.slot", i + 1), sx + 5, sy + 4, unlocked ? UiKit.ACCENT : UiKit.LOCKED);
			if (!unlocked) {
				int needed = i == 1 ? 3 : i == 2 ? 5 : 7;
				g.text(s.font(), Component.translatable("screen.primordial.intellect.slot_locked", needed), sx + 5, sy + 18, UiKit.LOCKED);
			} else {
				Identifier id = state.slot(i);
				if (id == null) {
					g.text(s.font(), Component.translatable("screen.primordial.intellect.empty"), sx + 5, sy + 18, UiKit.DIM);
				} else {
					g.text(s.font(), Component.translatable("ability." + id.getNamespace() + "." + id.getPath()), sx + 5, sy + 18, 0xFFE1A8FF);
				}
				this.hits.add(new Hit(sx, sy, slotW, 34, Kind.SLOT, i, id));
			}
		}

		// ---- habilidades
		g.enableScissor(x0, y0 + 134, x1, y1);
		int y = y0 + 138 - this.scroll;
		for (Map.Entry<Identifier, IntellectDef> entry : IntellectRegistry.all().entrySet()) {
			Identifier id = entry.getKey();
			IntellectDef def = entry.getValue();
			boolean owned = state.owned().contains(id);
			boolean brainOk = state.brain() >= def.brain();
			UiKit.box(g, x0 + 6, y, x1 - 6, y + CARD_H - 4, owned ? 0xFF14261A : brainOk ? UiKit.PANEL : 0xFF10151C, owned ? UiKit.GOOD : brainOk ? UiKit.BORDER : UiKit.LOCKED);
			Component name = Component.translatable("ability." + id.getNamespace() + "." + id.getPath());
			g.text(s.font(), name.copy().withStyle(ChatFormatting.BOLD), x0 + 12, y + 5, owned ? UiKit.GOOD : brainOk ? UiKit.TEXT : UiKit.DIM);
			Component how = Component.translatable(def.usesR() ? "screen.primordial.intellect.how_r" : "screen.primordial.intellect.how_mouse");
			g.text(s.font(), how, x0 + 16 + s.font().width(name), y + 5, UiKit.ACCENT);
			int ty = y + 17;
			for (var seq : s.font().split(Component.translatable("ability." + id.getNamespace() + "." + id.getPath() + ".desc"), x1 - x0 - 230)) {
				g.text(s.font(), seq, x0 + 12, ty, 0xFFC8D4E0);
				ty += 10;
			}
			int bx = x1 - 200;
			int by = y + 10;
			if (!owned) {
				if (brainOk) {
					button(g, s, mx, my, bx, by, 188, 20, Component.translatable("screen.primordial.intellect.buy", def.cost()), dna >= def.cost(), Kind.BUY, 0, id);
				} else {
					g.text(s.font(), Component.translatable("screen.primordial.intellect.need_brain", def.brain()), bx + 6, by + 6, UiKit.BAD);
				}
			} else if (state.isEquipped(id)) {
				button(g, s, mx, my, bx, by, 188, 20, Component.translatable("screen.primordial.intellect.remove"), true, Kind.REMOVE, 0, id);
			} else {
				button(g, s, mx, my, bx, by, 188, 20, Component.translatable("screen.primordial.intellect.equip", this.selectedSlot + 1), true, Kind.EQUIP, this.selectedSlot, id);
			}
			y += CARD_H;
		}
		g.disableScissor();
	}

	private void button(GuiGraphicsExtractor g, CodexScreen s, int mx, int my, int x, int y, int w, int h, Component label, boolean enabled, Kind kind, int slot, Identifier ability) {
		boolean hover = enabled && UiKit.inside(mx, my, x, y, w, h);
		UiKit.box(g, x, y, x + w, y + h, hover ? 0xFF2B3A52 : UiKit.PANEL_LIGHT, enabled ? UiKit.ACCENT : UiKit.LOCKED);
		g.centeredText(s.font(), label, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFFFFFFF : UiKit.LOCKED);
		if (enabled) {
			this.hits.add(new Hit(x, y, w, h, kind, slot, ability));
		}
	}

	@Override
	public boolean click(CodexScreen s, double mx, double my, int button) {
		for (Hit hit : this.hits) {
			if (!UiKit.inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) {
				continue;
			}
			switch (hit.kind()) {
				case BRAIN -> ClientPlayNetworking.send(new Payloads.UpgradeBrain());
				case STRENGTH -> ClientPlayNetworking.send(new Payloads.UpgradeStrength());
				case BUY -> ClientPlayNetworking.send(new Payloads.BuyIntellect(hit.ability()));
				case EQUIP -> ClientPlayNetworking.send(new Payloads.SetIntellectSlot(hit.slot(), Optional.of(hit.ability())));
				case REMOVE -> {
					IntellectState state = IntellectService.state(s.player());
					for (int i = 0; i < IntellectState.MAX_SLOTS; i++) {
						if (hit.ability().equals(state.slot(i))) {
							ClientPlayNetworking.send(new Payloads.SetIntellectSlot(i, Optional.empty()));
						}
					}
				}
				case SLOT -> {
					if (button == 1 && hit.ability() != null) {
						ClientPlayNetworking.send(new Payloads.SetIntellectSlot(hit.slot(), Optional.empty()));
					} else {
						this.selectedSlot = hit.slot();
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.scroll = (int) Math.max(0, Math.min(500, this.scroll - amount * 24));
		return true;
	}
}
