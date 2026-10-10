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
		List<Identifier> candidates = PartRegistry.droppedBy(species);
		if (candidates.isEmpty()) {
			return;
		}
		CreatureService.addDna(killer, 3);
		CreatureService.addXp(killer, 8);

		PrimordialConfig config = PrimordialConfig.get();
		CreatureProgress progress = Creatures.progress(killer);
		int misses = progress.pity().getOrDefault(species, 0);
		if (misses + 1 < config.pityKills && killer.getRandom().nextDouble() >= config.morselDropChance) {
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

		ServerLevel level = killer.level();
		ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.4, victim.getZ(), PrimordialItems.morsel(part));
		drop.setNoGravity(true);
		drop.setGlowingTag(true);
		drop.setCustomName(com.primordial.part.PartDefinition.name(part));
		drop.setCustomNameVisible(true);
		drop.setNeverPickUp();
		drop.setDeltaMovement(0, 0, 0);
		level.addFreshEntity(drop);
	}
}
