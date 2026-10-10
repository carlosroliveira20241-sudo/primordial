package com.primordial.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.level.Level;

public class PeixePapagaio extends Salmon {
	public PeixePapagaio(EntityType<? extends Salmon> type, Level level) {
		super(type, level);
	}
}
