package com.primordial.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.primordial.Primordial;
import com.primordial.client.render.FishBodyRenderer;
import com.primordial.creature.Creatures;
import com.primordial.entity.CreatureRegistry;
import com.primordial.look.Look;
import com.primordial.part.PartRegistry;
import java.util.List;
import net.minecraft.resources.Identifier;
import com.primordial.look.LookRegistry;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.network.chat.Component;

public class PrimordialClient implements ClientModInitializer {
	private static KeyMapping ability1;
	private static KeyMapping ability2;
	private static KeyMapping evolve;
	private static KeyMapping nest;
	private static KeyMapping looks;
	private static KeyMapping eat;
	private static KeyMapping plant;
	private static KeyMapping mutation;

	@Override
	public void onInitializeClient() {


		// Uma criatura por arquivo de creatures/*.json; o corpo vem da aparência (looks/*.json) que ela escolhe.
		CreatureRegistry.types().forEach((id, type) -> {
			Look look = LookRegistry.get(CreatureRegistry.get(id).look());
			registerFish(type, look, PartRegistry.droppedBy(id));
		});

		ability1 = key("ability1", InputConstants.KEY_Z);
		ability2 = key("ability2", InputConstants.KEY_X);
		evolve = key("evolve", InputConstants.KEY_V);
		nest = key("nest", InputConstants.KEY_N);
		looks = key("looks", InputConstants.KEY_K);
		eat = key("eat", InputConstants.KEY_C);
		plant = key("plant", InputConstants.KEY_G);
		mutation = key("mutation", InputConstants.KEY_M);

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
			while (mutation.consumeClick()) {
				if (Creatures.isCreature(mc.player) && com.primordial.mutation.MutationService.state(mc.player).pending() > 0) {
					mc.setScreenAndShow(new MutationScreen());
				}
			}
			while (plant.consumeClick()) {
				ClientPlayNetworking.send(new Payloads.Plant());
			}
			while (eat.consumeClick()) {
				ClientPlayNetworking.send(new Payloads.EatMorsel());
			}
			while (looks.consumeClick()) {
				if (Creatures.isCreature(mc.player)) {
					mc.setScreenAndShow(new EditorScreen());
				}
			}
			while (nest.consumeClick()) {
				ClientPlayNetworking.send(new Payloads.MakeNest());
			}
		});

		PrimordialEvents.clientAttackHook = pos -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level != null && !mc.level.getBlockState(pos).isAir()) {
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

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registerFish(EntityType type, Look look, List<Identifier> parts) {
		EntityRendererRegistry.register(type, ctx -> new FishBodyRenderer(ctx, look, parts));
	}

	private static KeyMapping key(String name, int code) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.primordial." + name, code, KeyMapping.Category.MISC));
	}
}
