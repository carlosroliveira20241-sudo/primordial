package com.primordial.entity;

import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.journal.JournalService;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;

/** Peixes-limpadores: alimentados, passam a seguir o jogador (até 2), curam um pouco e dão velocidade. */
public final class Symbionts {
	public static final int MAX = 2;
	public static final int FEED_COST = 3;
	private static final String OWNER = "owner:";

	private Symbionts() {
	}

	public static boolean isSymbiont(Mob mob) {
		CreatureDef def = CreatureRegistry.of(mob.getType());
		return def != null && def.has("symbiont");
	}

	public static UUID ownerOf(Mob mob) {
		for (String tag : mob.entityTags()) {
			if (tag.startsWith(OWNER)) {
				try {
					return UUID.fromString(tag.substring(OWNER.length()));
				} catch (IllegalArgumentException ignored) {
					return null;
				}
			}
		}
		return null;
	}

	/** O jogador apertou C sem Melhoria por perto: tenta alimentar um limpador. Retorna se achou algum. */
	public static boolean tryFeed(ServerPlayer player) {
		ServerLevel level = player.level();
		Mob target = null;
		int owned = 0;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64.0), Symbionts::isSymbiont)) {
			if (player.getUUID().equals(ownerOf(mob))) {
				owned++;
			}
		}
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(3.0), Symbionts::isSymbiont)) {
			if (ownerOf(mob) == null && (target == null || mob.distanceToSqr(player) < target.distanceToSqr(player))) {
				target = mob;
			}
		}
		if (target == null) {
			return false;
		}
		if (owned >= MAX) {
			player.sendSystemMessage(Component.translatable("msg.primordial.symbiont_max", MAX).withStyle(ChatFormatting.RED), true);
			return true;
		}
		if (Creatures.progress(player).dna() < FEED_COST) {
			player.sendSystemMessage(Component.translatable("msg.primordial.not_enough_dna", FEED_COST).withStyle(ChatFormatting.RED), true);
			return true;
		}
		CreatureService.addDna(player, -FEED_COST);
		target.addTag(OWNER + player.getUUID());
		target.setPersistenceRequired();
		JournalService.bump(player, "symbiont", 1);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, target.getX(), target.getY() + 0.4, target.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
		CreatureService.playSound(player, SoundEvents.DOLPHIN_EAT, 0.8F, 1.6F);
		player.sendSystemMessage(Component.translatable("msg.primordial.symbiont_fed").withStyle(ChatFormatting.GREEN), true);
		return true;
	}

	/** Chamado todo tick pelo próprio limpador: segue o dono e ajuda de vez em quando. */
	public static void tick(Mob mob) {
		UUID ownerId = ownerOf(mob);
		if (ownerId == null || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null || owner.level() != level) {
			return;
		}
		double distance = mob.distanceTo(owner);
		if (distance > 32) {
			mob.teleportTo(owner.getX(), owner.getY() + 0.5, owner.getZ());
		} else if (distance > 5 && mob.tickCount % 10 == 0) {
			mob.getNavigation().moveTo(owner, 1.4);
		}
		if (distance < 10 && mob.tickCount % 100 == 0) {
			owner.heal(1.0F);
			owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 120, 0, false, false, false));
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + 0.3, mob.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
		}
	}

	public static List<String> unused() {
		return List.of();
	}
}
