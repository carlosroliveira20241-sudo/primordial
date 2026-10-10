package com.primordial.world;

import com.primordial.Primordial;
import com.primordial.config.PrimordialConfig;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.creature.Diet;
import com.primordial.entity.CreatureRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * O oceano acontecendo sozinho: correntes marinhas que levam quem nada nelas, nuvens de plâncton que dão DNA a
 * herbívoros e eventos aleatórios (Desova em Massa, Frenesi Alimentar, Maré Vermelha).
 */
public final class OceanLife {
	/** Durante o Frenesi Alimentar, abates dão o dobro de DNA (lido por MorselDrops). */
	public static long frenzyUntil;

	private static final List<Cloud> CLOUDS = new ArrayList<>();
	private static long nextEvent = -1;
	private static BlockPos tideCenter;
	private static long tideUntil;

	private static final class Cloud {
		final Vec3 pos;
		int bites = 5;
		final long expires;

		Cloud(Vec3 pos, long expires) {
			this.pos = pos;
			this.expires = expires;
		}
	}

	private OceanLife() {
	}

	public static boolean frenzyActive(ServerLevel level) {
		return level.getGameTime() < frenzyUntil;
	}

	/** Um tick do Overworld: eventos e plâncton. As correntes ficam por jogador (veja {@link #tickPlayer}). */
	public static void tickLevel(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return;
		}
		long now = level.getGameTime();
		List<ServerPlayer> swimmers = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (Creatures.isCreature(player) && player.isInWater() && !player.isSpectator()) {
				swimmers.add(player);
			}
		}
		if (swimmers.isEmpty()) {
			return;
		}
		if (now % 20 == 0) {
			spawnPlankton(level, swimmers.get(level.getRandom().nextInt(swimmers.size())), now);
		}
		tickClouds(level, swimmers, now);
		tickTide(level, now);
		tickEvents(level, swimmers, now);
	}

	// ------------------------------------------------------------------ correntes

	/** Cada área de ~48 blocos pode ter uma corrente: direção e força vêm só da posição e da hora do mundo. */
	public static void tickPlayer(ServerLevel level, ServerPlayer player) {
		if (!player.isInWater() || !Creatures.isCreature(player)) {
			return;
		}
		Vec3 flow = currentAt(level, player.getX(), player.getZ());
		if (flow == null) {
			return;
		}
		player.setDeltaMovement(player.getDeltaMovement().add(flow.scale(0.035)));
		player.hurtMarked = true;
		if (player.tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, player.getX() - flow.x * 2, player.getY() + 0.3, player.getZ() - flow.z * 2, 3, 0.6, 0.4, 0.6, 0.02);
		}
	}

	private static Vec3 currentAt(ServerLevel level, double x, double z) {
		long cellX = Math.floorDiv((long) x, 48L);
		long cellZ = Math.floorDiv((long) z, 48L);
		long epoch = level.getGameTime() / 12000L;
		long h = cellX * 341873128712L + cellZ * 132897987541L + epoch * 7919L + level.getSeed();
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		if (Math.floorMod(h, 4L) != 0) {
			return null;
		}
		double angle = Math.floorMod(h >>> 8, 360L) * Math.PI / 180.0;
		return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
	}

	// ------------------------------------------------------------------ plâncton

	private static void spawnPlankton(ServerLevel level, ServerPlayer near, long now) {
		if (CLOUDS.size() >= 40 || level.getRandom().nextFloat() > (level.isDarkOutside() ? 0.5F : 0.15F)) {
			return;
		}
		double x = near.getX() + (level.getRandom().nextDouble() - 0.5) * 36;
		double z = near.getZ() + (level.getRandom().nextDouble() - 0.5) * 36;
		double y = near.getY() + (level.getRandom().nextDouble() - 0.5) * 8;
		BlockPos pos = BlockPos.containing(x, y, z);
		if (level.getFluidState(pos).is(FluidTags.WATER)) {
			CLOUDS.add(new Cloud(new Vec3(x, y, z), now + 3600));
		}
	}

	private static void tickClouds(ServerLevel level, List<ServerPlayer> swimmers, long now) {
		Iterator<Cloud> it = CLOUDS.iterator();
		while (it.hasNext()) {
			Cloud cloud = it.next();
			if (now > cloud.expires || cloud.bites <= 0) {
				it.remove();
				continue;
			}
			if (now % 8 == 0) {
				level.sendParticles(ParticleTypes.GLOW, cloud.pos.x, cloud.pos.y, cloud.pos.z, 4, 0.8, 0.5, 0.8, 0.0);
			}
			for (ServerPlayer player : swimmers) {
				if (player.position().distanceToSqr(cloud.pos) < 2.8 * 2.8 && now % 15 == 0) {
					Diet diet = Creatures.diet(player);
					if (diet.eatsPlants()) {
						cloud.bites--;
						CreatureService.addDna(player, 2);
						CreatureService.addXp(player, 1);
						com.primordial.creature.FeedingService.banquet(player);
						CreatureService.playSound(player, net.minecraft.sounds.SoundEvents.DOLPHIN_EAT, 0.3F, 1.8F);
						level.sendParticles(ParticleTypes.COMPOSTER, cloud.pos.x, cloud.pos.y, cloud.pos.z, 4, 0.4, 0.3, 0.4, 0.0);
						break;
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ eventos

	private static void tickEvents(ServerLevel level, List<ServerPlayer> swimmers, long now) {
		PrimordialConfig config = PrimordialConfig.get();
		if (!config.oceanEvents) {
			return;
		}
		if (nextEvent < 0) {
			nextEvent = now + 20L * 60 * 3;
		}
		if (now < nextEvent) {
			return;
		}
		int span = Math.max(1, config.eventMaxMinutes - config.eventMinMinutes);
		nextEvent = now + 20L * 60 * (config.eventMinMinutes + level.getRandom().nextInt(span + 1));
		ServerPlayer target = swimmers.get(level.getRandom().nextInt(swimmers.size()));
		switch (level.getRandom().nextInt(3)) {
			case 0 -> spawning(level, target);
			case 1 -> frenzy(level, target, now);
			default -> redTide(level, target, now);
		}
	}

	/** Desova em Massa: chuva de peixinhos fáceis de caçar. Corrida entre os amigos. */
	private static void spawning(ServerLevel level, ServerPlayer target) {
		announce(level, "event.primordial.spawning", ChatFormatting.AQUA);
		for (int i = 0; i < 24; i++) {
			spawnNear(level, target, "sardinha_prata", 14);
		}
		for (int i = 0; i < 6; i++) {
			spawnNear(level, target, "peixe_papagaio", 14);
		}
	}

	/** Frenesi Alimentar: predadores chegam e abates valem o dobro de DNA por 2 minutos. */
	private static void frenzy(ServerLevel level, ServerPlayer target, long now) {
		announce(level, "event.primordial.frenzy", ChatFormatting.RED);
		frenzyUntil = now + 20L * 120;
		for (int i = 0; i < 3; i++) {
			spawnNear(level, target, "enguia_eletrica", 16);
		}
		spawnNear(level, target, "tubarao_martelo", 18);
		for (int i = 0; i < 10; i++) {
			spawnNear(level, target, "atum_flecha", 12);
		}
		level.sendParticles(new DustParticleOptions(0xAA0000, 3.0F), target.getX(), target.getY(), target.getZ(), 80, 4, 2, 4, 0.0);
	}

	/** Maré Vermelha: uma mancha de alga tóxica que envenena quem nada nela por 90 s. */
	private static void redTide(ServerLevel level, ServerPlayer target, long now) {
		announce(level, "event.primordial.red_tide", ChatFormatting.DARK_RED);
		tideCenter = target.blockPosition().offset(level.getRandom().nextInt(31) - 15, 0, level.getRandom().nextInt(31) - 15);
		tideUntil = now + 20L * 90;
	}

	private static void tickTide(ServerLevel level, long now) {
		if (tideCenter == null) {
			return;
		}
		if (now > tideUntil) {
			tideCenter = null;
			return;
		}
		if (now % 5 == 0) {
			level.sendParticles(new DustParticleOptions(0xB01010, 2.5F), tideCenter.getX() + 0.5, tideCenter.getY() + 0.5, tideCenter.getZ() + 0.5, 30, 10, 5, 10, 0.0);
		}
		if (now % 40 == 0) {
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(tideCenter).inflate(14, 8, 14), e -> e.isInWater())) {
				if (!(entity instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()))) {
					entity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
				}
			}
		}
	}

	private static void announce(ServerLevel level, String key, ChatFormatting color) {
		level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable(key).withStyle(color, ChatFormatting.BOLD), false);
	}

	private static void spawnNear(ServerLevel level, ServerPlayer player, String creature, int radius) {
		EntityType<?> type = CreatureRegistry.types().get(Primordial.id(creature));
		if (type == null) {
			return;
		}
		for (int attempt = 0; attempt < 8; attempt++) {
			double x = player.getX() + (level.getRandom().nextDouble() - 0.5) * 2 * radius;
			double z = player.getZ() + (level.getRandom().nextDouble() - 0.5) * 2 * radius;
			double y = player.getY() + (level.getRandom().nextDouble() - 0.5) * 6;
			if (!level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) {
				continue;
			}
			Entity entity = type.create(level, EntitySpawnReason.EVENT);
			if (entity != null) {
				entity.setPos(x, y, z);
				level.addFreshEntity(entity);
			}
			return;
		}
	}
}
