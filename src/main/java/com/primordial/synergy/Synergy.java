package com.primordial.synergy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.primordial.part.PartModifier;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Sinergia de família, lida de {@code data/primordial/synergies/<nome>.json}: equipar {@code count} partes ou mais da
 * mesma família ({@code "families"} nas partes) dá bônus de atributos e um título. Nome: {@code synergy.primordial.<nome>}.
 */
public record Synergy(String family, int count, List<PartModifier> modifiers) {
	public static final Codec<Synergy> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("family").forGetter(Synergy::family),
		Codec.INT.fieldOf("count").forGetter(Synergy::count),
		PartModifier.CODEC.listOf().fieldOf("modifiers").forGetter(Synergy::modifiers)
	).apply(i, Synergy::new));

	public static Component name(Identifier id) {
		return Component.translatable("synergy." + id.getNamespace() + "." + id.getPath());
	}

	public static Component description(Identifier id) {
		return Component.translatable("synergy." + id.getNamespace() + "." + id.getPath() + ".desc");
	}
}
