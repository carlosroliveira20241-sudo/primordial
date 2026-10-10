package com.primordial.client.codex;

import com.primordial.ability.Abilities;
import com.primordial.creature.BodySlot;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureProgress;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.item.PrimordialItems;
import com.primordial.net.Payloads;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartModifier;
import com.primordial.part.PartRegistry;
import com.primordial.synergy.Synergy;
import com.primordial.synergy.SynergyRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/** Aba Corpo: uma coluna por encaixe do corpo, com as partes que cabem nele. Clique para equipar, botão direito para tirar. */
final class BodyTab implements CodexTab {
	private static final String[] ROMAN = {"", "I", "II", "III"};
	private static final int COL_W = 82;
	private static final int NODE = 28;
	private static final int ROW_H = 40;

	private record Hit(int x, int y, BodySlot slot, Identifier part) {
	}

	private final List<Hit> hits = new ArrayList<>();
	private int offX;
	private int offY;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.body");
	}

	private static List<Identifier> candidates(BodySlot slot, CreatureProgress progress) {
		List<Identifier> parts = new ArrayList<>();
		PartRegistry.all().forEach((id, def) -> {
			if (def.slot() == slot.type()) {
				parts.add(id);
			}
		});
		parts.sort(Comparator.comparing((Identifier id) -> !PartRegistry.get(id).base())
			.thenComparing(id -> !progress.unlocked(id))
			.thenComparing(id -> PartRegistry.get(id).minStage())
			.thenComparing(Identifier::getPath));
		return parts;
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		CreatureBody body = Creatures.body(s.player());
		CreatureProgress progress = Creatures.progress(s.player());
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		this.hits.clear();

		// grade de fundo, como o fundo da tela de conquistas
		for (int gx = x0 + Math.floorMod(this.offX, 32); gx < x1; gx += 32) {
			g.fill(gx, y0, gx + 1, y1, 0xFF0E141C);
		}
		for (int gy = y0 + Math.floorMod(this.offY, 32); gy < y1; gy += 32) {
			g.fill(x0, gy, x1, gy + 1, 0xFF0E141C);
		}

		g.enableScissor(x0, y0, x1, y1);
		List<BodySlot> slots = body.stage().slots;
		int maxRows = 0;
		for (int i = 0; i < slots.size(); i++) {
			BodySlot slot = slots.get(i);
			int colX = x0 + 14 + this.offX + i * COL_W;
			int headerY = y0 + 22 + this.offY;
			List<Identifier> parts = candidates(slot, progress);
			maxRows = Math.max(maxRows, parts.size());

			PartRef equipped = body.part(slot);
			// tronco da árvore
			if (!parts.isEmpty()) {
				g.fill(colX + NODE / 2 - 1, headerY + NODE, colX + NODE / 2 + 1, headerY + 52 + (parts.size() - 1) * ROW_H + NODE / 2, UiKit.BORDER);
			}
			g.centeredText(s.font(), Component.translatable(slot.translationKey()), colX + NODE / 2, headerY - 13, UiKit.DIM);
			ItemStack headerIcon = equipped != null ? PrimordialItems.morsel(equipped.id()) : ItemStack.EMPTY;
			UiKit.node(g, s.font(), colX, headerY, NODE, UiKit.GOLD, 0xFF2A2310, headerIcon, false, equipped == null ? "-" : null);
			this.hits.add(new Hit(colX, headerY, slot, null));

			for (int j = 0; j < parts.size(); j++) {
				Identifier id = parts.get(j);
				PartDefinition def = PartRegistry.get(id);
				int ny = headerY + 52 + j * ROW_H;
				boolean isEquipped = equipped != null && equipped.id().equals(id);
				boolean unlocked = progress.unlocked(id);
				boolean stageOk = body.stage().number() >= def.minStage();
				int frame = isEquipped ? UiKit.GOLD : unlocked ? (0xFF000000 | def.rarity().color) : UiKit.LOCKED;
				// pequeno traço ligando o tronco ao nó
				g.fill(colX + NODE / 2, ny + NODE / 2 - 1, colX + NODE, ny + NODE / 2 + 1, UiKit.BORDER);
				UiKit.node(g, s.font(), colX, ny, NODE, frame, background(def, unlocked), PrimordialItems.morsel(id),
					!unlocked || !stageOk, unlocked ? null : "?");
				if (isEquipped) {
					g.outline(colX - 1, ny - 1, NODE + 2, NODE + 2, 0x80FFC107);
				}
				if (unlocked && progress.partLevel(id) > 0) {
					g.text(s.font(), ROMAN[Math.min(3, Math.max(1, progress.partLevel(id)))], colX + NODE + 3, ny + 2, UiKit.TEXT);
				}
				this.hits.add(new Hit(colX, ny, slot, id));
			}
		}
		g.disableScissor();

		// dica sob o mouse
		if (UiKit.inside(mx, my, x0, y0, x1 - x0, y1 - y0)) {
			for (Hit hit : this.hits) {
				if (UiKit.inside(mx, my, hit.x(), hit.y(), NODE, NODE)) {
					s.tooltip(hit.part() == null ? slotTooltip(s, hit.slot(), body) : partTooltip(s, hit.part(), hit.slot(), body, progress));
					break;
				}
			}
		}

		// sinergias ativas, no canto de baixo
		List<Identifier> active = SynergyRegistry.active(body);
		if (!active.isEmpty()) {
			int sx = x0 + 6;
			int sy = y1 - 14;
			g.fill(x0, y1 - 18, x1, y1, 0xC0101820);
			g.text(s.font(), Component.translatable("screen.primordial.synergies"), sx, sy + 2, UiKit.GOLD);
			sx += s.font().width(Component.translatable("screen.primordial.synergies")) + 8;
			for (Identifier id : active) {
				Component name = Synergy.name(id);
				g.text(s.font(), name, sx, sy + 2, UiKit.GOOD);
				sx += s.font().width(name) + 10;
			}
		}
	}

	private static int background(PartDefinition def, boolean unlocked) {
		Optional<Integer> tint = def.visual().flatMap(v -> v.tint());
		if (!unlocked || tint.isEmpty()) {
			return 0xFF1B2433;
		}
		int c = tint.get();
		return ARGB.color(255, ARGB.red(c) / 3, ARGB.green(c) / 3, ARGB.blue(c) / 3);
	}

	private static List<UiKit.Line> slotTooltip(CodexScreen s, BodySlot slot, CreatureBody body) {
		List<UiKit.Line> lines = UiKit.newLines();
		UiKit.line(lines, Component.translatable(slot.translationKey()), UiKit.GOLD);
		PartRef equipped = body.part(slot);
		if (equipped == null) {
			UiKit.line(lines, Component.translatable("screen.primordial.editor.empty"), UiKit.DIM);
		} else {
			UiKit.line(lines, PartDefinition.name(equipped.id()), UiKit.TEXT);
			UiKit.line(lines, Component.translatable("screen.primordial.body.remove_hint"), UiKit.DIM);
		}
		return lines;
	}

	static List<UiKit.Line> partTooltip(CodexScreen s, Identifier id, BodySlot slot, CreatureBody body, CreatureProgress progress) {
		PartDefinition def = PartRegistry.get(id);
		List<UiKit.Line> lines = UiKit.newLines();
		boolean unlocked = progress.unlocked(id);
		int level = Math.max(1, progress.partLevel(id));
		Component title = PartDefinition.name(id).copy();
		if (unlocked) {
			title = title.copy().append(" " + ROMAN[Math.min(3, level)]);
		}
		UiKit.line(lines, title, 0xFF000000 | def.rarity().color);
		UiKit.line(lines, Component.translatable(def.rarity().translationKey()).append(" - ")
			.append(Component.translatable("slottype.primordial." + def.slot().getSerializedName())), UiKit.DIM);
		UiKit.wrap(s.font(), lines, PartDefinition.description(id), 220, UiKit.TEXT);

		if (def.ability().isPresent()) {
			String name = def.ability().get().getPath();
			String seconds = Abilities.exists(name) ? String.format(Locale.ROOT, "%.0f", Abilities.get(name).cooldownTicks() / 20.0) : "?";
			UiKit.line(lines, Component.literal(" "), UiKit.TEXT);
			UiKit.line(lines, Component.translatable("screen.primordial.editor.gives",
				Component.translatable("ability." + def.ability().get().getNamespace() + "." + name), seconds), UiKit.GOOD);
			UiKit.wrap(s.font(), lines, Component.translatable("ability." + def.ability().get().getNamespace() + "." + name + ".desc"), 220, 0xFFC8D4E0);
		}
		for (String passive : def.passives()) {
			Component text = Component.translatable("passive.primordial." + passive);
			UiKit.wrap(s.font(), lines, Component.translatable("screen.primordial.passive", text), 220, 0xFF9AD0FF);
		}
		for (PartModifier modifier : def.modifiers()) {
			double amount = modifier.amountAt(level);
			UiKit.line(lines, Component.literal(format(amount) + " ").append(Component.translatable(modifier.attribute().value().getDescriptionId())),
				amount >= 0 ? UiKit.GOOD : UiKit.BAD);
		}
		for (String family : def.families()) {
			UiKit.line(lines, Component.translatable("screen.primordial.family", Component.translatable("family.primordial." + family)), 0xFFB39DDB);
		}

		UiKit.line(lines, Component.literal(" "), UiKit.TEXT);
		if (!unlocked) {
			Component from = def.source().map(src -> (Component) Component.translatable("entity." + src.getNamespace() + "." + src.getPath()))
				.orElse(Component.translatable("screen.primordial.editor.unknown_source"));
			UiKit.wrap(s.font(), lines, Component.translatable("screen.primordial.editor.eat_from", from), 220, UiKit.GOLD);
		} else {
			if (level >= 3) {
				UiKit.line(lines, Component.translatable("screen.primordial.editor.awakened"), UiKit.GOLD);
			} else {
				UiKit.line(lines, Component.translatable("screen.primordial.editor.next_level", progress.eatenCount(id), CreatureProgress.EATEN_FOR_LEVEL[level]), UiKit.GOLD);
			}
			PartRef equipped = body.part(slot);
			if (equipped != null && equipped.id().equals(id)) {
				UiKit.line(lines, Component.translatable("screen.primordial.body.equipped"), UiKit.GOOD);
				int styleCount = def.visual().isEmpty() ? 0 : def.visual().get().styles().size();
				if (styleCount > 0) {
					UiKit.line(lines, Component.translatable("screen.primordial.body.style", equipped.style() + 1, styleCount + 1), 0xFFB39DDB);
				}
			} else {
				Component problem = CreatureService.equipProblem(body, progress, slot, id);
				if (problem == null) {
					UiKit.line(lines, Component.translatable("screen.primordial.body.click_equip"), UiKit.ACCENT);
					UiKit.line(lines, Component.translatable("screen.primordial.editor.cost", CreatureService.equipCost(progress, def, id)), UiKit.ACCENT);
				} else {
					UiKit.wrap(s.font(), lines, problem, 220, UiKit.BAD);
				}
			}
		}
		return lines;
	}

	static String format(double v) {
		String text = String.format(Locale.ROOT, "%+.2f", v);
		if (text.contains(".")) {
			text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return text;
	}

	@Override
	public boolean click(CodexScreen s, double mx, double my, int button) {
		if (!UiKit.inside(mx, my, s.cx0(), s.cy0(), s.cx1() - s.cx0(), s.cy1() - s.cy0())) {
			return false;
		}
		CreatureBody body = Creatures.body(s.player());
		CreatureProgress progress = Creatures.progress(s.player());
		for (Hit hit : this.hits) {
			if (!UiKit.inside(mx, my, hit.x(), hit.y(), NODE, NODE)) {
				continue;
			}
			PartRef equipped = body.part(hit.slot());
			boolean isEquippedNode = hit.part() == null || (equipped != null && equipped.id().equals(hit.part()));
			PartDefinition styled = hit.part() == null ? null : PartRegistry.get(hit.part());
			int styles = styled == null || styled.visual().isEmpty() ? 0 : styled.visual().get().styles().size();
			if (button == 0 && isEquippedNode && hit.part() != null && styles > 0) {
				ClientPlayNetworking.send(new Payloads.SetPartStyle(hit.slot(), (equipped.style() + 1) % (styles + 1)));
			} else if (button == 1 && isEquippedNode) {
				ClientPlayNetworking.send(new Payloads.EquipPart(hit.slot(), Optional.empty()));
			} else if (button == 0 && hit.part() != null && progress.unlocked(hit.part()) && !isEquippedNode) {
				ClientPlayNetworking.send(new Payloads.EquipPart(hit.slot(), Optional.of(hit.part())));
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean drag(CodexScreen s, double dx, double dy) {
		if (!UiKit.inside(s.mouseX(), s.mouseY(), s.cx0(), s.cy0(), s.cx1() - s.cx0(), s.cy1() - s.cy0())) {
			return false;
		}
		this.offX = (int) Math.max(-900, Math.min(0, this.offX + dx));
		this.offY = (int) Math.max(-900, Math.min(0, this.offY + dy));
		return true;
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.offY = (int) Math.max(-900, Math.min(0, this.offY + amount * 24));
		return true;
	}
}
