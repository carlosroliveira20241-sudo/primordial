package com.primordial.client;

import com.primordial.mutation.Mutation;
import com.primordial.mutation.MutationRegistry;
import com.primordial.mutation.MutationService;
import com.primordial.mutation.MutationState;
import com.primordial.net.Payloads;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** A cada 3 níveis: 3 cartas de mutação, uma escolha. Cada carta tem um lado bom e um ruim. */
public class MutationScreen extends Screen {
	private static final int CARD_H = 44;

	private final List<Identifier> offer = new ArrayList<>();

	public MutationScreen() {
		super(Component.translatable("screen.primordial.mutation"));
	}

	@Override
	protected void init() {
		MutationState state = MutationService.state(Minecraft.getInstance().player);
		this.offer.clear();
		this.offer.addAll(state.offer());
		int x = this.width / 2 - 150;
		int y = this.height / 2 - 50;
		for (Identifier id : this.offer) {
			Mutation mutation = MutationRegistry.get(id);
			if (mutation == null) {
				continue;
			}
			Component label = Mutation.name(id).copy().withColor(mutation.rarity().color);
			this.addRenderableWidget(Button.builder(label, b -> {
				ClientPlayNetworking.send(new Payloads.ChooseMutation(id));
				this.onClose();
			}).bounds(x, y, 300, CARD_H).build());
			y += CARD_H + 6;
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> this.onClose())
			.bounds(x, y + 4, 300, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int cx = this.width / 2;
		graphics.centeredText(this.font, this.title, cx, this.height / 2 - 84, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.primordial.mutation.sub"), cx, this.height / 2 - 72, 0xFFAAAAAA);
		int y = this.height / 2 - 50;
		for (Identifier id : this.offer) {
			graphics.centeredText(this.font, Mutation.description(id), cx, y + 28, 0xFFE0E0E0);
			y += CARD_H + 6;
		}
	}
}
