package com.primordial.mixin;

import com.primordial.capability.Capabilities;
import com.primordial.capability.InventoryRules;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dois bloqueios nos espaços da tela de inventário: espaços não liberados não aceitam itens, e o resultado de uma
 * receita de ferramenta só sai se a criatura aprendeu a criar aquele tipo de ferramenta.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void primordial$locked(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Slot self = (Slot) (Object) this;
		if (self.container instanceof Inventory inventory && InventoryRules.locked(inventory.player, self.getContainerSlot())) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
	private void primordial$single(CallbackInfoReturnable<Integer> cir) {
		Slot self = (Slot) (Object) this;
		if (self.container instanceof Inventory inventory && InventoryRules.applies(inventory.player) && self.getContainerSlot() < InventoryRules.MAIN_SIZE) {
			cir.setReturnValue(1);
		}
	}

	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void primordial$craft(Player player, CallbackInfoReturnable<Boolean> cir) {
		Slot self = (Slot) (Object) this;
		if (self instanceof ResultSlot && InventoryRules.applies(player) && !Capabilities.canCraftTool(player, self.getItem())) {
			cir.setReturnValue(false);
		}
	}
}
