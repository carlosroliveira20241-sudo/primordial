package com.primordial.item;

import com.primordial.Primordial;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;

public final class PrimordialItems {
	/** Qual parte do corpo esta Melhoria desbloqueia. */
	public static final DataComponentType<Identifier> PART = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Primordial.id("part"),
		DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build());

	public static final Item MORSEL = register("melhoria");
	/** Comidas: a criatura só ganha DNA comendo o que dropa de plantas e de bichos. */
	public static final Item ALGA = register("alga_marinha");
	public static final Item CARNE = register("carne_marinha");
	public static final Item PLANCTON = register("plancton");
	/** Ferramenta feita de coral (3 corais na grade de criação): quebra pedra como uma picareta de pedra. */
	public static final Item CORAL_TOOL = registerTool("ferramenta_de_coral");

	private PrimordialItems() {
	}

	private static Item register(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Primordial.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}

	private static Item registerTool(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Primordial.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).pickaxe(ToolMaterial.STONE, 1.0F, -2.8F)));
	}

	public static ItemStack morsel(Identifier part) {
		ItemStack stack = new ItemStack(MORSEL);
		stack.set(PART, part);
		stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.primordial.melhoria", PartDefinition.name(part)));
		PartDefinition def = PartRegistry.get(part);
		if (def != null) {
			stack.set(DataComponents.RARITY, switch (def.rarity()) {
				case COMMON -> Rarity.COMMON;
				case RARE -> Rarity.RARE;
				default -> Rarity.EPIC;
			});
		}
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			PartDefinition.description(part).copy().withStyle(ChatFormatting.GRAY),
			Component.translatable("item.primordial.melhoria.hint").withStyle(ChatFormatting.YELLOW))));
		return stack;
	}

	public static void init() {
	}
}
