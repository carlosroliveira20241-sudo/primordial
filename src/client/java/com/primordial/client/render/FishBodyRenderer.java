package com.primordial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.primordial.look.Look;
import java.util.Collection;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Criaturas do mod: o mesmo corpo do jogador, com cor e proporção próprias de cada espécie. */
public class FishBodyRenderer<E extends LivingEntity> extends LivingEntityRenderer<E, LivingEntityRenderState, CodModel> {
	private final int tint;
	private final float sx;
	private final float sy;
	private final float sz;

	/** @param parts as partes que essa criatura dropa: o corpo dela se parece com quem as equipa */
	public FishBodyRenderer(EntityRendererProvider.Context context, Look look, Collection<Identifier> parts) {
		super(context, new CodModel(context.bakeLayer(ModelLayers.COD)), look.shadow());
		PartVisuals.Result visual = PartVisuals.combine(look, look.tint(), parts, 0.4F);
		this.tint = visual.tint();
		this.sx = visual.sx();
		this.sy = visual.sy();
		this.sz = visual.sz();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return FishLook.TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	protected int getModelTint(LivingEntityRenderState state) {
		return this.tint;
	}

	@Override
	protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
		poseStack.scale(this.sx, this.sy, this.sz);
	}

	@Override
	protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		FishLook.setupRotations(state, poseStack);
	}
}
