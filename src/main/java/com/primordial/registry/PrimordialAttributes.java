package com.primordial.registry;

import com.primordial.Primordial;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Atributos próprios do mod. Ficam no jogador e são sincronizados com o cliente (o movimento roda lá). */
public final class PrimordialAttributes {
	/** Multiplicador da velocidade de nado da criatura (1.0 = normal). */
	public static final Holder<Attribute> SWIM_SPEED = register("swim_speed",
		new RangedAttribute("attribute.name.primordial.swim_speed", 1.0, 0.0, 10.0).setSyncable(true));

	/** Dano extra de choque em cada mordida (Órgão Elétrico). */
	public static final Holder<Attribute> SHOCK_DAMAGE = register("shock_damage",
		new RangedAttribute("attribute.name.primordial.shock_damage", 0.0, 0.0, 100.0).setSyncable(true));

	/** Dano devolvido a quem te acerta corpo a corpo (Espinhos). */
	public static final Holder<Attribute> THORNS = register("thorns",
		new RangedAttribute("attribute.name.primordial.thorns", 0.0, 0.0, 100.0).setSyncable(true));

	/** Fração da vida roubada a cada mordida. */
	public static final Holder<Attribute> LIFESTEAL = register("lifesteal",
		new RangedAttribute("attribute.name.primordial.lifesteal", 0.0, 0.0, 1.0).setSyncable(true));

	private PrimordialAttributes() {
	}

	private static Holder<Attribute> register(String name, Attribute attribute) {
		return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, Primordial.id(name), attribute);
	}

	public static void init() {
		FabricDefaultAttributeRegistry.MODIFY.register(context -> context.modify(EntityTypes.PLAYER, (type, builder) -> {
			builder.add(SWIM_SPEED);
			builder.add(SHOCK_DAMAGE);
			builder.add(THORNS);
			builder.add(LIFESTEAL);
		}));
	}
}
