package com.primordial.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.primordial.Primordial;
import com.primordial.creature.FeedingService;
import com.primordial.entity.PrimordialEntities;
import com.primordial.event.PrimordialEvents;
import com.primordial.net.Payloads;
import com.primordial.part.PartDefinition;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.PufferfishRenderer;
import net.minecraft.client.renderer.entity.SalmonRenderer;
import net.minecraft.network.chat.Component;

public class PrimordialClient implements ClientModInitializer {
	private static KeyMapping ability1;
	private static KeyMapping ability2;
	private static KeyMapping evolve;
	private static KeyMapping nest;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(PrimordialEntities.SARDINHA_PRATA, CodRenderer::new);
		EntityRendererRegistry.register(PrimordialEntities.PEIXE_PAPAGAIO, SalmonRenderer::new);
		EntityRendererRegistry.register(PrimordialEntities.BAIACU, PufferfishRenderer::new);

		ability1 = key("ability1", InputConstants.KEY_Z);
		ability2 = key("ability2", InputConstants.KEY_X);
		evolve = key("evolve", InputConstants.KEY_V);
		nest = key("nest", InputConstants.KEY_N);

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player == null) {
				return;
			}
			while (ability1.consumeClick()) {
				ClientPlayNetworking.send(new Payloads.UseAbility(0));
			}
			while (ability2.consumeClick()) {
				ClientPlayNetworking.send(new Payloads.UseAbility(1));
			}
			while (evolve.consumeClick()) {
				ClientPlayNetworking.send(new Payloads.Evolve());
			}
			while (nest.consumeClick()) {
				ClientPlayNetworking.send(new Payloads.MakeNest());
			}
		});

		PrimordialEvents.clientAttackHook = pos -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null && FeedingService.isPlant(mc.level.getBlockState(pos))) {
				ClientPlayNetworking.send(new Payloads.EatPlant(pos));
			}
		};

		ClientPlayNetworking.registerGlobalReceiver(Payloads.OpenOrigin.TYPE, (payload, ctx) -> ctx.client().setScreenAndShow(new OriginScreen()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.PartToast.TYPE, (payload, ctx) -> {
			Component name = PartDefinition.name(payload.part());
			Component message = switch (payload.kind()) {
				case UNLOCKED -> Component.translatable("msg.primordial.toast_unlocked", name).withStyle(ChatFormatting.GREEN);
				case LEVEL_UP -> Component.translatable("msg.primordial.toast_level_up", name, payload.level()).withStyle(ChatFormatting.AQUA);
				case AWAKENED -> Component.translatable("msg.primordial.toast_awakened", name).withStyle(ChatFormatting.GOLD);
				case STOLEN -> Component.translatable("msg.primordial.toast_unlocked", name).withStyle(ChatFormatting.RED);
			};
			ctx.player().sendSystemMessage(message);
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Evolved.TYPE, (payload, ctx) ->
			ctx.player().sendSystemMessage(Component.translatable("msg.primordial.you_evolved", payload.stage()).withStyle(ChatFormatting.GOLD)));

		HudElementRegistry.addLast(Primordial.id("hud"), PrimordialHud::render);
	}

	private static KeyMapping key(String name, int code) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.primordial." + name, code, KeyMapping.Category.MISC));
	}
}
