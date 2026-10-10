package com.primordial.client.codex;

import com.primordial.ability.Abilities;
import com.primordial.ability.AbilityManager;
import com.primordial.client.render.FishLook;
import com.primordial.creature.BodySlot;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureProgress;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.item.PrimordialItems;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartModifier;
import com.primordial.part.PartRegistry;
import com.primordial.synergy.Synergy;
import com.primordial.synergy.SynergyRegistry;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Aba Personagem: seu bicho em 3D à esquerda e, à direita, cada modificação equipada com sua habilidade e passivas. */
final class CharacterTab implements CodexTab {
	private static final String[] KEYS = {"Z", "X", "R", "F"};
	private int scroll;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.character");
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		Player player = s.player();
		CreatureBody body = Creatures.body(player);
		CreatureProgress progress = Creatures.progress(player);
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		int leftW = 190;

		// ---- esquerda: o personagem
		UiKit.box(g, x0 + 4, y0 + 4, x0 + leftW, y0 + 170, 0xFF0F1620, UiKit.BORDER);
		FishLook.previewing = true;
		try {
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0 + 8, y0 + 8, x0 + leftW - 4, y0 + 166, 70, 0.0625F, mx, my, player);
		} finally {
			FishLook.previewing = false;
		}
		String species = body.speciesName().isEmpty() ? player.getName().getString() : body.speciesName();
		g.centeredText(s.font(), species, x0 + leftW / 2 + 2, y0 + 176, UiKit.TEXT);
		g.centeredText(s.font(), Component.translatable(body.stage().translationKey()), x0 + leftW / 2 + 2, y0 + 188, UiKit.GOLD);

		int sy = y0 + 204;
		g.text(s.font(), Component.translatable("screen.primordial.character.stats"), x0 + 10, sy, UiKit.ACCENT);
		sy += 12;
		sy = stat(g, s, x0 + 10, sy, "attribute.name.max_health", String.format(Locale.ROOT, "%.0f", player.getMaxHealth()));
		sy = stat(g, s, x0 + 10, sy, "attribute.name.armor", String.format(Locale.ROOT, "%.0f", player.getAttributeValue(Attributes.ARMOR)));
		com.primordial.builder.BlockPouch pouch = com.primordial.builder.Builder.pouch(player);
		if (pouch.total() > 0 || Creatures.hasPassive(player, "sand_pouch")) {
			sy = stat(g, s, x0 + 10, sy, "screen.primordial.character.pouch", pouch.total() + "/" + com.primordial.builder.Builder.capacity(player));
		}
		sy = stat(g, s, x0 + 10, sy, "attribute.name.attack_damage", String.format(Locale.ROOT, "%.1f", player.getAttributeValue(Attributes.ATTACK_DAMAGE)));
		stat(g, s, x0 + 10, sy, "screen.primordial.character.level", String.valueOf(progress.level()));

		// ---- direita: as modificações equipadas, com descrição
		int rx = x0 + leftW + 10;
		g.enableScissor(rx, y0, x1, y1);
		int y = y0 + 6 - this.scroll;
		List<Identifier> synergies = SynergyRegistry.active(body);
		if (!synergies.isEmpty()) {
			g.text(s.font(), Component.translatable("screen.primordial.synergies"), rx, y, UiKit.GOLD);
			int sx = rx + s.font().width(Component.translatable("screen.primordial.synergies")) + 6;
			for (Identifier id : synergies) {
				g.text(s.font(), Synergy.name(id), sx, y, UiKit.GOOD);
				sx += s.font().width(Synergy.name(id)) + 10;
			}
			y += 16;
		}

		int width = x1 - rx - 8;
		List<Identifier> abilities = AbilityManager.abilitiesOf(player);
		for (BodySlot slot : body.stage().slots) {
			PartRef ref = body.part(slot);
			if (ref == null) {
				continue;
			}
			PartDefinition def = PartRegistry.get(ref.id());
			if (def == null) {
				continue;
			}
			int top = y;
			int color = 0xFF000000 | def.rarity().color;
			int height = measure(s, def, ref, width - 40);
			UiKit.box(g, rx, top, rx + width, top + height, UiKit.PANEL, color);
			g.item(PrimordialItems.morsel(ref.id()), rx + 6, top + 6);
			g.text(s.font(), PartDefinition.name(ref.id()).copy().withStyle(ChatFormatting.BOLD).append(" " + roman(ref.level())), rx + 28, top + 5, color);
			g.text(s.font(), Component.translatable(slot.translationKey()), rx + width - 6 - s.font().width(Component.translatable(slot.translationKey())), top + 5, UiKit.DIM);
			int ty = top + 18;
			for (var seq : s.font().split(PartDefinition.description(ref.id()), width - 40)) {
				g.text(s.font(), seq, rx + 28, ty, UiKit.TEXT);
				ty += 10;
			}
			if (def.ability().isPresent()) {
				Identifier ab = def.ability().get();
				int index = abilities.indexOf(ab);
				String key = index >= 0 && index < KEYS.length ? KEYS[index] : "-";
				String seconds = Abilities.exists(ab.getPath()) ? String.format(Locale.ROOT, "%.0f", Abilities.get(ab.getPath()).cooldownTicks() / 20.0) : "?";
				ty += 2;
				g.text(s.font(), Component.translatable("screen.primordial.character.active", key,
					Component.translatable("ability." + ab.getNamespace() + "." + ab.getPath()), seconds), rx + 28, ty, UiKit.GOOD);
				ty += 11;
				for (var seq : s.font().split(Component.translatable("ability." + ab.getNamespace() + "." + ab.getPath() + ".desc"), width - 40)) {
					g.text(s.font(), seq, rx + 28, ty, 0xFFC8D4E0);
					ty += 10;
				}
			}
			for (String passive : def.passives()) {
				ty += 2;
				for (var seq : s.font().split(Component.translatable("screen.primordial.passive", Component.translatable("passive.primordial." + passive)), width - 40)) {
					g.text(s.font(), seq, rx + 28, ty, 0xFF9AD0FF);
					ty += 10;
				}
			}
			for (PartModifier modifier : def.modifiers()) {
				double amount = modifier.amountAt(ref.level());
				g.text(s.font(), Component.literal(BodyTab.format(amount) + " ").append(Component.translatable(modifier.attribute().value().getDescriptionId())),
					rx + 28, ty + 1, amount >= 0 ? UiKit.GOOD : UiKit.BAD);
				ty += 10;
			}
			y = top + height + 6;
		}
		g.disableScissor();
	}

	/** Altura do cartão de uma parte, para empilhar os cartões sem sobrepor. */
	private int measure(CodexScreen s, PartDefinition def, PartRef ref, int textWidth) {
		int lines = s.font().split(PartDefinition.description(ref.id()), textWidth).size();
		int height = 18 + lines * 10 + 6;
		if (def.ability().isPresent()) {
			Identifier ab = def.ability().get();
			height += 13 + s.font().split(Component.translatable("ability." + ab.getNamespace() + "." + ab.getPath() + ".desc"), textWidth).size() * 10;
		}
		for (String passive : def.passives()) {
			height += 2 + s.font().split(Component.translatable("screen.primordial.passive", Component.translatable("passive.primordial." + passive)), textWidth).size() * 10;
		}
		height += def.modifiers().size() * 10;
		return height;
	}

	private static String roman(int level) {
		return level >= 3 ? "III" : level == 2 ? "II" : "I";
	}

	private int stat(GuiGraphicsExtractor g, CodexScreen s, int x, int y, String labelKey, String value) {
		g.text(s.font(), Component.translatable(labelKey), x, y, UiKit.DIM);
		g.text(s.font(), value, x + 130, y, UiKit.TEXT);
		return y + 11;
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.scroll = (int) Math.max(0, Math.min(1200, this.scroll - amount * 24));
		return true;
	}
}
