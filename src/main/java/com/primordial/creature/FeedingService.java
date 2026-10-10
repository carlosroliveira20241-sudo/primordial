package com.primordial.creature;

import com.primordial.item.PrimordialItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Alimentação: a criatura só ganha DNA e XP comendo comida que dropou. Colher uma alga ou matar um bicho solta o
 * alimento (Alga, Carne, Plâncton); depois é pegar e comer. A dieta da boca decide o que ela aceita.
 */
public final class FeedingService {
	private FeedingService() {
	}

	public static boolean isPlant(BlockState state) {
		return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS);
	}

	public static boolean isFood(ItemStack stack) {
		return stack.is(PrimordialItems.ALGA) || stack.is(PrimordialItems.CARNE) || stack.is(PrimordialItems.PLANCTON);
	}

	/** Morder/quebrar uma alga: ela some e solta uma Alga para comer. */
	public static void harvestPlant(ServerPlayer player, BlockPos pos) {
		if (!Creatures.isRestricted(player) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 36.0) {
			return;
		}
		ServerLevel level = player.level();
		if (!isPlant(level.getBlockState(pos))) {
			return;
		}
		level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
		level.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.0);
		CreatureService.playSound(player, SoundEvents.WET_GRASS_BREAK, 0.6F, 1.2F);
		drop(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(PrimordialItems.ALGA));
	}

	public static void drop(ServerLevel level, double x, double y, double z, ItemStack stack) {
		ItemEntity entity = new ItemEntity(level, x, y, z, stack);
		entity.setDeltaMovement(0, 0.05, 0);
		level.addFreshEntity(entity);
	}

	/** Come UMA unidade do alimento. Retorna se comeu (a dieta pode recusar). */
	public static boolean eat(ServerPlayer player, ItemStack stack) {
		Item item = stack.getItem();
		boolean meat = item == PrimordialItems.CARNE;
		Diet diet = Creatures.diet(player);
		if (meat ? !diet.eatsMeat() : !diet.eatsPlants()) {
			player.sendSystemMessage(Component.translatable("msg.primordial.diet_refuses").withStyle(ChatFormatting.RED), true);
			return false;
		}
		float efficiency = meat ? diet.meatEfficiency : diet.plantEfficiency;
		int dna = item == PrimordialItems.CARNE ? 5 : item == PrimordialItems.ALGA ? 3 : 2;
		int xp = item == PrimordialItems.CARNE ? 3 : item == PrimordialItems.ALGA ? 2 : 1;
		int hunger = item == PrimordialItems.CARNE ? 5 : item == PrimordialItems.ALGA ? 3 : 2;
		player.getFoodData().eat(hunger, 0.3F);
		if (meat) {
			player.heal(2.0F);
		} else {
			banquet(player);
		}
		float stomach = meat && Creatures.hasPassive(player, "iron_stomach") ? (Creatures.passiveLevel(player, "iron_stomach") >= 3 ? 3.0F : 2.0F) : 1.0F;
		CreatureService.addDna(player, Math.max(1, Math.round(dna * efficiency * stomach)));
		CreatureService.addXp(player, xp);
		CreatureService.playSound(player, SoundEvents.DOLPHIN_EAT, 0.6F, meat ? 0.9F : 1.4F);
		return true;
	}

	/** Boca de Filtro desperta (Banquete): comer planta ou plâncton regenera. */
	public static void banquet(ServerPlayer player) {
		if (Creatures.passiveLevel(player, "filter_feeder") >= 3) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
		}
	}
}
