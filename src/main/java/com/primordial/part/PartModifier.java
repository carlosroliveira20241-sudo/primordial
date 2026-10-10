package com.primordial.part;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Um bônus de atributo vanilla que a parte dá: {@code base + perLevel * (nível - 1)}. */
public record PartModifier(Holder<Attribute> attribute, double base, double perLevel, AttributeModifier.Operation operation) {
	public static final Codec<PartModifier> CODEC = RecordCodecBuilder.create(i -> i.group(
		BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(PartModifier::attribute),
		Codec.DOUBLE.fieldOf("base").forGetter(PartModifier::base),
		Codec.DOUBLE.optionalFieldOf("per_level", 0.0).forGetter(PartModifier::perLevel),
		AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(PartModifier::operation)
	).apply(i, PartModifier::new));

	public double amountAt(int level) {
		return this.base + this.perLevel * Math.max(0, level - 1);
	}
}
