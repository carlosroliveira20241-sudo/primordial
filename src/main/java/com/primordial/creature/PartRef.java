package com.primordial.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/** Uma parte equipada: o nível (1 a 3) e o estilo visual escolhido (0 = o padrão; só muda a aparência). */
public record PartRef(Identifier id, int level, int style) {
	public static final Codec<PartRef> CODEC = RecordCodecBuilder.create(i -> i.group(
		Identifier.CODEC.fieldOf("id").forGetter(PartRef::id),
		Codec.INT.optionalFieldOf("level", 1).forGetter(PartRef::level),
		Codec.INT.optionalFieldOf("style", 0).forGetter(PartRef::style)
	).apply(i, PartRef::new));

	public PartRef(Identifier id, int level) {
		this(id, level, 0);
	}

	public boolean awakened() {
		return this.level >= 3;
	}
}
