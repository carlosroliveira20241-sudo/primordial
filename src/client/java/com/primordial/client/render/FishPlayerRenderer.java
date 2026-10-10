package com.primordial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.Creatures;
import com.primordial.look.Look;
import com.primordial.look.LookRegistry;
import net.minecraft.client.model.animal.fish.CodModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

/** Desenha o jogador-criatura como peixe, tingido com a cor primária escolhida. */
public class FishPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, FishPlayerState, CodModel> {
	public FishPlayerRenderer(EntityRendererProvider.Context context) {
		super(context, new CodModel(context.bakeLayer(ModelLayers.COD)), 0.3F);
	}

	public static boolean applies(AbstractClientPlayer player) {
		return Creatures.isCreature(player);
	}

	@Override
	public Identifier getTextureLocation(FishPlayerState state) {
		return FishLook.TEXTURE;
	}

	@Override
	public FishPlayerState createRenderState() {
		return new FishPlayerState();
	}

	@Override
	public void extractRenderState(AbstractClientPlayer entity, FishPlayerState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.id = entity.getId();
		CreatureBody body = Creatures.body(entity);
		Look look = LookRegistry.get(body.look());
		state.tint = 0xFF000000 | body.primaryColor();
		state.sx = look.sx();
		state.sy = look.sy();
		state.sz = look.sz();
	}

	@Override
	protected int getModelTint(FishPlayerState state) {
		return state.tint;
	}

	@Override
	protected void scale(FishPlayerState state, PoseStack poseStack) {
		poseStack.scale(state.sx, state.sy, state.sz);
	}

	@Override
	protected void setupRotations(FishPlayerState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		FishLook.setupRotations(state, poseStack);
	}
}
