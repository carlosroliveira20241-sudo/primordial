package com.primordial.event;

import com.primordial.item.PrimordialItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/** Os baús dos naufrágios e das ruínas submersas ganham Cápsulas de DNA no lugar de tesouros comuns. */
public final class LootInjection {
	private LootInjection() {
	}

	public static void init() {
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (key.equals(BuiltInLootTables.SHIPWRECK_TREASURE) || key.equals(BuiltInLootTables.SHIPWRECK_SUPPLY)
				|| key.equals(BuiltInLootTables.UNDERWATER_RUIN_BIG) || key.equals(BuiltInLootTables.UNDERWATER_RUIN_SMALL)
				|| key.equals(BuiltInLootTables.BURIED_TREASURE)) {
				builder.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
					.when(LootItemRandomChanceCondition.randomChance(0.7F))
					.add(LootItem.lootTableItem(PrimordialItems.CAPSULA)));
			}
		});
	}
}
