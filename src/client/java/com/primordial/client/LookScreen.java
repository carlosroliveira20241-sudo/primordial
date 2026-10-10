package com.primordial.client;

import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.creature.Origin;
import com.primordial.creature.Pattern;
import com.primordial.look.Look;
import com.primordial.look.LookRegistry;
import com.primordial.net.Payloads;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Escolha do corpo e da cor. As formas vêm de {@code looks/*.json} (as mesmas que as criaturas do mod usam).
 * Sem origem: é o editor de aparência (tecla K), que custa um pouco de DNA. Com origem: é o fim da criação do personagem.
 */
public class LookScreen extends Screen {
	private static final int[] COLORS = {
		-1, 0x4A90C2, 0xE8C547, 0xE74C3C, 0x2ECC9A, 0x9B59B6, 0xFF9B3D, 0xF5F5F5, 0x34495E, 0xFF7EB6, 0x7DCEA0
	};

	private final Origin origin;
	private final List<Identifier> looks = LookRegistry.selectable();
	private int lookIndex;
	private int colorIndex;
	private Button lookButton;

	public LookScreen() {
		this(null);
	}

	public LookScreen(Origin origin) {
		super(Component.translatable("screen.primordial.look"));
		this.origin = origin;
		Minecraft mc = Minecraft.getInstance();
		if (origin == null && mc.player != null) {
			CreatureBody body = Creatures.body(mc.player);
			int at = this.looks.indexOf(body.look());
			this.lookIndex = Math.max(at, 0);
			for (int i = 1; i < COLORS.length; i++) {
				if (COLORS[i] == body.primaryColor()) {
					this.colorIndex = i;
				}
			}
		}
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = this.height / 2 - 40;

		this.addRenderableWidget(Button.builder(Component.literal("<"), b -> shiftLook(-1)).bounds(cx - 130, y, 20, 20).build());
		this.lookButton = this.addRenderableWidget(Button.builder(lookLabel(), b -> shiftLook(1)).bounds(cx - 106, y, 212, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), b -> shiftLook(1)).bounds(cx + 110, y, 20, 20).build());

		y += 30;
		int x = cx - (COLORS.length * 22) / 2;
		for (int i = 0; i < COLORS.length; i++) {
			int index = i;
			Component label = i == 0 ? Component.translatable("screen.primordial.look.default_color")
				: Component.literal("■").withStyle(Style.EMPTY.withColor(COLORS[i]));
			this.addRenderableWidget(Button.builder(label, b -> this.colorIndex = index).bounds(x + i * 22, y, i == 0 ? 20 : 20, 20).build());
		}

		y += 40;
		Component confirm = Component.translatable(this.origin == null ? "screen.primordial.look.apply" : "screen.primordial.look.confirm", CreatureService.LOOK_CHANGE_COST);
		this.addRenderableWidget(Button.builder(confirm, b -> confirm()).bounds(cx - 100, y, 200, 20).build());
		if (this.origin == null) {
			this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).bounds(cx - 100, y + 24, 200, 20).build());
		}
	}

	private Identifier currentLook() {
		return this.looks.isEmpty() ? Look.DEFAULT_ID : this.looks.get(this.lookIndex);
	}

	private int currentColor() {
		return this.colorIndex == 0 ? LookRegistry.get(currentLook()).tint() : COLORS[this.colorIndex];
	}

	private Component lookLabel() {
		return Look.name(currentLook());
	}

	private void shiftLook(int by) {
		if (this.looks.isEmpty()) {
			return;
		}
		this.lookIndex = Math.floorMod(this.lookIndex + by, this.looks.size());
		this.lookButton.setMessage(lookLabel());
	}

	private void confirm() {
		int color = currentColor();
		if (this.origin != null) {
			ClientPlayNetworking.send(new Payloads.ChooseOrigin(this.origin, "", color, CreatureBody.DEFAULT_SECONDARY, Pattern.LISO, currentLook()));
		} else {
			CreatureBody body = Creatures.body(Minecraft.getInstance().player);
			ClientPlayNetworking.send(new Payloads.SetLook(color, body.secondaryColor(), body.detailColor(), body.pattern(), currentLook()));
		}
		this.onClose();
	}

	@Override
	public void onClose() {
		if (this.origin == null) {
			this.minecraft.setScreenAndShow(new com.primordial.client.codex.CodexScreen());
		} else {
			super.onClose();
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return this.origin == null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int cx = this.width / 2;
		graphics.centeredText(this.font, this.title, cx, this.height / 2 - 72, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.primordial.look.sub"), cx, this.height / 2 - 60, 0xFFAAAAAA);

		// Prévia simples: o corpo desenhado com as proporções da forma, na cor escolhida.
		Look look = LookRegistry.get(currentLook());
		int color = 0xFF000000 | currentColor();
		int w = Math.round(24 * look.sz());
		int h = Math.round(10 * look.sy() * Math.max(look.sx(), 0.8F));
		int py = this.height / 2 + 70;
		graphics.fill(cx - w / 2, py - h / 2, cx + w / 2, py + h / 2, color);
		graphics.fill(cx + w / 2, py - h / 2 - 2, cx + w / 2 + 8, py + h / 2 + 2, color);
		graphics.fill(cx - w / 2 + 3, py - h / 4, cx - w / 2 + 6, py - h / 4 + 3, 0xFF101010);
	}
}
