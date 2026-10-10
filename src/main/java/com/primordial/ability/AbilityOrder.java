package com.primordial.ability;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.resources.Identifier;

/** A ordem escolhida pelo jogador para as habilidades: a primeira vai na tecla Z, a segunda no X. */
public record AbilityOrder(List<Identifier> order) {
	public static final AbilityOrder DEFAULT = new AbilityOrder(List.of());
	public static final Codec<AbilityOrder> CODEC = Identifier.CODEC.listOf().xmap(AbilityOrder::new, AbilityOrder::order);
}
