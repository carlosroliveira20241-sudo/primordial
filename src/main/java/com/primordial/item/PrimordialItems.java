package com.primordial.item;

import com.primordial.Primordial;
import com.primordial.part.PartDefinition;
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

public final class PrimordialItems {
	/** Qual parte do corpo esta Melhoria desbloqueia. */
	public static final DataComponentType<Identifier> PART = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Primordial.id("part"),
		DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build());

	public static final Item MORSEL = register("melhoria");

	private PrimordialItems() {
	}

	private static Item register(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Primordial.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}

	public static ItemStack morsel(Identifier part) {
		ItemStack stack = new ItemStack(MORSEL);
		stack.set(PART, part);
		stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.primordial.melhoria", PartDefinition.name(part)));
		return stack;
	}

	public static void init() {
	}
}
