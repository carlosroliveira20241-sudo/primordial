package com.primordial.part;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.primordial.creature.Diet;
import com.primordial.creature.SlotType;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Uma parte do corpo, lida de {@code data/primordial/parts/<nome>.json}.
 * Nome e descrição vêm do arquivo de idioma: {@code part.primordial.<nome>} e {@code part.primordial.<nome>.desc}.
 */
public record PartDefinition(
	SlotType slot,
	int minStage,
	int cost,
	PartRarity rarity,
	Optional<Identifier> source,
	Optional<Diet> diet,
	Optional<Identifier> ability,
	List<String> families,
	List<PartModifier> modifiers,
	List<String> passives,
	boolean base,
	Optional<String> model,
	Optional<Visual> visual
) {
	public static final Codec<PartDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
		SlotType.CODEC.fieldOf("slot").forGetter(PartDefinition::slot),
		Codec.INT.optionalFieldOf("min_stage", 1).forGetter(PartDefinition::minStage),
		Codec.INT.optionalFieldOf("cost", 0).forGetter(PartDefinition::cost),
		PartRarity.CODEC.optionalFieldOf("rarity", PartRarity.COMMON).forGetter(PartDefinition::rarity),
		Identifier.CODEC.optionalFieldOf("source").forGetter(PartDefinition::source),
		Diet.CODEC.optionalFieldOf("diet").forGetter(PartDefinition::diet),
		Identifier.CODEC.optionalFieldOf("ability").forGetter(PartDefinition::ability),
		Codec.STRING.listOf().optionalFieldOf("families", List.of()).forGetter(PartDefinition::families),
		PartModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(PartDefinition::modifiers),
		Codec.STRING.listOf().optionalFieldOf("passives", List.of()).forGetter(PartDefinition::passives),
		Codec.BOOL.optionalFieldOf("base", false).forGetter(PartDefinition::base),
		Codec.STRING.optionalFieldOf("model").forGetter(PartDefinition::model),
		Visual.CODEC.optionalFieldOf("visual").forGetter(PartDefinition::visual)
	).apply(i, PartDefinition::new));

	/**
	 * Como a parte muda o corpo desenhado: {@code scale} multiplica as proporções (x, y, z) e {@code tint} puxa um
	 * pouco a cor do corpo em direção a essa cor. Tudo opcional; sem isso a parte não muda o visual.
	 */
	public record Visual(List<Float> scale, Optional<Integer> tint, List<String> features) {
		public static final Codec<Visual> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.FLOAT.listOf().optionalFieldOf("scale", List.of(1.0F, 1.0F, 1.0F)).forGetter(Visual::scale),
			com.primordial.look.Look.HEX.optionalFieldOf("tint").forGetter(Visual::tint),
			Codec.STRING.listOf().optionalFieldOf("features", List.of()).forGetter(Visual::features)
		).apply(i, Visual::new));
	}

	public boolean hasPassive(String passive) {
		return this.passives.contains(passive);
	}

	public static String translationKey(Identifier id) {
		return "part." + id.getNamespace() + "." + id.getPath();
	}

	public static Component name(Identifier id) {
		return Component.translatable(translationKey(id));
	}

	public static Component description(Identifier id) {
		return Component.translatable(translationKey(id) + ".desc");
	}
}
