package com.primordial.capability;

import com.primordial.intellect.IntellectDef;
import com.primordial.intellect.IntellectRegistry;
import com.primordial.intellect.IntellectService;
import com.primordial.intellect.IntellectState;
import com.primordial.item.PrimordialItems;
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

/**
 * O que a criatura pode fazer do Minecraft normal. Cada habilidade de inteligência comprada e EQUIPADA liberta uma
 * dessas ações: quebrar blocos moles e madeira, colocar blocos, usar ferramentas e armas, criar ferramentas, criar
 * armas (só a espada de pedra) e criar baú de areia. Blocos duros (pedra, minério) só quebram com uma ferramenta.
 */
public final class Capabilities {
	public enum Kind {
		TOOL, WEAPON
	}

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

	/** Maior nível de quebra liberado pelas habilidades (0 = não quebra nada). */
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

	/** Quebra liberada por habilidade, ou por uma ferramenta na mão que sirva para o bloco (e saiba usar). */
	public static boolean canBreak(Player player, BlockGetter level, BlockPos pos, BlockState state) {
		int needed = tierNeeded(state, level, pos);
		if (needed >= 99) {
			return false;
		}
		if (breakTier(player) >= needed) {
			return true;
		}
		ItemStack held = player.getMainHandItem();
		return kind(held) == Kind.TOOL && has(player, "use_tools") && held.getDestroySpeed(state) > 1.0F;
	}

	/** Ferramenta (pá, picareta, machado, enxada, Ferramenta de Coral) ou arma (espada, arco); nulo se não for nenhum. */
	public static Kind kind(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.HOES)
			|| stack.is(PrimordialItems.CORAL_TOOL)) {
			return Kind.TOOL;
		}
		if (stack.is(ItemTags.SWORDS) || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
			return Kind.WEAPON;
		}
		return null;
	}

	/** "Usar Ferramenta" é a única habilidade que deixa segurar e usar ferramentas E armas. */
	public static boolean canUseTool(Player player, ItemStack stack) {
		return kind(stack) == null || has(player, "use_tools");
	}

	/**
	 * Criar ferramenta exige "Criar Ferramenta". Criar arma exige "Criar Arma" E uma pedra: só a espada de pedra
	 * pode ser fabricada.
	 */
	public static boolean canCraftTool(Player player, ItemStack result) {
		Kind kind = kind(result);
		if (kind == null) {
			return true;
		}
		if (kind == Kind.TOOL) {
			return has(player, "craft_tool");
		}
		return has(player, "craft_weapon") && result.is(Items.STONE_SWORD);
	}
}
