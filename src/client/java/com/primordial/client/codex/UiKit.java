package com.primordial.client.codex;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** Cores e peças de desenho do menu estilo "conquistas". */
final class UiKit {
	static final int BG = 0xF00D1219;
	static final int PANEL = 0xFF18202C;
	static final int PANEL_LIGHT = 0xFF222E40;
	static final int BORDER = 0xFF3B4A63;
	static final int ACCENT = 0xFF4FC3F7;
	static final int GOLD = 0xFFFFC107;
	static final int TEXT = 0xFFE8EEF5;
	static final int DIM = 0xFF8A97A8;
	static final int GOOD = 0xFF7CE38B;
	static final int BAD = 0xFFFF8A80;
	static final int LOCKED = 0xFF4A5568;

	/** Uma linha de tooltip já quebrada e colorida. */
	record Line(FormattedCharSequence text, int color) {
	}

	private UiKit() {
	}

	static void box(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int fill, int border) {
		g.fill(x0, y0, x1, y1, border);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
	}

	static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Quadrado de conquista: moldura colorida, fundo, ícone e uma marca opcional por cima. */
	static void node(GuiGraphicsExtractor g, Font font, int x, int y, int size, int frame, int bg, ItemStack icon, boolean dim, String mark) {
		box(g, x, y, x + size, y + size, bg, frame);
		g.fill(x + 2, y + 2, x + size - 2, y + 4, 0x30FFFFFF);
		if (icon != null && !icon.isEmpty()) {
			g.item(icon, x + (size - 16) / 2, y + (size - 16) / 2);
		}
		if (dim) {
			g.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xB0101520);
		}
		if (mark != null) {
			g.centeredText(font, mark, x + size / 2, y + size / 2 - 4, 0xFFFFFFFF);
		}
	}

	static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, double fraction, int color) {
		g.fill(x, y, x + w, y + h, 0xFF0A0E14);
		int filled = (int) Math.round((w - 2) * Math.max(0, Math.min(1, fraction)));
		g.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, color);
	}

	static void wrap(Font font, List<Line> out, Component text, int width, int color) {
		for (FormattedCharSequence seq : font.split(text, width)) {
			out.add(new Line(seq, color));
		}
	}

	static void line(List<Line> out, Component text, int color) {
		out.add(new Line(text.getVisualOrderText(), color));
	}

	/** Desenha um tooltip ao lado do mouse, sem sair da tela. */
	static void tooltip(GuiGraphicsExtractor g, Font font, int mx, int my, int screenW, int screenH, List<Line> lines) {
		if (lines.isEmpty()) {
			return;
		}
		int width = 0;
		for (Line line : lines) {
			width = Math.max(width, font.width(line.text()));
		}
		int height = lines.size() * 10;
		int x = mx + 12;
		int y = my - 6;
		if (x + width + 10 > screenW) {
			x = Math.max(4, mx - width - 18);
		}
		if (y + height + 8 > screenH) {
			y = Math.max(4, screenH - height - 10);
		}
		box(g, x - 4, y - 4, x + width + 4, y + height + 2, 0xF0101820, ACCENT);
		int ty = y;
		for (Line line : lines) {
			g.text(font, line.text(), x, ty, line.color());
			ty += 10;
		}
	}

	static List<Line> newLines() {
		return new ArrayList<>();
	}
}
