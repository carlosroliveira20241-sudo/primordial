package com.primordial.client.render;

import java.util.Set;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

public class FishPlayerState extends AvatarRenderState implements ModularFishModel.Appearance, ColorPassLayer.Colors {
	public int tint = 0xFFFFFFFF;
	public float sx = 1.0F;
	public float sy = 1.0F;
	public float sz = 1.0F;
	public String shape = "fish";
	public Set<String> features = Set.of();
	public com.primordial.creature.Pattern pattern = com.primordial.creature.Pattern.LISO;
	public int secondary = 0xF2C14E;
	public int detail = 0xF5F5F5;
	public float head = 1.0F;

	@Override
	public String shape() {
		return this.shape;
	}

	@Override
	public Set<String> features() {
		return this.features;
	}

	@Override
	public float headScale() {
		return this.head;
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
		return FishLook.texture(this.pattern);
	}
}
