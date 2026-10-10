package com.primordial.client.render;

import java.util.Set;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class FishPlayerState extends AvatarRenderState implements ModularFishModel.Appearance {
	public int tint = 0xFFFFFFFF;
	public float sx = 1.0F;
	public float sy = 1.0F;
	public float sz = 1.0F;
	public String shape = "fish";
	public com.primordial.creature.Pattern pattern = com.primordial.creature.Pattern.LISO;
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
