package com.primordial.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** Uma parte equipada, com o nível (1 a 3). */
public record PartRef(Identifier id, int level) {
	public static final Codec<PartRef> CODEC = RecordCodecBuilder.create(i -> i.group(
		Identifier.CODEC.fieldOf("id").forGetter(PartRef::id),
		Codec.INT.optionalFieldOf("level", 1).forGetter(PartRef::level)
	).apply(i, PartRef::new));

	public boolean awakened() {
		return this.level >= 3;
	}
}
