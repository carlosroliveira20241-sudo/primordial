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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Corpo, três cores (principal, secundária e de detalhe), padrão de pele, proporções (comprimento e cabeça) e, na
 * criação, o nome da espécie. As formas vêm de {@code looks/*.json}. Sem origem: é o editor de aparência, que custa DNA.
 */
public class LookScreen extends Screen {
	private static final int[] COLORS = {0x4A90C2, 0xE8C547, 0xE74C3C, 0x2ECC9A, 0x9B59B6, 0xFF9B3D, 0xF5F5F5, 0x34495E, 0xFF7EB6, 0x7DCEA0};

	private final Origin origin;
	private final List<Identifier> looks = LookRegistry.selectable();
	private int lookIndex;
	private int primary = -1;
	private int secondary = CreatureBody.DEFAULT_SECONDARY;
	private int detail = CreatureBody.DEFAULT_DETAIL;
	private Pattern pattern = Pattern.LISO;
	private int length = 100;
	private int head = 100;
	private Button lookButton;
	private Button patternButton;
	private EditBox nameBox;

	public LookScreen() {
		this(null);
	}

	public LookScreen(Origin origin) {
		super(Component.translatable("screen.primordial.look"));
		this.origin = origin;
		Minecraft mc = Minecraft.getInstance();
		if (origin == null && mc.player != null) {
			CreatureBody body = Creatures.body(mc.player);
			this.lookIndex = Math.max(this.looks.indexOf(body.look()), 0);
			this.pattern = body.pattern();
			this.primary = body.primaryColor();
			this.secondary = body.secondaryColor();
			this.detail = body.detailColor();
			this.length = body.length();
			this.head = body.head();
		}
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int y = 30;

		this.addRenderableWidget(Button.builder(Component.literal("<"), b -> shiftLook(-1)).bounds(cx - 130, y, 20, 20).build());
		this.lookButton = this.addRenderableWidget(Button.builder(lookLabel(), b -> shiftLook(1)).bounds(cx - 106, y, 212, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal(">"), b -> shiftLook(1)).bounds(cx + 110, y, 20, 20).build());

		// três fileiras de cores: principal, secundária, detalhe
		y += 36;
		swatches(cx, y, 0);
		y += 36;
		swatches(cx, y, 1);
		y += 36;
		swatches(cx, y, 2);

		y += 30;
		this.patternButton = this.addRenderableWidget(Button.builder(patternLabel(), b -> {
			Pattern[] all = Pattern.values();
			this.pattern = all[(this.pattern.ordinal() + 1) % all.length];
			this.patternButton.setMessage(patternLabel());
		}).bounds(cx - 106, y, 212, 20).build());

		// proporções: -  corpo  +   -  cabeça  +
		y += 26;
		this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.length = Math.max(70, this.length - 10)).bounds(cx - 130, y, 18, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.length = Math.min(140, this.length + 10)).bounds(cx - 4, y, 18, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.head = Math.max(70, this.head - 10)).bounds(cx + 20, y, 18, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.head = Math.min(140, this.head + 10)).bounds(cx + 112, y, 18, 20).build());

		y += 26;
		if (this.origin != null) {
			this.nameBox = new EditBox(this.font, cx - 106, y, 212, 20, Component.translatable("screen.primordial.look.name"));
			this.nameBox.setMaxLength(24);
			this.nameBox.setHint(Component.translatable("screen.primordial.look.name"));
			this.addRenderableWidget(this.nameBox);
			y += 26;
		}

		Component confirm = Component.translatable(this.origin == null ? "screen.primordial.look.apply" : "screen.primordial.look.confirm", CreatureService.LOOK_CHANGE_COST);
		this.addRenderableWidget(Button.builder(confirm, b -> confirm()).bounds(cx - 100, y, 200, 20).build());
		if (this.origin == null) {
			this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose()).bounds(cx - 100, y + 24, 200, 20).build());
		}
	}

	/** Uma fileira de amostras de cor; {@code row}: 0 principal, 1 secundária, 2 detalhe. */
	private void swatches(int cx, int y, int row) {
		int x = cx - (COLORS.length * 22) / 2;
		for (int i = 0; i < COLORS.length; i++) {
			int color = COLORS[i];
			this.addRenderableWidget(Button.builder(Component.literal("■").withStyle(Style.EMPTY.withColor(color)), b -> {
				switch (row) {
					case 0 -> this.primary = color;
					case 1 -> this.secondary = color;
					default -> this.detail = color;
				}
			}).bounds(x + i * 22, y, 20, 20).build());
		}
	}

	private Component patternLabel() {
		return Component.translatable("screen.primordial.look.pattern", Component.translatable(this.pattern.translationKey()));
	}

	private Identifier currentLook() {
		return this.looks.isEmpty() ? Look.DEFAULT_ID : this.looks.get(this.lookIndex);
	}

	private int currentPrimary() {
		return this.primary < 0 ? LookRegistry.get(currentLook()).tint() : this.primary;
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
		if (this.origin != null) {
			String name = this.nameBox == null ? "" : this.nameBox.getValue();
			ClientPlayNetworking.send(new Payloads.ChooseOrigin(this.origin, name, currentPrimary(), this.secondary, this.detail, this.pattern, currentLook()));
		} else {
			ClientPlayNetworking.send(new Payloads.SetLook(currentPrimary(), this.secondary, this.detail, this.pattern, currentLook(), this.length, this.head));
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
		graphics.centeredText(this.font, this.title, cx, 8, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.primordial.look.sub"), cx, 18, 0xFFAAAAAA);
		graphics.text(this.font, Component.translatable("screen.primordial.look.primary"), cx - 120, 62, 0xFFCCCCCC);
		graphics.text(this.font, Component.translatable("screen.primordial.look.secondary"), cx - 120, 98, 0xFFCCCCCC);
		graphics.text(this.font, Component.translatable("screen.primordial.look.detail"), cx - 120, 134, 0xFFCCCCCC);
		int propY = 30 + 36 * 3 + 30 + 26 + 6;
		graphics.text(this.font, Component.translatable("screen.primordial.look.length", this.length), cx - 108, propY, 0xFFCCCCCC);
		graphics.text(this.font, Component.translatable("screen.primordial.look.head", this.head), cx + 42, propY, 0xFFCCCCCC);

		// Prévia: corpo na cor principal, cauda/nadadeira na secundária, detalhe pequeno na de detalhe.
		Look look = LookRegistry.get(currentLook());
		int body = 0xFF000000 | currentPrimary();
		int fin = 0xFF000000 | this.secondary;
		int accent = 0xFF000000 | this.detail;
		int w = Math.round(24 * look.sz() * this.length / 100.0F);
		int h = Math.round(10 * look.sy() * Math.max(look.sx(), 0.8F));
		int hd = Math.round(8 * this.head / 100.0F);
		int px = this.width - 80;
		int py = 90;
		graphics.fill(px - w / 2, py - h / 2, px + w / 2, py + h / 2, body);
		graphics.fill(px + w / 2, py - h / 2 - hd / 2, px + w / 2 + hd, py + h / 2 + hd / 2, body);
		graphics.fill(px - w / 2 - 8, py - h / 2 - 2, px - w / 2, py + h / 2 + 2, fin);
		graphics.fill(px - 4, py - h / 2 - 4, px + 4, py - h / 2, fin);
		graphics.fill(px + w / 2 - 6, py - h / 2 - 8, px + w / 2 - 3, py - h / 2, accent);
		graphics.fill(px + w / 2 + 2, py - h / 4, px + w / 2 + 5, py - h / 4 + 3, 0xFF101010);
	}
}
