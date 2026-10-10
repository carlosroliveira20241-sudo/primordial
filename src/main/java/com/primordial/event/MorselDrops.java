package com.primordial.event;

import com.primordial.config.PrimordialConfig;
import com.primordial.creature.CreatureProgress;
import com.primordial.creature.CreatureService;
import com.primordial.creature.Creatures;
import com.primordial.entity.CreatureDef;
import com.primordial.entity.CreatureRegistry;
import com.primordial.item.PrimordialItems;
import com.primordial.part.PartRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

/** Abate de uma criatura do mod: dá DNA/XP e talvez derruba a Melhoria dela. */
public final class MorselDrops {
	/** Marca das criaturas Alfa: raras, mais fortes e sempre dropam a Melhoria. */
	public static final String ALPHA_TAG = "primordial_alpha";

	private MorselDrops() {
	}

	public static void onKill(ServerPlayer killer, LivingEntity victim) {
		Identifier species = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
		if (CreatureRegistry.isBig(victim.getType())) {
			CreatureProgress before = Creatures.progress(killer);
			CreatureService.setProgress(killer, before.withBigKills(before.bigKills() + 1));
		}
		CreatureDef def = CreatureRegistry.get(species);
		if (def != null && def.has("undead")) {
			// Zumbis dão um pouco de experiência, mas nunca uma Melhoria.
			CreatureService.addDna(killer, 1);
			CreatureService.addXp(killer, 5);
			return;
		}
		boolean alpha = victim.entityTags().contains(ALPHA_TAG);
		boolean treasure = def != null && def.has("treasure");
		List<Identifier> candidates = PartRegistry.droppedBy(species);
		if (treasure) {
			// Concha Gigante: sempre solta uma Melhoria de qualquer criatura, mais DNA e XP.
			CreatureService.addDna(killer, 15);
			CreatureService.addXp(killer, 20);
			candidates = new ArrayList<>(PartRegistry.droppable());
		}
		if (candidates.isEmpty()) {
			return;
		}
		if (!treasure) {
			CreatureService.addDna(killer, (alpha ? 8 : 3) * (com.primordial.world.OceanLife.frenzyActive(killer.level()) ? 2 : 1));
			CreatureService.addXp(killer, alpha ? 25 : 8);
		}

		PrimordialConfig config = PrimordialConfig.get();
		CreatureProgress progress = Creatures.progress(killer);
		int misses = progress.pity().getOrDefault(species, 0);
		if (!treasure && !alpha && misses + 1 < config.pityKills && killer.getRandom().nextDouble() >= config.morselDropChance) {
			CreatureService.setProgress(killer, progress.withPity(species, misses + 1));
			return;
		}
		CreatureService.setProgress(killer, progress.withPity(species, 0));

		// Prefere uma parte que o jogador ainda não tem.
		List<Identifier> fresh = new ArrayList<>();
		for (Identifier part : candidates) {
			if (!progress.unlocked(part)) {
				fresh.add(part);
			}
		}
		List<Identifier> pool = fresh.isEmpty() ? candidates : fresh;
		Identifier part = pool.get(killer.getRandom().nextInt(pool.size()));

		spawnMorsel(killer.level(), victim.getX(), victim.getY() + 0.4, victim.getZ(), part);
	}

	/** Solta uma Melhoria parada, brilhando e com o nome da parte em cima. */
	public static void spawnMorsel(ServerLevel level, double x, double y, double z, Identifier part) {
		ItemEntity drop = new ItemEntity(level, x, y, z, PrimordialItems.morsel(part));
		drop.setNoGravity(true);
		drop.setGlowingTag(true);
		drop.setCustomName(com.primordial.part.PartDefinition.name(part));
		drop.setCustomNameVisible(true);
		drop.setNeverPickUp();
		drop.setDeltaMovement(0, 0, 0);
		level.addFreshEntity(drop);
	}
}
