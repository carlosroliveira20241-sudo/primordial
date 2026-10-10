package com.primordial.client.codex;

import com.mojang.blaze3d.platform.InputConstants;
import com.primordial.client.LookScreen;
import com.primordial.client.MutationScreen;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureProgress;
import com.primordial.creature.Creatures;
import com.primordial.mutation.MutationService;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Menu da criatura no estilo da tela de conquistas: abas no topo, uma árvore que se arrasta com o mouse e dicas ao
 * passar o mouse. Abas: Corpo (equipar partes), Habilidades (ordem das teclas), Mutações e sinergias, Bestiário e Desafios.
 */
public class CodexScreen extends Screen {
	private static int lastTab;

	private final List<CodexTab> tabs = List.of(new CharacterTab(), new BodyTab(), new AbilityTab(), new IntellectTab(), new MutationTab(), new BestiaryTab(), new ChallengeTab(), new EvolutionTab());
	private int tab;
	private int wx0;
	private int wy0;
	private int wx1;
	private int wy1;
	private List<UiKit.Line> pendingTooltip;
	private int mouseX;
	private int mouseY;

	public CodexScreen() {
		super(Component.translatable("screen.primordial.codex"));
		this.tab = lastTab;
	}

	// ---- geometria usada pelas abas
	int cx0() {
		return this.wx0 + 10;
	}

	int cy0() {
		return this.wy0 + 38;
	}

	int cx1() {
		return this.wx1 - 10;
	}

	int cy1() {
		return this.wy1 - 32;
	}

	int mouseX() {
		return this.mouseX;
	}

	int mouseY() {
		return this.mouseY;
	}

	Player player() {
		return Minecraft.getInstance().player;
	}

	net.minecraft.client.gui.Font font() {
		return this.font;
	}

	void tooltip(List<UiKit.Line> lines) {
		this.pendingTooltip = lines;
	}

	int screenWidth() {
		return this.width;
	}

	int screenHeight() {
		return this.height;
	}

	@Override
	protected void init() {
		this.wx0 = Math.max(8, this.width / 2 - 260);
		this.wx1 = Math.min(this.width - 8, this.width / 2 + 260);
		this.wy0 = 8;
		this.wy1 = this.height - 8;
	}

	private int tabWidth() {
		return Math.min(96, (this.wx1 - this.wx0 - 130) / this.tabs.size());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
		this.mouseX = mx;
		this.mouseY = my;
		this.pendingTooltip = null;
		g.fill(0, 0, this.width, this.height, 0xA0000000);
		UiKit.box(g, this.wx0, this.wy0, this.wx1, this.wy1, UiKit.BG, UiKit.BORDER);
		g.fill(this.wx0 + 1, this.wy0 + 1, this.wx1 - 1, this.wy0 + 32, UiKit.PANEL);

		// abas
		int tw = tabWidth();
		for (int i = 0; i < this.tabs.size(); i++) {
			int x = this.wx0 + 8 + i * (tw + 2);
			boolean active = i == this.tab;
			boolean hover = UiKit.inside(mx, my, x, this.wy0 + 8, tw, 24);
			UiKit.box(g, x, this.wy0 + 8, x + tw, this.wy0 + 33, active ? UiKit.PANEL_LIGHT : hover ? 0xFF1D2838 : 0xFF121820, active ? UiKit.ACCENT : UiKit.BORDER);
			g.centeredText(this.font, this.tabs.get(i).title(), x + tw / 2, this.wy0 + 16, active ? 0xFFFFFFFF : UiKit.DIM);
		}

		// cabeçalho à direita: DNA, nível, estágio
		Player player = player();
		CreatureProgress progress = Creatures.progress(player);
		CreatureBody body = Creatures.body(player);
		Component info = Component.translatable("screen.primordial.codex.info", progress.dna(), progress.level(), Component.translatable(body.stage().translationKey()));
		g.text(this.font, info, this.wx1 - 8 - this.font.width(info), this.wy0 + 16, UiKit.ACCENT);

		// conteúdo
		g.fill(this.cx0() - 2, this.cy0() - 2, this.cx1() + 2, this.cy1() + 2, 0xFF0A0F15);
		this.tabs.get(this.tab).render(this, g, mx, my);

		// barra de baixo
		int by = this.wy1 - 28;
		g.fill(this.wx0 + 1, by, this.wx1 - 1, this.wy1 - 1, UiKit.PANEL);
		int pending = MutationService.state(player).pending();
		if (pending > 0) {
			g.text(this.font, Component.translatable("hud.primordial.mutation_ready"), this.wx0 + 10, by + 10, 0xFFE1A8FF);
		} else {
			g.text(this.font, Component.translatable("screen.primordial.codex.hint"), this.wx0 + 10, by + 10, UiKit.DIM);
		}
		button(g, this.wx1 - 90, by + 4, 80, 20, Component.translatable("gui.done"), mx, my);
		button(g, this.wx1 - 200, by + 4, 106, 20, Component.translatable("screen.primordial.editor.look"), mx, my);
		if (pending > 0) {
			button(g, this.wx1 - 306, by + 4, 102, 20, Component.translatable("screen.primordial.codex.mutate"), mx, my);
		}

		if (this.pendingTooltip != null) {
			UiKit.tooltip(g, this.font, mx, my, this.width, this.height, this.pendingTooltip);
		}
	}

	private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, int mx, int my) {
		boolean hover = UiKit.inside(mx, my, x, y, w, h);
		UiKit.box(g, x, y, x + w, y + h, hover ? 0xFF2B3A52 : UiKit.PANEL_LIGHT, hover ? UiKit.ACCENT : UiKit.BORDER);
		g.centeredText(this.font, label, x + w / 2, y + 6, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		int tw = tabWidth();
		for (int i = 0; i < this.tabs.size(); i++) {
			if (UiKit.inside(mx, my, this.wx0 + 8 + i * (tw + 2), this.wy0 + 8, tw, 24)) {
				this.tab = i;
				lastTab = i;
				return true;
			}
		}
		int by = this.wy1 - 28;
		if (UiKit.inside(mx, my, this.wx1 - 90, by + 4, 80, 20)) {
			this.onClose();
			return true;
		}
		if (UiKit.inside(mx, my, this.wx1 - 200, by + 4, 106, 20)) {
			this.minecraft.setScreenAndShow(new LookScreen());
			return true;
		}
		if (MutationService.state(player()).pending() > 0 && UiKit.inside(mx, my, this.wx1 - 306, by + 4, 102, 20)) {
			this.minecraft.setScreenAndShow(new MutationScreen());
			return true;
		}
		return this.tabs.get(this.tab).click(this, mx, my, event.button()) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		return this.tabs.get(this.tab).drag(this, dx, dy) || super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		return this.tabs.get(this.tab).scroll(this, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_K) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
