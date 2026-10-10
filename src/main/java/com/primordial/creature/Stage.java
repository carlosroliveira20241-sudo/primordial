package com.primordial.creature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.util.StringRepresentable;

import static com.primordial.creature.BodySlot.*;

/**
 * Estágios de evolução. Os números de vida/dano são a base do estágio; partes e mutações somam por cima.
 * {@code available} = já implementado nesta versão do mod (os outros aparecem como "em breve").
 */
public enum Stage implements StringRepresentable {
	LARVA("larva", 0.35F, 8, 1.0, 2, 3, 5, true, true,
		List.of(MOUTH, TAIL, ORGAN_1)),
	PEIXE("peixe", 0.55F, 14, 2.0, 3, 3, 12, true, true,
		List.of(MOUTH, EYES, FINS, BACK, SKIN, TAIL, ORGAN_1)),
	PREDADOR("predador", 0.8F, 22, 3.5, 4, 8, 20, true, false,
		List.of(MOUTH, EYES, HEAD, FINS, BACK, SKIN, TAIL, ORGAN_1, ORGAN_2)),
	ANFIBIO("anfibio", 0.9F, 26, 4.0, 4, 12, 30, true, false,
		List.of(MOUTH, EYES, HEAD, FINS, BACK, SKIN, TAIL, ORGAN_1, ORGAN_2)),
	TERRESTRE("terrestre", 1.0F, 30, 5.0, 5, 30, 45, false, false,
		List.of(MOUTH, EYES, HEAD, FINS, FORELIMBS, BACK, SKIN, TAIL, ORGAN_1, ORGAN_2)),
	APICE("apice", 1.2F, 36, 6.0, 6, 50, Integer.MAX_VALUE, false, false,
		List.of(MOUTH, EYES, HEAD, FINS, FORELIMBS, BACK, SKIN, TAIL, ORGAN_1, ORGAN_2, ORGAN_3, WINGS));

	public static final Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);

	private final String name;
	public final float scale;
	public final double maxHealth;
	public final double attackDamage;
	public final int abilitySlots;
	public final int intelligenceCap;
	/** Nível necessário para evoluir PARA O PRÓXIMO estágio. */
	public final int levelToEvolve;
	/** Respira debaixo d'água e se sufoca fora dela. */
	public final boolean aquatic;
	public final boolean available;
	public final List<BodySlot> slots;

	Stage(String name, float scale, double maxHealth, double attackDamage, int abilitySlots, int intelligenceCap,
		int levelToEvolve, boolean aquatic, boolean available, List<BodySlot> slots) {
		this.name = name;
		this.scale = scale;
		this.maxHealth = maxHealth;
		this.attackDamage = attackDamage;
		this.abilitySlots = abilitySlots;
		this.intelligenceCap = intelligenceCap;
		this.levelToEvolve = levelToEvolve;
		this.aquatic = aquatic;
		this.available = available;
		this.slots = slots;
	}

	public int number() {
		return this.ordinal() + 1;
	}

	public Stage next() {
		Stage[] values = values();
		return this.ordinal() + 1 < values.length ? values[this.ordinal() + 1] : this;
	}

	public boolean hasNext() {
		return this != APICE;
	}

	public String translationKey() {
		return "stage.primordial." + this.name;
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}
}
