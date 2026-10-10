package com.primordial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.Creatures;
import com.primordial.creature.PartRef;
import com.primordial.look.Look;
import com.primordial.look.LookRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

/** Desenha o jogador-criatura como peixe, tingido com a cor primária escolhida. */
public class FishPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, FishPlayerState, ModularFishModel> {
	public FishPlayerRenderer(EntityRendererProvider.Context context) {
		super(context, new ModularFishModel(context.bakeLayer(ModularFishModel.LAYER)), 0.3F);
	}

	public static boolean applies(AbstractClientPlayer player) {
		return Creatures.isCreature(player);
	}

	@Override
	public Identifier getTextureLocation(FishPlayerState state) {
		return FishLook.texture(state.pattern);
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
		PartVisuals.Result visual = PartVisuals.combine(look, body.primaryColor(),
			body.parts().values().stream().map(PartRef::id).toList(), java.util.List.of(), 0.25F);
		state.tint = visual.tint();
		state.sx = visual.sx();
		state.sy = visual.sy();
		state.sz = visual.sz();
		state.features = visual.features();
		int brain = com.primordial.intellect.IntellectService.state(entity).brain();
		if (brain >= 2) {
			state.features.add(brain >= 6 ? "brain_l" : brain >= 4 ? "brain_m" : "brain_s");
		}
		state.shape = FishLook.shapeOf(look.model());
		state.pattern = body.pattern();
		// o nome da espécie aparece junto do nome do jogador
		if (!body.speciesName().isEmpty() && state.nameTag != null) {
			state.nameTag = net.minecraft.network.chat.Component.literal(body.speciesName() + " (").append(state.nameTag).append(")");
		}
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
