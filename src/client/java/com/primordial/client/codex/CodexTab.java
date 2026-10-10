package com.primordial.client.codex;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Uma aba do menu. A área útil é o retângulo (x0,y0)-(x1,y1) calculado pela tela. */
interface CodexTab {
	Component title();

	void render(CodexScreen screen, GuiGraphicsExtractor g, int mx, int my);

	default boolean click(CodexScreen screen, double mx, double my, int button) {
		return false;
	}

	default boolean drag(CodexScreen screen, double dx, double dy) {
		return false;
	}

	default boolean scroll(CodexScreen screen, double amount) {
		return false;
	}
}
