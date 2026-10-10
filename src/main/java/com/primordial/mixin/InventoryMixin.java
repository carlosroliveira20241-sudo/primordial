package com.primordial.mixin;

import com.primordial.capability.InventoryRules;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Com o inventário limitado, o primeiro espaço livre só vale se estiver dentro dos espaços liberados. */
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
}
