package com.primordial.creature;

import com.primordial.Primordial;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartModifier;
import com.primordial.part.PartRegistry;
import com.primordial.registry.PrimordialAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Transforma estágio + origem + partes em modificadores de atributos vanilla.
 * Os modificadores são transitórios: são recalculados ao entrar, renascer e mudar o corpo.
 */
public final class CreatureStats {
	private static final String STAGE = "stage/";
	private static final String ORIGIN = "origin/";
	private static final String PART = "part/";
	private static final String MUTATION = "mutation/";
	private static final String SYNERGY = "synergy/";

	/** Atributos que o estágio mexe (além dos que as partes declaram). */
	private static final List<Holder<Attribute>> STAGE_ATTRIBUTES = List.of(
		Attributes.SCALE, Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE,
		Attributes.ENTITY_INTERACTION_RANGE, Attributes.BLOCK_INTERACTION_RANGE, PrimordialAttributes.SWIM_SPEED,
		Attributes.WATER_MOVEMENT_EFFICIENCY
	);

	private CreatureStats() {
	}

	public static void apply(ServerPlayer player) {
		CreatureBody body = Creatures.body(player);
		clear(player);
		if (!body.active()) {
			return;
		}

		Stage stage = body.stage();
		add(player, Attributes.SCALE, STAGE + "scale", stage.scale - 1.0, AttributeModifier.Operation.ADD_VALUE);
		add(player, Attributes.MAX_HEALTH, STAGE + "max_health", stage.maxHealth - 20.0, AttributeModifier.Operation.ADD_VALUE);
		add(player, Attributes.ATTACK_DAMAGE, STAGE + "attack_damage", stage.attackDamage - 1.0, AttributeModifier.Operation.ADD_VALUE);
		// Alcance proporcional ao tamanho (um peixinho não morde a 3 blocos de distância).
		add(player, Attributes.ENTITY_INTERACTION_RANGE, STAGE + "entity_range", Math.max(1.6, 3.0 * stage.scale + 0.6) - 3.0, AttributeModifier.Operation.ADD_VALUE);
		add(player, Attributes.BLOCK_INTERACTION_RANGE, STAGE + "block_range", Math.max(2.0, 4.5 * stage.scale + 0.5) - 4.5, AttributeModifier.Operation.ADD_VALUE);

		switch (Creatures.progress(player).origin()) {
			case FILTRADORA -> add(player, Attributes.MAX_HEALTH, ORIGIN + "health", 4.0, AttributeModifier.Operation.ADD_VALUE);
			case MORDEDORA -> add(player, Attributes.ATTACK_DAMAGE, ORIGIN + "attack", 1.0, AttributeModifier.Operation.ADD_VALUE);
			case NECROFAGA -> add(player, PrimordialAttributes.SWIM_SPEED, ORIGIN + "swim", 0.1, AttributeModifier.Operation.ADD_VALUE);
		}

		for (Map.Entry<BodySlot, PartRef> entry : body.parts().entrySet()) {
			PartDefinition def = PartRegistry.get(entry.getValue().id());
			if (def == null) {
				continue;
			}
			int index = 0;
			for (PartModifier modifier : def.modifiers()) {
				add(player, modifier.attribute(), PART + entry.getKey().getSerializedName() + "/" + index++,
					modifier.amountAt(entry.getValue().level()), modifier.operation());
			}
		}

		for (Identifier chosen : com.primordial.mutation.MutationService.state(player).chosen()) {
			com.primordial.mutation.Mutation mutation = com.primordial.mutation.MutationRegistry.get(chosen);
			if (mutation == null) {
				continue;
			}
			int index = 0;
			for (PartModifier modifier : mutation.modifiers()) {
				add(player, modifier.attribute(), MUTATION + chosen.getPath() + "/" + index++, modifier.amountAt(1), modifier.operation());
			}
		}

		for (Identifier active : com.primordial.synergy.SynergyRegistry.active(body)) {
			int index = 0;
			for (PartModifier modifier : com.primordial.synergy.SynergyRegistry.all().get(active).modifiers()) {
				add(player, modifier.attribute(), SYNERGY + active.getPath() + "/" + index++, modifier.amountAt(1), modifier.operation());
			}
		}

		if (stage.aquatic) {
			add(player, Attributes.WATER_MOVEMENT_EFFICIENCY, STAGE + "water", 1.0, AttributeModifier.Operation.ADD_VALUE);
		}
		add(player, Attributes.MOVEMENT_SPEED, STAGE + "swim", player.getAttributeValue(PrimordialAttributes.SWIM_SPEED) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void add(ServerPlayer player, Holder<Attribute> attribute, String path, double amount, AttributeModifier.Operation op) {
		if (amount == 0.0) {
			return;
		}
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(Primordial.id(path), amount, op));
		}
	}

	/** Remove todos os modificadores de estágio/origem/parte do mod. */
	public static void clear(ServerPlayer player) {
		Set<Holder<Attribute>> touched = new LinkedHashSet<>(STAGE_ATTRIBUTES);
		touched.add(Attributes.MOVEMENT_SPEED);
		for (PartDefinition def : PartRegistry.all().values()) {
			for (PartModifier modifier : def.modifiers()) {
				touched.add(modifier.attribute());
			}
		}
		for (com.primordial.mutation.Mutation mutation : com.primordial.mutation.MutationRegistry.all().values()) {
			for (PartModifier modifier : mutation.modifiers()) {
				touched.add(modifier.attribute());
			}
		}
		for (com.primordial.synergy.Synergy synergy : com.primordial.synergy.SynergyRegistry.all().values()) {
			for (PartModifier modifier : synergy.modifiers()) {
				touched.add(modifier.attribute());
			}
		}
		for (Holder<Attribute> attribute : touched) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance == null) {
				continue;
			}
			List<Identifier> ours = new ArrayList<>();
			for (AttributeModifier modifier : instance.getModifiers()) {
				Identifier id = modifier.id();
				if (id.getNamespace().equals(Primordial.MOD_ID)
					&& (id.getPath().startsWith(STAGE) || id.getPath().startsWith(ORIGIN) || id.getPath().startsWith(PART) || id.getPath().startsWith(MUTATION) || id.getPath().startsWith(SYNERGY))) {
					ours.add(id);
				}
			}
			ours.forEach(instance::removeModifier);
		}
	}
}
