package com.primordial.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.primordial.client.render.FishPlayerRenderer;
import com.primordial.client.render.FishPlayerState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Troca o desenho do jogador (modelo de gente) pelo peixe quando ele já escolheu a origem. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Unique
	private FishPlayerRenderer primordial$fish;

	@Inject(method = "onResourceManagerReload", at = @At("TAIL"))
	private void primordial$build(ResourceManager resourceManager, CallbackInfo ci, @Local EntityRendererProvider.Context context) {
		this.primordial$fish = new FishPlayerRenderer(context);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
	private <T extends Entity> void primordial$entity(T entity, CallbackInfoReturnable<EntityRenderer<? super T, ?>> cir) {
		if (this.primordial$fish != null && entity instanceof AbstractClientPlayer player && FishPlayerRenderer.applies(player)) {
			cir.setReturnValue((EntityRenderer) this.primordial$fish);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
	private <S extends EntityRenderState> void primordial$state(S state, CallbackInfoReturnable<EntityRenderer<?, ? super S>> cir) {
		if (this.primordial$fish != null && state instanceof FishPlayerState) {
			cir.setReturnValue((EntityRenderer) this.primordial$fish);
		}
	}
}
