package com.primordial.client;

import com.primordial.ability.Abilities;
import com.primordial.creature.BodySlot;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureProgress;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.net.Payloads;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Editor de Criatura (tecla K): à esquerda os encaixes do corpo, no meio as partes que cabem no encaixe escolhido
 * (as que você já comeu podem ser equipadas, as outras mostram de qual criatura vêm) e embaixo os detalhes.
 */
public class EditorScreen extends Screen {
	private static final String[] ROMAN = {"", "I", "II", "III"};
	private static final int ROW_H = 22;

	private BodySlot selected;
	private CreatureBody lastBody;
	private CreatureProgress lastProgress;
	private final List<Row> rows = new ArrayList<>();
	private int listX;
	private int listY;

	private record Row(int y, Identifier part) {
	}

	public EditorScreen() {
		super(Component.translatable("screen.primordial.editor"));
	}

	private Player player() {
		return Minecraft.getInstance().player;
	}

	@Override
	protected void init() {
		Player player = player();
		CreatureBody body = Creatures.body(player);
		CreatureProgress progress = Creatures.progress(player);
		this.lastBody = body;
		this.lastProgress = progress;
		this.rows.clear();
		if (this.selected == null || !body.stage().slots.contains(this.selected)) {
			this.selected = body.stage().slots.get(0);
		}

		int left = Math.max(10, this.width / 2 - 215);
		int top = 40;

		// Encaixes do corpo.
		int y = top;
		for (BodySlot slot : body.stage().slots) {
			PartRef ref = body.part(slot);
			Component label = Component.translatable(slot.translationKey()).append(": ")
				.append(ref == null ? Component.translatable("screen.primordial.editor.empty") : PartDefinition.name(ref.id()));
			if (slot == this.selected) {
				label = Component.literal("> ").append(label);
			}
			this.addRenderableWidget(Button.builder(label, b -> {
				this.selected = slot;
				this.rebuildWidgets();
			}).bounds(left, y, 170, 20).build());
			y += 22;
		}

		// Partes que cabem no encaixe escolhido.
		this.listX = left + 180;
		this.listY = top;
		List<Identifier> parts = new ArrayList<>();
		PartRegistry.all().forEach((id, def) -> {
			if (def.slot() == this.selected.type()) {
				parts.add(id);
			}
		});
		parts.sort(Comparator.comparing((Identifier id) -> !progress.unlocked(id)).thenComparing(Identifier::getPath));

		PartRef current = body.part(this.selected);
		int ry = this.listY;
		for (Identifier id : parts) {
			PartDefinition def = PartRegistry.get(id);
			boolean unlocked = progress.unlocked(id);
			boolean equipped = current != null && current.id().equals(id);
			Component label = rowLabel(id, def, progress, unlocked, equipped);
			Button button = Button.builder(label, b -> ClientPlayNetworking.send(new Payloads.EquipPart(this.selected, Optional.of(id))))
				.bounds(this.listX, ry, 250, 20).build();
			button.active = unlocked && !equipped;
			this.addRenderableWidget(button);
			this.rows.add(new Row(ry, id));
			ry += ROW_H;
		}

		// Tirar a parte do encaixe (boca e primeiro órgão nunca ficam vazios).
		if (this.selected != BodySlot.MOUTH && this.selected != BodySlot.ORGAN_1 && current != null) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.primordial.editor.remove"),
				b -> ClientPlayNetworking.send(new Payloads.EquipPart(this.selected, Optional.empty())))
				.bounds(this.listX, ry + 4, 250, 20).build());
		}

		int bottom = this.height - 28;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.primordial.editor.look"),
			b -> this.minecraft.setScreenAndShow(new LookScreen())).bounds(left, bottom, 170, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
			.bounds(left + 180, bottom, 250, 20).build());
	}

	private Component rowLabel(Identifier id, PartDefinition def, CreatureProgress progress, boolean unlocked, boolean equipped) {
		Component name = PartDefinition.name(id);
		if (!unlocked) {
			Component from = def.source().map(source -> (Component) Component.translatable("entity." + source.getNamespace() + "." + source.getPath()))
				.orElse(Component.translatable("screen.primordial.editor.unknown_source"));
			return Component.literal("[?] ").append(name).append(" - ").append(Component.translatable("screen.primordial.editor.eat_from", from));
		}
		int level = Math.max(1, progress.partLevel(id));
		MutableComponent text = Component.literal(equipped ? "* " : "").append(name).append(" " + ROMAN[Math.min(level, 3)]);
		if (def.ability().isPresent()) {
			text.append(" [").append(abilityName(def.ability().get())).append("]");
		}
		return equipped ? text.withStyle(ChatFormatting.GREEN) : text;
	}

	private static Component abilityName(Identifier ability) {
		return Component.translatable("ability." + ability.getNamespace() + "." + ability.getPath());
	}

	@Override
	public void tick() {
		Player player = player();
		if (player == null) {
			return;
		}
		if (!Creatures.body(player).equals(this.lastBody) || !Creatures.progress(player).equals(this.lastProgress)) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		Player player = player();
		CreatureBody body = Creatures.body(player);
		CreatureProgress progress = Creatures.progress(player);
		int left = Math.max(10, this.width / 2 - 215);

		graphics.text(this.font, this.title, left, 12, 0xFFFFFFFF);
		graphics.text(this.font, Component.translatable("hud.primordial.dna", progress.dna()), left, 24, 0xFF80DEEA);
		graphics.text(this.font, Component.translatable("hud.primordial.stage", Component.translatable(body.stage().translationKey())),
			left + 180, 24, 0xFFFFD54F);

		// Detalhes da parte sob o mouse (ou da equipada no encaixe, se o mouse não está em nenhuma).
		Identifier shown = null;
		for (Row row : this.rows) {
			if (mouseX >= this.listX && mouseX < this.listX + 250 && mouseY >= row.y() && mouseY < row.y() + 20) {
				shown = row.part();
			}
		}
		if (shown == null) {
			PartRef equipped = body.part(this.selected);
			shown = equipped == null ? null : equipped.id();
		}
		if (shown != null) {
			drawDetails(graphics, body, progress, shown, left + 180, this.height - 90);
		}
	}

	private void drawDetails(GuiGraphicsExtractor graphics, CreatureBody body, CreatureProgress progress, Identifier id, int x, int y) {
		PartDefinition def = PartRegistry.get(id);
		if (def == null) {
			return;
		}
		int width = 250;
		List<net.minecraft.util.FormattedCharSequence> desc = this.font.split(PartDefinition.description(id), width);
		List<net.minecraft.util.FormattedCharSequence> abilityDesc = List.of();
		if (def.ability().isPresent()) {
			abilityDesc = this.font.split(Component.translatable("ability." + def.ability().get().getNamespace() + "." + def.ability().get().getPath() + ".desc"), width);
		}
		int height = 14 + desc.size() * 10 + (def.ability().isPresent() ? 14 + abilityDesc.size() * 10 : 0) + 12;
		y = Math.min(y, this.height - 34 - height);
		graphics.fill(x - 4, y - 4, x + width + 4, y + height, 0xC0000000);

		graphics.text(this.font, PartDefinition.name(id).copy().withStyle(ChatFormatting.BOLD), x, y, def.rarity().color | 0xFF000000);
		int line = y + 12;
		for (var text : desc) {
			graphics.text(this.font, text, x, line, 0xFFCCCCCC);
			line += 10;
		}
		if (def.ability().isPresent()) {
			String name = def.ability().get().getPath();
			String seconds = Abilities.exists(name) ? String.format("%.0f", Abilities.get(name).cooldownTicks() / 20.0) : "?";
			graphics.text(this.font, Component.translatable("screen.primordial.editor.gives", abilityName(def.ability().get()), seconds), x, line + 2, 0xFFB2FF59);
			line += 14;
			for (var text : abilityDesc) {
				graphics.text(this.font, text, x, line, 0xFFE0E0E0);
				line += 10;
			}
		}
		if (progress.unlocked(id)) {
			int level = Math.max(1, progress.partLevel(id));
			Component state = level >= 3
				? Component.translatable("screen.primordial.editor.awakened")
				: Component.translatable("screen.primordial.editor.next_level", progress.eatenCount(id), CreatureProgress.EATEN_FOR_LEVEL[level]);
			graphics.text(this.font, state, x, line + 2, 0xFFFFD54F);
			if (!progress.hasBought(id) && !def.base()) {
				graphics.text(this.font, Component.translatable("screen.primordial.editor.cost", def.cost()), x + 170, line + 2, 0xFF80DEEA);
			}
		} else {
			Component problem = CreatureService.equipProblem(body, progress, this.selected, id);
			if (problem != null) {
				graphics.text(this.font, problem, x, line + 2, 0xFFFF8A80);
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
