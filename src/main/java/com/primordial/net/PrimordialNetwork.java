package com.primordial.net;

import com.primordial.ability.AbilityManager;
import com.primordial.creature.CreatureService;
import com.primordial.creature.FeedingService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class PrimordialNetwork {
	private PrimordialNetwork() {
	}

	public static void init() {
		PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.serverboundPlay();
		c2s.register(Payloads.ChooseOrigin.TYPE, Payloads.ChooseOrigin.CODEC);
		c2s.register(Payloads.EquipPart.TYPE, Payloads.EquipPart.CODEC);
		c2s.register(Payloads.Evolve.TYPE, Payloads.Evolve.CODEC);
		c2s.register(Payloads.EatMorsel.TYPE, Payloads.EatMorsel.CODEC);
		c2s.register(Payloads.SetLook.TYPE, Payloads.SetLook.CODEC);
		c2s.register(Payloads.UseAbility.TYPE, Payloads.UseAbility.CODEC);
		c2s.register(Payloads.EatPlant.TYPE, Payloads.EatPlant.CODEC);
		c2s.register(Payloads.MakeNest.TYPE, Payloads.MakeNest.CODEC);

		PayloadTypeRegistry<net.minecraft.network.RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.clientboundPlay();
		s2c.register(Payloads.OpenOrigin.TYPE, Payloads.OpenOrigin.CODEC);
		s2c.register(Payloads.Cooldowns.TYPE, Payloads.Cooldowns.CODEC);
		s2c.register(Payloads.PartToast.TYPE, Payloads.PartToast.CODEC);
		s2c.register(Payloads.Evolved.TYPE, Payloads.Evolved.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(Payloads.ChooseOrigin.TYPE, (p, ctx) ->
			CreatureService.chooseOrigin(ctx.player(), p.origin(), p.speciesName(), p.primary(), p.secondary(), p.pattern(), p.look()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.EquipPart.TYPE, (p, ctx) ->
			CreatureService.equip(ctx.player(), p.slot(), p.part()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.EatMorsel.TYPE, (p, ctx) ->
			com.primordial.event.PrimordialEvents.eatNearestMorsel(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Evolve.TYPE, (p, ctx) ->
			CreatureService.evolve(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.SetLook.TYPE, (p, ctx) ->
			CreatureService.setLook(ctx.player(), p.primary(), p.secondary(), p.detail(), p.pattern(), p.look()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.UseAbility.TYPE, (p, ctx) ->
			AbilityManager.use(ctx.player(), p.slot()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.EatPlant.TYPE, (p, ctx) ->
			FeedingService.eatPlant(ctx.player(), p.pos()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.MakeNest.TYPE, (p, ctx) ->
			AbilityManager.makeNest(ctx.player()));
	}
}
