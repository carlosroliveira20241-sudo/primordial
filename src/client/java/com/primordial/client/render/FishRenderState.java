package com.primordial.client.render;

import java.util.Set;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Estado de desenho das criaturas do mod: forma do corpo, acessórios ligados, cor e proporção. */
public class FishRenderState extends LivingEntityRenderState implements ModularFishModel.Appearance {
	public String shape = "fish";
	public Set<String> features = Set.of();

	@Override
	public String shape() {
		return this.shape;
	}

	@Override
	public Set<String> features() {
		return this.features;
	}
}
