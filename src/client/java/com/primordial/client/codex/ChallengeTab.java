package com.primordial.client.codex;

import com.primordial.journal.Challenge;
import com.primordial.journal.ChallengeRegistry;
import com.primordial.journal.JournalService;
import com.primordial.journal.JournalState;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Aba Desafios: o que fazer para ganhar DNA e XP, com barra de progresso. */
final class ChallengeTab implements CodexTab {
	private static final int ROW_H = 44;
	private int scroll;

	@Override
	public Component title() {
		return Component.translatable("screen.primordial.tab.challenges");
	}

	@Override
	public void render(CodexScreen s, GuiGraphicsExtractor g, int mx, int my) {
		int x0 = s.cx0();
		int y0 = s.cy0();
		int x1 = s.cx1();
		int y1 = s.cy1();
		JournalState journal = JournalService.state(s.player());
		int done = journal.done().size();
		g.text(s.font(), Component.translatable("screen.primordial.challenges.count", done, ChallengeRegistry.all().size()), x0 + 8, y0 + 4, UiKit.GOLD);

		g.enableScissor(x0, y0 + 16, x1, y1);
		int y = y0 + 22 - this.scroll;
		for (Map.Entry<String, Challenge> entry : ChallengeRegistry.all().entrySet()) {
			Challenge challenge = entry.getValue();
			boolean complete = journal.done().contains(entry.getKey());
			int have = Math.min(challenge.goal(), journal.counter(challenge.counter()));
			UiKit.box(g, x0 + 6, y, x1 - 6, y + ROW_H - 4, complete ? 0xFF2A2310 : UiKit.PANEL, complete ? UiKit.GOLD : UiKit.BORDER);
			UiKit.box(g, x0 + 12, y + 6, x0 + 42, y + 36, complete ? 0xFF4A3A10 : 0xFF141A24, complete ? UiKit.GOLD : UiKit.LOCKED);
			g.centeredText(s.font(), complete ? "✔" : "☆", x0 + 27, y + 17, complete ? UiKit.GOLD : UiKit.DIM);
			g.text(s.font(), Challenge.name(entry.getKey()).copy().withStyle(net.minecraft.ChatFormatting.BOLD), x0 + 50, y + 6, complete ? UiKit.GOLD : UiKit.TEXT);
			g.text(s.font(), Challenge.description(entry.getKey()), x0 + 50, y + 18, UiKit.DIM);
			int barW = 120;
			UiKit.bar(g, x1 - barW - 14, y + 8, barW, 8, (double) have / challenge.goal(), complete ? UiKit.GOLD : UiKit.ACCENT);
			g.text(s.font(), have + "/" + challenge.goal(), x1 - barW - 14, y + 19, UiKit.DIM);
			g.text(s.font(), Component.translatable("screen.primordial.challenges.reward", challenge.dna()), x1 - 62, y + 19, 0xFF80DEEA);
			y += ROW_H;
		}
		g.disableScissor();
	}

	@Override
	public boolean scroll(CodexScreen s, double amount) {
		this.scroll = (int) Math.max(0, Math.min(1400, this.scroll - amount * 24));
		return true;
	}
}
