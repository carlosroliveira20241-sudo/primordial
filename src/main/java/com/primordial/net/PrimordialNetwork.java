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
		c2s.register(Payloads.ChooseMutation.TYPE, Payloads.ChooseMutation.CODEC);
		c2s.register(Payloads.SetAbilityOrder.TYPE, Payloads.SetAbilityOrder.CODEC);
		c2s.register(Payloads.UpgradeStrength.TYPE, Payloads.UpgradeStrength.CODEC);
		c2s.register(Payloads.UpgradeBrain.TYPE, Payloads.UpgradeBrain.CODEC);
		c2s.register(Payloads.BuyIntellect.TYPE, Payloads.BuyIntellect.CODEC);
		c2s.register(Payloads.SetIntellectSlot.TYPE, Payloads.SetIntellectSlot.CODEC);
		c2s.register(Payloads.UseIntellect.TYPE, Payloads.UseIntellect.CODEC);
		c2s.register(Payloads.Plant.TYPE, Payloads.Plant.CODEC);
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
		ServerPlayNetworking.registerGlobalReceiver(Payloads.ChooseMutation.TYPE, (p, ctx) ->
			com.primordial.mutation.MutationService.choose(ctx.player(), p.mutation()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.SetAbilityOrder.TYPE, (p, ctx) ->
			AbilityManager.setOrder(ctx.player(), p.order()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.UpgradeStrength.TYPE, (p, ctx) -> com.primordial.intellect.IntellectService.upgradeStrength(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.UpgradeBrain.TYPE, (p, ctx) -> com.primordial.intellect.IntellectService.upgradeBrain(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.BuyIntellect.TYPE, (p, ctx) -> com.primordial.intellect.IntellectService.buy(ctx.player(), p.ability()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.SetIntellectSlot.TYPE, (p, ctx) -> com.primordial.intellect.IntellectService.setSlot(ctx.player(), p.slot(), p.ability()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.UseIntellect.TYPE, (p, ctx) -> AbilityManager.useIntellect(ctx.player(), p.slot()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Plant.TYPE, (p, ctx) ->
			com.primordial.world.WorldInteractions.plant(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.EatMorsel.TYPE, (p, ctx) ->
			com.primordial.event.PrimordialEvents.eatNearestMorsel(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Evolve.TYPE, (p, ctx) ->
			CreatureService.evolve(ctx.player()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.SetLook.TYPE, (p, ctx) ->
			CreatureService.setLook(ctx.player(), p.primary(), p.secondary(), p.detail(), p.pattern(), p.look()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.UseAbility.TYPE, (p, ctx) ->
			AbilityManager.use(ctx.player(), p.slot()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.EatPlant.TYPE, (p, ctx) ->
			com.primordial.world.WorldInteractions.bite(ctx.player(), p.pos()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.MakeNest.TYPE, (p, ctx) ->
			AbilityManager.makeNest(ctx.player()));
	}
}
