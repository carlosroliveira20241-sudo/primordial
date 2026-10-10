package com.primordial.mutation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.primordial.part.PartModifier;
import com.primordial.part.PartRarity;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Uma mutação, lida de {@code data/primordial/mutations/<nome>.json}. Costuma ter um lado bom e um ruim (dois
 * modificadores). Nome e descrição: {@code mutation.primordial.<nome>} e {@code .desc} no idioma.
 */
public record Mutation(PartRarity rarity, List<PartModifier> modifiers) {
	public static final Codec<Mutation> CODEC = RecordCodecBuilder.create(i -> i.group(
		PartRarity.CODEC.optionalFieldOf("rarity", PartRarity.COMMON).forGetter(Mutation::rarity),
		PartModifier.CODEC.listOf().fieldOf("modifiers").forGetter(Mutation::modifiers)
	).apply(i, Mutation::new));

	public static Component name(Identifier id) {
		return Component.translatable("mutation." + id.getNamespace() + "." + id.getPath());
	}

	public static Component description(Identifier id) {
		return Component.translatable("mutation." + id.getNamespace() + "." + id.getPath() + ".desc");
	}
}
