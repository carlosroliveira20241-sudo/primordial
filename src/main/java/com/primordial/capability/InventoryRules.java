package com.primordial.capability;

import com.primordial.creature.Creatures;
import com.primordial.intellect.IntellectService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Inventário limitado: a criatura começa com 1 espaço e a Força (e algumas partes) libera mais. Só vale para quem já
 * escolheu a origem e não está no criativo.
 */
public final class InventoryRules {
	/** Quantos espaços cada nível de Força dá. */
	private static final int[] BY_STRENGTH = {1, 2, 3, 4, 6, 9, 12, 18, 27, 36};
	public static final int MAIN_SIZE = 36;

	private InventoryRules() {
	}

	public static boolean applies(Player player) {
		return Creatures.isRestricted(player);
	}

	/** Espaços liberados: 1 a 36, na ordem da barra de atalhos e depois do inventário. */
	public static int slots(Player player) {
		int strength = Math.max(1, Math.min(BY_STRENGTH.length, IntellectService.state(player).strength()));
		int bonus = 2 * Creatures.passiveLevel(player, "sand_pouch");
		return Math.min(MAIN_SIZE, BY_STRENGTH[strength - 1] + bonus);
	}

	public static int slotsForStrength(int strength) {
		return BY_STRENGTH[Math.max(1, Math.min(BY_STRENGTH.length, strength)) - 1];
	}

	public static boolean locked(Player player, int index) {
		return applies(player) && index >= slots(player) && index < MAIN_SIZE;
	}

	/** De tempos em tempos: solta o que estiver em espaço bloqueado e ferramentas que a criatura não sabe usar. */
	public static void enforce(ServerPlayer player) {
		if (!applies(player) || player.tickCount % 20 != 0) {
			return;
		}
		Inventory inventory = player.getInventory();
		boolean dropped = false;
		for (int i = 0; i < MAIN_SIZE; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.getCount() > 1 && i < slots(player)) {
				ItemStack extra = stack.split(stack.getCount() - 1);
				ItemEntity spill = player.drop(extra, false, false);
				if (spill != null) {
					spill.setPickUpDelay(200);
				}
				dropped = true;
			}
			boolean blockedSlot = i >= slots(player);
			boolean unknownTool = !Capabilities.canUseTool(player, stack);
			if (blockedSlot || unknownTool) {
				inventory.setItem(i, ItemStack.EMPTY);
				ItemEntity drop = player.drop(stack, false, false);
				if (drop != null) {
					drop.setPickUpDelay(200);
				}
				dropped = true;
			}
		}
		if (dropped) {
			player.sendSystemMessage(Component.translatable("msg.primordial.inventory_dropped").withStyle(ChatFormatting.YELLOW), true);
		}
	}
}
