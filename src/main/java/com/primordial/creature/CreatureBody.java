package com.primordial.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.Map;
import com.primordial.look.Look;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * A aparência e o corpo da criatura. É sincronizado com TODOS os jogadores, porque eles precisam
 * desenhar o seu bicho. O que é só seu (DNA, desbloqueios) fica em {@link CreatureProgress}.
 *
 * @param active falso até o jogador escolher a origem; enquanto isso ele aparece como um jogador normal
 */
public record CreatureBody(
	boolean active,
	Stage stage,
	Map<BodySlot, PartRef> parts,
	int primaryColor,
	int secondaryColor,
	int detailColor,
	Pattern pattern,
	String speciesName,
	boolean inflated,
	Identifier look,
	int length,
	int head
) {
	public static final int DEFAULT_PRIMARY = 0x4A90C2;
	public static final int DEFAULT_SECONDARY = 0xF2C14E;
	public static final int DEFAULT_DETAIL = 0xF5F5F5;

	public static final CreatureBody DEFAULT = new CreatureBody(
		false, Stage.LARVA, Map.of(), DEFAULT_PRIMARY, DEFAULT_SECONDARY, DEFAULT_DETAIL, Pattern.LISO, "", false, Look.DEFAULT_ID, 100, 100
	);

	public static final Codec<CreatureBody> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.BOOL.optionalFieldOf("active", false).forGetter(CreatureBody::active),
		Stage.CODEC.optionalFieldOf("stage", Stage.LARVA).forGetter(CreatureBody::stage),
		Codec.unboundedMap(BodySlot.CODEC, PartRef.CODEC).optionalFieldOf("parts", Map.of()).forGetter(CreatureBody::parts),
		Codec.INT.optionalFieldOf("primary", DEFAULT_PRIMARY).forGetter(CreatureBody::primaryColor),
		Codec.INT.optionalFieldOf("secondary", DEFAULT_SECONDARY).forGetter(CreatureBody::secondaryColor),
		Codec.INT.optionalFieldOf("detail", DEFAULT_DETAIL).forGetter(CreatureBody::detailColor),
		Pattern.CODEC.optionalFieldOf("pattern", Pattern.LISO).forGetter(CreatureBody::pattern),
		Codec.STRING.optionalFieldOf("species_name", "").forGetter(CreatureBody::speciesName),
		Codec.BOOL.optionalFieldOf("inflated", false).forGetter(CreatureBody::inflated),
		Identifier.CODEC.optionalFieldOf("look", Look.DEFAULT_ID).forGetter(CreatureBody::look),
		Codec.INT.optionalFieldOf("length", 100).forGetter(CreatureBody::length),
		Codec.INT.optionalFieldOf("head", 100).forGetter(CreatureBody::head)
	).apply(i, CreatureBody::new));

	public CreatureBody {
		parts = parts.isEmpty() ? Map.of() : Map.copyOf(parts);
	}

	public @Nullable PartRef part(BodySlot slot) {
		return this.parts.get(slot);
	}

	public boolean has(Identifier partId) {
		for (PartRef ref : this.parts.values()) {
			if (ref.id().equals(partId)) {
				return true;
			}
		}
		return false;
	}

	public int levelOf(Identifier partId) {
		for (PartRef ref : this.parts.values()) {
			if (ref.id().equals(partId)) {
				return ref.level();
			}
		}
		return 0;
	}

	public CreatureBody withActive(boolean active) {
		return new CreatureBody(active, this.stage, this.parts, this.primaryColor, this.secondaryColor, this.detailColor, this.pattern, this.speciesName, this.inflated, this.look, this.length, this.head);
	}

	public CreatureBody withStage(Stage stage) {
		return new CreatureBody(this.active, stage, this.parts, this.primaryColor, this.secondaryColor, this.detailColor, this.pattern, this.speciesName, this.inflated, this.look, this.length, this.head);
	}

	public CreatureBody withPart(BodySlot slot, @Nullable PartRef ref) {
		Map<BodySlot, PartRef> copy = new EnumMap<>(BodySlot.class);
		copy.putAll(this.parts);
		if (ref == null) {
			copy.remove(slot);
		} else {
			copy.put(slot, ref);
		}
		return new CreatureBody(this.active, this.stage, copy, this.primaryColor, this.secondaryColor, this.detailColor, this.pattern, this.speciesName, this.inflated, this.look, this.length, this.head);
	}

	public CreatureBody withLook(int primary, int secondary, int detail, Pattern pattern) {
		return new CreatureBody(this.active, this.stage, this.parts, primary, secondary, detail, pattern, this.speciesName, this.inflated, this.look, this.length, this.head);
	}

	public CreatureBody withBodyLook(Identifier look) {
		return new CreatureBody(this.active, this.stage, this.parts, this.primaryColor, this.secondaryColor, this.detailColor, this.pattern, this.speciesName, this.inflated, look, this.length, this.head);
	}

	/** Proporções só visuais (percentual): comprimento do corpo e tamanho da cabeça. */
	public CreatureBody withProportions(int length, int head) {
		return new CreatureBody(this.active, this.stage, this.parts, this.primaryColor, this.secondaryColor, this.detailColor, this.pattern, this.speciesName, this.inflated, this.look,
			Math.max(70, Math.min(140, length)), Math.max(70, Math.min(140, head)));
	}

	public CreatureBody withSpeciesName(String name) {
		return new CreatureBody(this.active, this.stage, this.parts, this.primaryColor, this.secondaryColor, this.detailColor, this.pattern, name, this.inflated, this.look, this.length, this.head);
	}

	public CreatureBody withInflated(boolean inflated) {
		return new CreatureBody(this.active, this.stage, this.parts, this.primaryColor, this.secondaryColor, this.detailColor, this.pattern, this.speciesName, inflated, this.look, this.length, this.head);
	}
}
