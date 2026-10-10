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
 * O oceano acontecendo sozinho: nuvens de plâncton que dão DNA a
 * herbívoros e eventos aleatórios (Desova em Massa, Frenesi Alimentar, Maré Vermelha).
 */
public final class OceanLife {
	/** Durante o Frenesi Alimentar, abates dão o dobro de DNA (lido por MorselDrops). */
	public static long frenzyUntil;

	private static final List<Cloud> CLOUDS = new ArrayList<>();
	private static final List<Cloud> CARCASSES = new ArrayList<>();
	private static long stormUntil;

	/** Uma criatura grande morreu: o corpo atrai predadores por 90 segundos. */
	public static void addCarcass(net.minecraft.world.entity.LivingEntity dead) {
		if (!(dead.level() instanceof ServerLevel) || CARCASSES.size() > 30 || dead.getMaxHealth() < 8.0F
			|| com.primordial.entity.CreatureRegistry.of(dead.getType()) == null || com.primordial.entity.CreatureRegistry.of(dead.getType()).has("undead")) {
			return;
		}
		CARCASSES.add(new Cloud(dead.position(), dead.level().getGameTime() + 1800));
	}

	/** Para a IA dos predadores: a carcaça mais próxima, ou nulo. */
	public static Vec3 nearestCarcass(ServerLevel level, Vec3 from, double range) {
		Vec3 best = null;
		double bestDist = range * range;
		for (Cloud c : CARCASSES) {
			double d = c.pos.distanceToSqr(from);
			if (d < bestDist && level.getGameTime() < c.expires) {
				best = c.pos;
				bestDist = d;
			}
		}
		return best;
	}
	private static long nextEvent = -1;
	private static final java.util.Set<java.util.UUID> DANGER_SENT = new java.util.HashSet<>();
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
		CARCASSES.removeIf(c -> now > c.expires);
		if (now % 20 == 0) {
			for (Cloud carcass : CARCASSES) {
				level.sendParticles(new DustParticleOptions(0xAA1111, 1.5F), carcass.pos.x, carcass.pos.y + 0.3, carcass.pos.z, 8, 0.5, 0.4, 0.5, 0.0);
			}
		}
		tickStorm(level, swimmers, now);
		if (now % 10 == 0) {
			sendDanger(level, swimmers);
		}
		tickTide(level, now);
		tickEvents(level, swimmers, now);
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
						com.primordial.creature.FeedingService.drop(level, cloud.pos.x, cloud.pos.y, cloud.pos.z,
							new net.minecraft.world.item.ItemStack(com.primordial.item.PrimordialItems.PLANCTON));
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
		switch (level.getRandom().nextInt(6)) {
			case 0 -> spawning(level, target);
			case 1 -> frenzy(level, target, now);
			case 2 -> redTide(level, target, now);
			case 3 -> leviathan(level, target);
			case 4 -> whale(level, target);
			default -> storm(level, now);
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

	/** Aparição do Leviatã: um chefe-mundo emerge por 10 minutos. Quem der o último golpe leva a Coroa lendária. */
	private static void leviathan(ServerLevel level, ServerPlayer target) {
		EntityType<?> type = CreatureRegistry.types().get(Primordial.id("leviata_ancestral"));
		if (type == null || !level.getEntities(type, e -> true).isEmpty()) {
			return;
		}
		announce(level, "event.primordial.leviathan", ChatFormatting.DARK_PURPLE);
		spawnNear(level, target, "leviata_ancestral", 28);
	}

	/** Migração da Baleia-Colosso: uma baleia gigante passa; nadar na esteira dela dá velocidade e vida. */
	private static void whale(ServerLevel level, ServerPlayer target) {
		announce(level, "event.primordial.whale", ChatFormatting.AQUA);
		EntityType<?> type = CreatureRegistry.types().get(Primordial.id("baleia_colosso"));
		if (type == null) {
			return;
		}
		double ang = level.getRandom().nextDouble() * Math.PI * 2;
		double x = target.getX() + Math.cos(ang) * 40;
		double z = target.getZ() + Math.sin(ang) * 40;
		Entity whale = type.create(level, EntitySpawnReason.EVENT);
		if (whale != null) {
			whale.setPos(x, target.getY() + 2, z);
			whale.setYRot((float) (Math.toDegrees(Math.atan2(-(target.getX() - x), target.getZ() - z))));
			level.addFreshEntity(whale);
			for (int i = 0; i < 6; i++) {
				spawnNearPoint(level, "parasita", x, whale.getY(), z, 5);
			}
		}
	}

	/** Tempestade: raios caem na superfície por 90 segundos e as criaturas elétricas ficam mais fortes. */
	private static void storm(ServerLevel level, long now) {
		announce(level, "event.primordial.storm", ChatFormatting.YELLOW);
		stormUntil = now + 20L * 90;
	}

	private static void tickStorm(ServerLevel level, List<ServerPlayer> swimmers, long now) {
		if (now >= stormUntil) {
			return;
		}
		if (now % 40 == 0) {
			ServerPlayer near = swimmers.get(level.getRandom().nextInt(swimmers.size()));
			double x = near.getX() + (level.getRandom().nextDouble() - 0.5) * 36;
			double z = near.getZ() + (level.getRandom().nextDouble() - 0.5) * 36;
			net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
			if (bolt != null) {
				bolt.setPos(x, level.getSeaLevel(), z);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
			for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(x - 7, level.getSeaLevel() - 10, z - 7, x + 7, level.getSeaLevel() + 2, z + 7), e -> e.isInWater())) {
				if (!(hit instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()))) {
					hit.hurtServer(level, level.damageSources().lightningBolt(), 4.0F);
				}
			}
		}
		if (now % 100 == 0) {
			for (ServerPlayer p : swimmers) {
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(40.0))) {
					var def = com.primordial.entity.CreatureRegistry.of(e.getType());
					if (def != null && (def.has("shock_hit") || def.has("shock_touch"))) {
						e.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 140, 1));
					}
				}
			}
		}
	}

	/** Seta de perigo: avisa o jogador de onde vêm os predadores que estão atrás dele. */
	private static void sendDanger(ServerLevel level, List<ServerPlayer> swimmers) {
		for (ServerPlayer player : swimmers) {
			List<BlockPos> hunters = new ArrayList<>();
			for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(40.0), m -> m.getTarget() == player)) {
				if (hunters.size() < 3) {
					hunters.add(mob.blockPosition());
				}
			}
			boolean had = DANGER_SENT.contains(player.getUUID());
			if (!hunters.isEmpty() || had) {
				net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new com.primordial.net.Payloads.Danger(hunters));
				if (hunters.isEmpty()) {
					DANGER_SENT.remove(player.getUUID());
				} else {
					DANGER_SENT.add(player.getUUID());
				}
			}
		}
	}

	private static void spawnNearPoint(ServerLevel level, String creature, double x, double y, double z, int radius) {
		EntityType<?> type = CreatureRegistry.types().get(Primordial.id(creature));
		if (type == null) {
			return;
		}
		Entity entity = type.create(level, EntitySpawnReason.EVENT);
		if (entity != null) {
			entity.setPos(x + (level.getRandom().nextDouble() - 0.5) * 2 * radius, y, z + (level.getRandom().nextDouble() - 0.5) * 2 * radius);
			level.addFreshEntity(entity);
		}
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
