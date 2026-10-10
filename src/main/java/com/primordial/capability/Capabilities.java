package com.primordial.capability;

import com.primordial.intellect.IntellectDef;
import com.primordial.intellect.IntellectRegistry;
import com.primordial.intellect.IntellectService;
import com.primordial.intellect.IntellectState;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * O que a criatura pode fazer do Minecraft normal. Cada habilidade de inteligência comprada e EQUIPADA liberta uma
 * dessas ações: quebrar blocos (4 níveis de dureza), colocar blocos, usar e criar cada tipo de ferramenta e atirar.
 */
public final class Capabilities {
	public static final String[] TOOLS = {"pickaxe", "shovel", "axe", "sword", "bow"};

	private Capabilities() {
	}

	/** As capacidades dos slots de inteligência que estão ocupados agora. */
	public static Set<String> active(Player player) {
		IntellectState state = IntellectService.state(player);
		Set<String> result = new HashSet<>();
		for (int i = 0; i < state.slotCount(); i++) {
			Identifier id = state.slot(i);
			IntellectDef def = id == null ? null : IntellectRegistry.get(id);
			if (def != null) {
				result.add(def.capability());
			}
		}
		return result;
	}

	public static boolean has(Player player, String capability) {
		return active(player).contains(capability);
	}

	/** Maior nível de quebra liberado (0 = não quebra nada). */
	public static int breakTier(Player player) {
		int best = 0;
		for (String capability : active(player)) {
			if (capability.startsWith("break_")) {
				best = Math.max(best, Integer.parseInt(capability.substring(6)));
			}
		}
		return best;
	}

	/** Nível de quebra que o bloco exige, pela dureza: 1 mole, 2 madeira, 3 pedra, 4 minério e duros. 99 = inquebrável. */
	public static int tierNeeded(BlockState state, BlockGetter level, BlockPos pos) {
		float hardness = state.getDestroySpeed(level, pos);
		if (hardness < 0) {
			return 99;
		}
		return hardness <= 0.8F ? 1 : hardness <= 2.5F ? 2 : hardness <= 4.5F ? 3 : 4;
	}

	public static boolean canBreak(Player player, BlockGetter level, BlockPos pos, BlockState state) {
		return breakTier(player) >= tierNeeded(state, level, pos);
	}

	/** O tipo de ferramenta do item (pickaxe, shovel, axe, sword, bow), ou nulo se não for ferramenta. */
	public static @Nullable String toolType(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		if (stack.is(ItemTags.PICKAXES)) {
			return "pickaxe";
		}
		if (stack.is(ItemTags.SHOVELS)) {
			return "shovel";
		}
		if (stack.is(ItemTags.AXES)) {
			return "axe";
		}
		if (stack.is(ItemTags.SWORDS)) {
			return "sword";
		}
		if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
			return "bow";
		}
		return null;
	}

	public static boolean canUseTool(Player player, ItemStack stack) {
		String type = toolType(stack);
		return type == null || has(player, "use_" + type);
	}

	public static boolean canCraftTool(Player player, ItemStack stack) {
		String type = toolType(stack);
		return type == null || has(player, "craft_" + type);
	}
}
