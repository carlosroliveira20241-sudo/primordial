package com.primordial.client;

import com.primordial.creature.Origin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Tela "Escolha sua Origem": 3 larvas com bocas e bônus diferentes. */
public class OriginScreen extends Screen {
	public OriginScreen() {
		super(Component.translatable("screen.primordial.origin"));
	}

	@Override
	protected void init() {
		int y = this.height / 2 - 24;
		for (Origin origin : Origin.values()) {
			Component label = Component.translatable(origin.translationKey()).append(" - ").append(Component.translatable(origin.translationKey() + ".desc"));
			this.addRenderableWidget(Button.builder(label, button -> choose(origin)).bounds(this.width / 2 - 150, y, 300, 20).build());
			y += 26;
		}
	}

	private void choose(Origin origin) {
		// Depois da origem vem a escolha do corpo e da cor; o pedido ao servidor sai de lá.
		this.minecraft.setScreenAndShow(new LookScreen(origin));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 52, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.primordial.origin.sub"), this.width / 2, this.height / 2 - 40, 0xFFAAAAAA);
	}
}
