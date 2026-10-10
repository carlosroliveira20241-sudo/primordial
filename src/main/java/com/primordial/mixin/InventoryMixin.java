package com.primordial.mixin;

import com.primordial.capability.InventoryRules;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inventário da criatura: só há espaços liberados (primeiro, os da barra) e cada espaço guarda 1 item, sem empilhar.
 * Pegar um monte do chão leva só 1 e deixa o resto lá.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
	@Inject(method = "getFreeSlot", at = @At("RETURN"), cancellable = true)
	private void primordial$limit(CallbackInfoReturnable<Integer> cir) {
		Inventory self = (Inventory) (Object) this;
		int free = cir.getReturnValue();
		if (free >= 0 && InventoryRules.applies(self.player) && free >= InventoryRules.slots(self.player)) {
			cir.setReturnValue(-1);
		}
	}

	@Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
	private void primordial$oneItemPerSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Inventory self = (Inventory) (Object) this;
		if (stack.isEmpty() || !InventoryRules.applies(self.player)) {
			return;
		}
		int target = slot == -1 ? self.getFreeSlot() : slot;
		if (target < 0 || !self.getItem(target).isEmpty()) {
			cir.setReturnValue(false);
			return;
		}
		self.setItem(target, stack.copyWithCount(1));
		self.getItem(target).setPopTime(5);
		stack.shrink(1);
		cir.setReturnValue(true);
	}
}
