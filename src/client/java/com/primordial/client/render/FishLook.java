package com.primordial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.primordial.Primordial;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** O visual de peixe compartilhado: o jogador e as criaturas do mod usam o mesmo corpo, só muda a cor. */
public final class FishLook {
	public static final Identifier TEXTURE = Primordial.id("textures/entity/fish_body.png");

	private FishLook() {
	}

	public static void setupRotations(LivingEntityRenderState state, PoseStack poseStack) {
		poseStack.mulPose(Axis.YP.rotationDegrees(4.3F * Mth.sin(0.6F * state.ageInTicks)));
		if (state.isInWater) {
			poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot));
		} else {
			poseStack.translate(0.1F, 0.1F, -0.1F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
		}
	}
}
