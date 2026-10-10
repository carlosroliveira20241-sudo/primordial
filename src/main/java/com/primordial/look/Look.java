package com.primordial.look;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Uma aparência (modelo de corpo), lida de {@code data/primordial/looks/<nome>.json}. É a MESMA coisa que o
 * jogador escolhe para o personagem e que as criaturas usam, então qualquer corpo serve para os dois.
 *
 * @param model      qual modelo base desenhar (hoje: "cod")
 * @param tint       cor padrão (RGB) quando ninguém escolheu outra; no jogador vale a cor primária dele
 * @param scale      proporção x, y, z aplicada em cima do modelo (corpo largo, alto, comprido)
 * @param selectable aparece na tela de aparência do jogador
 */
public record Look(String model, int tint, List<Float> scale, float shadow, boolean selectable) {
	/** Cor escrita como "#RRGGBB" no JSON. */
	public static final Codec<Integer> HEX = Codec.STRING.comapFlatMap(text -> {
		try {
			return DataResult.success(Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16) & 0xFFFFFF);
		} catch (NumberFormatException e) {
			return DataResult.error(() -> "cor inválida: " + text);
		}
	}, color -> String.format("#%06X", color));

	public static final Codec<Look> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.optionalFieldOf("model", "cod").forGetter(Look::model),
		HEX.optionalFieldOf("tint", 0xFFFFFF).forGetter(Look::tint),
		Codec.FLOAT.listOf().optionalFieldOf("scale", List.of(1.0F, 1.0F, 1.0F)).forGetter(Look::scale),
		Codec.FLOAT.optionalFieldOf("shadow", 0.3F).forGetter(Look::shadow),
		Codec.BOOL.optionalFieldOf("selectable", false).forGetter(Look::selectable)
	).apply(i, Look::new));

	public static final Identifier DEFAULT_ID = Identifier.fromNamespaceAndPath("primordial", "peixe_comum");

	public float sx() {
		return this.scale.size() > 0 ? this.scale.get(0) : 1.0F;
	}

	public float sy() {
		return this.scale.size() > 1 ? this.scale.get(1) : 1.0F;
	}

	public float sz() {
		return this.scale.size() > 2 ? this.scale.get(2) : 1.0F;
	}

	public static Component name(Identifier id) {
		return Component.translatable("look." + id.getNamespace() + "." + id.getPath());
	}
}
