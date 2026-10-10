package com.primordial;

import com.primordial.ability.Abilities;
import com.primordial.config.PrimordialConfig;
import com.primordial.entity.PrimordialEntities;
import com.primordial.event.PrimordialEvents;
import com.primordial.item.PrimordialItems;
import com.primordial.net.PrimordialNetwork;
import com.primordial.entity.CreatureRegistry;
import com.primordial.look.LookRegistry;
import com.primordial.part.PartRegistry;
import com.primordial.registry.PrimordialAttachments;
import com.primordial.registry.PrimordialAttributes;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Primordial implements ModInitializer {
	public static final String MOD_ID = "primordial";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PrimordialConfig.load();
		PrimordialAttributes.init();
		PartRegistry.load();
		LookRegistry.load();
		com.primordial.mutation.MutationRegistry.load();
		com.primordial.synergy.SynergyRegistry.load();
		com.primordial.journal.ChallengeRegistry.load();
		CreatureRegistry.load();
		PrimordialAttachments.init();
		PrimordialItems.init();
		PrimordialEntities.init();
		Abilities.init();
		PrimordialNetwork.init();
		PrimordialEvents.init();
		LOGGER.info("Primordial carregado: o oceano primordial te espera.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
