package com.primordial.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * As cores extras do corpo. A passada normal pinta o corpo com a cor principal; esta camada desenha de novo só as
 * nadadeiras e caudas na cor secundária e só os detalhes (chifres, espinhos, dentes...) na cor de detalhe.
 */
public class ColorPassLayer<S extends LivingEntityRenderState & ModularFishModel.Appearance & ColorPassLayer.Colors> extends RenderLayer<S, ModularFishModel> {
	/** O que o estado precisa informar para as passadas extras. */
	public interface Colors {
		int secondaryColor();

		int detailColor();

		Identifier texture();
	}

	/** Um estado "emprestado" que só muda a passada; o modelo lê dele a forma, os acessórios e a animação. */
	private static final class PassState extends LivingEntityRenderState implements ModularFishModel.Appearance {
		private final String shape;
		private final Set<String> features;
		private final int pass;
		private final float head;

		PassState(LivingEntityRenderState from, String shape, Set<String> features, float head, int pass) {
			this.shape = shape;
			this.features = features;
			this.pass = pass;
			this.head = head;
			this.ageInTicks = from.ageInTicks;
			this.isInWater = from.isInWater;
			this.xRot = from.xRot;
			this.yRot = from.yRot;
			this.isInvisible = from.isInvisible;
			this.outlineColor = from.outlineColor;
		}

		@Override
		public String shape() {
			return this.shape;
		}

		@Override
		public Set<String> features() {
			return this.features;
		}

		@Override
		public int pass() {
			return this.pass;
		}

		@Override
		public float headScale() {
			return this.head;
		}
	}

	public ColorPassLayer(RenderLayerParent<S, ModularFishModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
		for (int pass = 1; pass <= 2; pass++) {
			PassState passState = new PassState(state, state.shape(), state.features(), state.headScale(), pass);
			int color = 0xFF000000 | (pass == 1 ? state.secondaryColor() : state.detailColor());
			coloredCutoutModelCopyLayerRender(this.getParentModel(), state.texture(), poseStack, collector, lightCoords, passState, color, pass);
		}
	}
}
