package com.primordial.ability;

import com.primordial.creature.BodySlot;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Habilidades ativas. No protótipo existem duas: Investida e Inflar. */
public final class AbilityManager {
	private static final Map<String, Integer> COOLDOWN_TICKS = Map.of("investida", 50, "inflar", 300);
	private static final Map<UUID, Map<String, Long>> LAST_USED = new HashMap<>();

	private AbilityManager() {
	}

	/** Habilidades que a criatura tem agora, na ordem dos slots do corpo (a posição = tecla Z, X...). */
	public static List<Identifier> abilitiesOf(Player player) {
		CreatureBody body = Creatures.body(player);
		List<Identifier> result = new ArrayList<>();
		for (BodySlot slot : body.stage().slots) {
			PartRef ref = body.part(slot);
			if (ref == null) {
				continue;
			}
			PartDefinition def = PartRegistry.get(ref.id());
			if (def != null && def.ability().isPresent() && COOLDOWN_TICKS.containsKey(def.ability().get().getPath())
				&& !result.contains(def.ability().get())) {
				result.add(def.ability().get());
			}
		}
		return result;
	}

	public static void use(ServerPlayer player, int index) {
		List<Identifier> abilities = abilitiesOf(player);
		if (!Creatures.isCreature(player) || index < 0 || index >= abilities.size()) {
			return;
		}
		String name = abilities.get(index).getPath();
		long now = player.level().getGameTime();
		Map<String, Long> used = LAST_USED.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
		long readyAt = used.getOrDefault(name, 0L) + COOLDOWN_TICKS.get(name);
		if (now < readyAt) {
			player.sendSystemMessage(Component.translatable("msg.primordial.cooldown", (readyAt - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		used.put(name, now);
		switch (name) {
			case "investida" -> dash(player);
			case "inflar" -> inflate(player);
			default -> {
			}
		}
	}

	private static void dash(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 look = player.getLookAngle();
		player.setDeltaMovement(look.scale(1.6));
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.2).move(look.scale(1.5)), e -> e != player)) {
			target.hurtServer(level, player.damageSources().playerAttack(player), 3.0F);
		}
		level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 0.2, player.getZ(), 25, 0.3, 0.2, 0.3, 0.1);
		CreatureService.playSound(player, SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.6F, 1.6F);
	}

	private static void inflate(ServerPlayer player) {
		ServerLevel level = player.level();
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1, false, true, true));
		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1, false, false, true));
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(2.5), e -> e != player)) {
			Vec3 away = target.position().subtract(player.position()).normalize().scale(0.8);
			target.setDeltaMovement(away.x, 0.2, away.z);
		}
		level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.3, player.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
		CreatureService.playSound(player, SoundEvents.PUFFER_FISH_BLOW_UP, 0.8F, 1.0F);
	}

	public static void makeNest(ServerPlayer player) {
		CreatureService.setNest(player);
	}
}
