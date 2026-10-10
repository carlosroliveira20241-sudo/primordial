package com.primordial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.primordial.look.Look;
import java.util.Collection;
import java.util.Set;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/** Criaturas do mod: o mesmo corpo modular do jogador, com forma, cor e acessórios próprios de cada espécie. */
public class FishBodyRenderer<E extends LivingEntity> extends LivingEntityRenderer<E, FishRenderState, ModularFishModel> {
	private final int tint;
	private final float sx;
	private final float sy;
	private final float sz;
	private final String shape;
	private final Set<String> features;
	private final int secondary;
	private final int detail;

	/**
	 * @param parts as partes que essa criatura dropa: o corpo dela se parece com quem as equipa
	 * @param extra acessórios próprios da criatura (campo "features" do JSON)
	 */
	public FishBodyRenderer(EntityRendererProvider.Context context, Look look, Collection<Identifier> parts, Collection<String> extra) {
		super(context, new ModularFishModel(context.bakeLayer(ModularFishModel.LAYER)), look.shadow());
		PartVisuals.Result visual = PartVisuals.combine(look, look.tint(), parts, extra, 0.4F);
		this.tint = visual.tint();
		this.sx = visual.sx();
		this.sy = visual.sy();
		this.sz = visual.sz();
		this.features = visual.features();
		this.shape = FishLook.shapeOf(look.model());
		this.secondary = PartVisuals.shade(visual.tint() & 0xFFFFFF, 0.72F);
		this.detail = PartVisuals.lighten(visual.tint() & 0xFFFFFF, 0.45F);
		this.addLayer(new ColorPassLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(FishRenderState state) {
		return FishLook.TEXTURE;
	}

	@Override
	public FishRenderState createRenderState() {
		return new FishRenderState();
	}

	@Override
	public void extractRenderState(E entity, FishRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.shape = this.shape;
		state.features = this.features;
		state.secondary = this.secondary;
		state.detail = this.detail;
	}

	@Override
	protected int getModelTint(FishRenderState state) {
		return this.tint;
	}

	@Override
	protected void scale(FishRenderState state, PoseStack poseStack) {
		poseStack.scale(this.sx, this.sy, this.sz);
	}

	@Override
	protected void setupRotations(FishRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		FishLook.setupRotations(state, poseStack);
	}
}
