package com.primordial.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.level.Level;

/** Comportamento "cod": peixe de cardume (sardinha, atum). Aparência, vida e spawn vêm do JSON. */
public class Cardume extends Cod {
	public Cardume(EntityType<? extends Cod> type, Level level) {
		super(type, level);
	}
}
