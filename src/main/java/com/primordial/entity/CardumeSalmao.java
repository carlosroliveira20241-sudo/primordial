package com.primordial.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.level.Level;

/** Comportamento "salmon": cardume que segue o líder, como o salmão (peixe-papagaio). */
public class CardumeSalmao extends Salmon {
	public CardumeSalmao(EntityType<? extends Salmon> type, Level level) {
		super(type, level);
	}
}
