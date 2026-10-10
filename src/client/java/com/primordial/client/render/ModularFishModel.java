package com.primordial.client.render;

import com.primordial.Primordial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * O corpo modular de todas as criaturas (jogador e mobs). Tem 3 formas base (peixe, bolha, serpente) e vários
 * "acessórios" que aparecem ou somem conforme as partes equipadas: chifre, espinhos, asas, tentáculos...
 * Para criar um acessório novo: acrescente as caixas em {@link #createLayer()} com o prefixo "feat_<nome>".
 */
public class ModularFishModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Primordial.id("modular_body"), "main");

	private static final int Y = 22;
	private static final int SEGMENTS = 6;
	private static final int TENTACLES = 6;

	/** Quem quiser ser desenhado por este modelo diz a forma e os acessórios. */
	public interface Appearance {
		String shape();

		Set<String> features();
	}

	private final Map<String, List<ModelPart>> features = new HashMap<>();
	private final Map<ModelPart, float[]> basePose = new HashMap<>();
	private final List<ModelPart> fish = new ArrayList<>();
	private final List<ModelPart> blob = new ArrayList<>();
	private final List<ModelPart> serpent = new ArrayList<>();
	private final List<ModelPart> blobTentacles = new ArrayList<>();
	private final List<ModelPart> serpentSegments = new ArrayList<>();
	private final List<ModelPart> featTentacles = new ArrayList<>();
	private final ModelPart fishTail;
	private final ModelPart bigTail;
	private final ModelPart wingL;
	private final ModelPart wingR;
	private final ModelPart eyeL;
	private final ModelPart eyeR;

	public ModularFishModel(ModelPart root) {
		super(root);
		for (String name : new String[] {"fish_body", "fish_head", "fish_nose", "fish_tail", "fish_top", "fish_fin_l", "fish_fin_r"}) {
			this.fish.add(root.getChild(name));
		}
		this.blob.add(root.getChild("blob_body"));
		for (int i = 0; i < TENTACLES; i++) {
			ModelPart part = root.getChild("blob_tentacle_" + i);
			this.blob.add(part);
			this.blobTentacles.add(part);
		}
		for (int i = 0; i < SEGMENTS; i++) {
			ModelPart part = root.getChild("serp_" + i);
			this.serpent.add(part);
			this.serpentSegments.add(part);
		}
		this.serpent.add(root.getChild("serp_head"));
		this.fishTail = root.getChild("fish_tail");
		this.bigTail = root.getChild("feat_bigtail");
		this.wingL = root.getChild("feat_wings_l");
		this.wingR = root.getChild("feat_wings_r");
		this.eyeL = root.getChild("eye_l");
		this.eyeR = root.getChild("eye_r");

		register(root, "spikes", "feat_spikes_", 5);
		register(root, "horn", "feat_horn", 0);
		register(root, "jaw", "feat_jaw", 0);
		register(root, "teeth", "feat_teeth", 0);
		register(root, "bigtail", "feat_bigtail", 0);
		register(root, "wings", "feat_wings_l", 0);
		register(root, "wings", "feat_wings_r", 0);
		register(root, "sail", "feat_sail", 0);
		register(root, "antenna", "feat_antenna", 0);
		register(root, "antenna", "feat_antenna_bulb", 0);
		register(root, "shell", "feat_shell", 0);
		register(root, "bulb", "feat_bulb", 0);
		register(root, "whiskers", "feat_whisker_l", 0);
		register(root, "whiskers", "feat_whisker_r", 0);
		register(root, "big_eyes", "feat_eye_big_l", 0);
		register(root, "big_eyes", "feat_eye_big_r", 0);
		register(root, "orbs", "feat_orb_l", 0);
		register(root, "orbs", "feat_orb_r", 0);
		register(root, "crystals", "feat_crystals_", 3);
		register(root, "legs", "feat_legs_", 4);
		register(root, "tentacles", "feat_tentacles_", 4);
		register(root, "brain_s", "feat_brain_s", 0);
		register(root, "brain_m", "feat_brain_m", 0);
		register(root, "brain_l", "feat_brain_l", 0);
		this.featTentacles.addAll(this.features.getOrDefault("tentacles", List.of()));
		for (ModelPart part : this.eachPart(root)) {
			this.basePose.put(part, new float[] {part.x, part.y, part.z});
		}
	}

	private List<ModelPart> eachPart(ModelPart root) {
		List<ModelPart> all = new ArrayList<>();
		for (List<ModelPart> list : this.features.values()) {
			all.addAll(list);
		}
		all.addAll(this.fish);
		all.addAll(this.blob);
		all.addAll(this.serpent);
		all.add(this.eyeL);
		all.add(this.eyeR);
		return all;
	}

	private void register(ModelPart root, String feature, String prefix, int count) {
		List<ModelPart> list = this.features.computeIfAbsent(feature, k -> new ArrayList<>());
		if (count == 0) {
			list.add(root.getChild(prefix));
		} else {
			for (int i = 0; i < count; i++) {
				list.add(root.getChild(prefix + i));
			}
		}
	}

	private static void box(PartDefinition root, String name, int u, int v, float x, float y, float z, float w, float h, float d, float px, float py, float pz) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d), PartPose.offset(px, py, pz));
	}

	private static void boxRot(PartDefinition root, String name, int u, int v, float x, float y, float z, float w, float h, float d,
		float px, float py, float pz, float rx, float ry, float rz) {
		root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d), PartPose.offsetAndRotation(px, py, pz, rx, ry, rz));
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Textura 64x64: tudo claro, exceto a zona escura (32,0) para espinhos/barbatanas e a preta (48,0) para os olhos.
		final int dark = 32;

		// ---- forma PEIXE
		box(root, "fish_body", 0, 0, -1.5F, -2.5F, 0, 3, 5, 8, 0, Y, 0);
		box(root, "fish_head", 0, 16, -1.5F, -2.5F, -4, 3, 5, 4, 0, Y, 0);
		box(root, "fish_nose", 0, 26, -1, -1.5F, -2, 2, 3, 2, 0, Y, -4);
		box(root, "fish_tail", dark, 0, 0, -2.5F, 0, 0, 5, 4, 0, Y, 8);
		box(root, "fish_top", dark, 0, 0, -2, -1, 0, 2, 6, 0, Y - 2.5F, 0);
		boxRot(root, "fish_fin_l", dark, 0, 0, 0, -1, 2.5F, 0, 2, 1.5F, Y + 1, 1, 0, 0, 0.7F);
		boxRot(root, "fish_fin_r", dark, 0, -2.5F, 0, -1, 2.5F, 0, 2, -1.5F, Y + 1, 1, 0, 0, -0.7F);

		// ---- forma BOLHA (criatura redonda com tentáculos)
		box(root, "blob_body", 0, 0, -4, -4, -4, 8, 8, 8, 0, Y - 3, 0);
		for (int i = 0; i < TENTACLES; i++) {
			double angle = i * Math.PI * 2 / TENTACLES;
			box(root, "blob_tentacle_" + i, dark, 0, -0.5F, 0, -0.5F, 1, 7, 1, (float) Math.cos(angle) * 3, Y + 1, (float) Math.sin(angle) * 3);
		}

		// ---- forma SERPENTE (corpo comprido em segmentos)
		for (int i = 0; i < SEGMENTS; i++) {
			float size = 4.0F - i * 0.45F;
			box(root, "serp_" + i, 0, 0, -size / 2, -size / 2, 0, size, size, 5, 0, Y, i * 5);
		}
		box(root, "serp_head", 0, 16, -2.5F, -2.5F, -5, 5, 5, 5, 0, Y, 0);

		// ---- olhos (a posição muda com a forma)
		box(root, "eye_l", 48, 0, -0.5F, -0.5F, -0.5F, 1, 1, 1, 1.6F, Y - 1, -3);
		box(root, "eye_r", 48, 0, -0.5F, -0.5F, -0.5F, 1, 1, 1, -1.6F, Y - 1, -3);

		// ---- ACESSÓRIOS (aparecem conforme as partes equipadas)
		for (int i = 0; i < 5; i++) {
			box(root, "feat_spikes_" + i, dark, 0, -0.5F, -2, -0.5F, 1, 2, 1, 0, Y - 2.5F, 0.5F + i * 1.6F);
		}
		box(root, "feat_horn", dark, 0, -0.5F, -0.5F, -6, 1, 1, 6, 0, Y - 1, -4);
		box(root, "feat_jaw", 0, 26, -1.2F, 0, -5, 2.4F, 1, 4, 0, Y + 2.2F, 0);
		box(root, "feat_teeth", 48, 0, -1, -0.8F, -5, 2, 1, 1, 0, Y + 2.2F, 0);
		box(root, "feat_bigtail", dark, 0, 0, -4, 0, 0, 8, 6, 0, Y, 8);
		boxRot(root, "feat_wings_l", dark, 0, 0, 0, -2.5F, 7, 0, 5, 1.5F, Y, 2, 0, 0, -0.3F);
		boxRot(root, "feat_wings_r", dark, 0, -7, 0, -2.5F, 7, 0, 5, -1.5F, Y, 2, 0, 0, 0.3F);
		box(root, "feat_sail", dark, 0, 0, -8, 0, 0, 7, 8, 0, Y - 2.5F, 0);
		box(root, "feat_antenna", 0, 26, -0.25F, -6, -0.25F, 0.5F, 6, 0.5F, 0, Y - 2.5F, -3);
		box(root, "feat_antenna_bulb", 48, 0, -1, -7.5F, -1, 2, 2, 2, 0, Y - 2.5F, -3);
		box(root, "feat_shell", dark, 0, -3, -1.5F, 0, 6, 2, 8, 0, Y - 3.5F, 0);
		box(root, "feat_bulb", 0, 0, -3, 0, 0, 6, 4, 7, 0, Y + 1.5F, 0);
		boxRot(root, "feat_whisker_l", dark, 0, 0, 0, 0, 3, 0.3F, 0.3F, 1.5F, Y + 1, -4, 0, -0.4F, 0);
		boxRot(root, "feat_whisker_r", dark, 0, -3, 0, 0, 3, 0.3F, 0.3F, -1.5F, Y + 1, -4, 0, 0.4F, 0);
		box(root, "feat_eye_big_l", 48, 0, -1, -1, -1, 2, 2, 2, 2, Y - 1, -3);
		box(root, "feat_eye_big_r", 48, 0, -1, -1, -1, 2, 2, 2, -2, Y - 1, -3);
		box(root, "feat_orb_l", 0, 26, -0.6F, -0.6F, -0.6F, 1.2F, 1.2F, 1.2F, 2, Y - 7, -1);
		box(root, "feat_orb_r", 0, 26, -0.6F, -0.6F, -0.6F, 1.2F, 1.2F, 1.2F, -2, Y - 7, 1);
		for (int i = 0; i < 3; i++) {
			boxRot(root, "feat_crystals_" + i, 0, 26, -0.5F, -4, -0.5F, 1, 4, 1, (i - 1) * 1.3F, Y - 2.5F, 1.5F + i * 2.2F, 0, 0, (i - 1) * 0.35F);
		}
		for (int i = 0; i < 4; i++) {
			box(root, "feat_legs_" + i, dark, 0, -0.5F, 0, -0.5F, 1, 3, 1, i % 2 == 0 ? 2 : -2, Y + 2, i < 2 ? 1.5F : 6);
		}
		box(root, "feat_brain_s", dark, 0, -1, -1.6F, -3.5F, 2, 1.6F, 2.5F, 0, Y - 2.5F, 0);
		box(root, "feat_brain_m", dark, 0, -1.5F, -2.6F, -4, 3, 2.6F, 4, 0, Y - 2.5F, 0);
		box(root, "feat_brain_l", dark, 0, -2.2F, -3.8F, -4.6F, 4.4F, 3.8F, 5.2F, 0, Y - 2.5F, 0);
		for (int i = 0; i < 4; i++) {
			box(root, "feat_tentacles_" + i, dark, 0, -0.4F, 0, -0.4F, 0.8F, 7, 0.8F, i % 2 == 0 ? 0.8F : -0.8F, Y + 2, 1 + i * 1.6F);
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		String shape = "fish";
		Set<String> wanted = Set.of();
		if (state instanceof Appearance appearance) {
			shape = appearance.shape();
			wanted = appearance.features();
		}
		boolean isFish = shape.equals("fish");
		boolean isBlob = shape.equals("blob");
		boolean isSerpent = shape.equals("serpent");
		this.fish.forEach(p -> p.visible = isFish);
		this.blob.forEach(p -> p.visible = isBlob);
		this.serpent.forEach(p -> p.visible = isSerpent);

		// Posições dos acessórios mudam com a forma: na bolha ficam mais altos.
		float lift = isBlob ? -3.0F : 0.0F;
		float forward = isSerpent ? -0.5F : 0.0F;
		for (Map.Entry<String, List<ModelPart>> entry : this.features.entrySet()) {
			boolean show = wanted.contains(entry.getKey());
			for (ModelPart part : entry.getValue()) {
				part.visible = show;
				float[] base = this.basePose.get(part);
				if (base != null) {
					part.y = base[1] + lift;
					part.z = base[2] + forward;
				}
			}
		}
		float eyeY = isBlob ? Y - 4 : Y - 1;
		float eyeZ = isBlob ? -4.2F : isSerpent ? -4.5F : -3;
		float eyeX = isBlob ? 2.2F : isSerpent ? 1.8F : 1.6F;
		this.eyeL.setPos(eyeX, eyeY, eyeZ);
		this.eyeR.setPos(-eyeX, eyeY, eyeZ);
		boolean bigEyes = wanted.contains("big_eyes");
		this.eyeL.visible = !bigEyes;
		this.eyeR.visible = !bigEyes;

		float t = state.ageInTicks;
		float amplitude = state.isInWater ? 1.0F : 1.5F;
		this.fishTail.yRot = -amplitude * 0.45F * Mth.sin(0.6F * t);
		this.bigTail.yRot = this.fishTail.yRot;
		for (int i = 0; i < this.blobTentacles.size(); i++) {
			this.blobTentacles.get(i).xRot = 0.35F * Mth.sin(0.25F * t + i);
			this.blobTentacles.get(i).zRot = 0.2F * Mth.cos(0.2F * t + i * 1.3F);
		}
		for (int i = 0; i < this.serpentSegments.size(); i++) {
			this.serpentSegments.get(i).yRot = 0.3F * Mth.sin(0.45F * t - i * 0.9F);
		}
		for (int i = 0; i < this.featTentacles.size(); i++) {
			this.featTentacles.get(i).xRot = 0.4F * Mth.sin(0.3F * t + i * 1.1F);
		}
		float flap = 0.25F * Mth.sin(0.35F * t);
		this.wingL.zRot = -0.3F - flap;
		this.wingR.zRot = 0.3F + flap;
	}
}
