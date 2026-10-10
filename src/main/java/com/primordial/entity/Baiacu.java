package com.primordial.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.level.Level;

public class Baiacu extends Pufferfish {
	public Baiacu(EntityType<? extends Pufferfish> type, Level level) {
		super(type, level);
	}
}
