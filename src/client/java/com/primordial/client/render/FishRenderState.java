package com.primordial.client.render;

import java.util.Set;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Estado de desenho das criaturas do mod: forma do corpo, acessórios ligados e as três cores. */
public class FishRenderState extends LivingEntityRenderState implements ModularFishModel.Appearance, ColorPassLayer.Colors {
	public String shape = "fish";
	public Set<String> features = Set.of();
	public int secondary = 0xCCCCCC;
	public int detail = 0xFFFFFF;

	@Override
	public String shape() {
		return this.shape;
	}

	@Override
	public Set<String> features() {
		return this.features;
	}

	@Override
	public int secondaryColor() {
		return this.secondary;
	}

	@Override
	public int detailColor() {
		return this.detail;
	}

	@Override
	public Identifier texture() {
		return FishLook.TEXTURE;
	}
}
