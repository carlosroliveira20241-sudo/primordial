package com.primordial.mixin;

import com.primordial.creature.Creatures;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/** Peixe na água sempre nada (pose de nado), sem precisar correr. Roda no cliente e no servidor. */
	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void primordial$alwaysSwim(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (Creatures.isAquatic(self) && self.isInWater() && !self.getAbilities().flying) {
			self.setSwimming(true);
			ci.cancel();
		}
	}
}
