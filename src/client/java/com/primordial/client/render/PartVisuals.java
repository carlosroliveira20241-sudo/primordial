package com.primordial.client.render;

import com.primordial.creature.PartRef;
import com.primordial.look.Look;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Junta a aparência base com o que as partes mudam no corpo (campo "visual" do JSON da parte, incluindo o estilo
 * escolhido). Serve o jogador (partes equipadas) e as criaturas (partes que elas dropam), então um bicho que "tem" uma
 * habilidade se parece com quem a equipa.
 */
public final class PartVisuals {
	private PartVisuals() {
	}

	public record Result(float sx, float sy, float sz, int tint, Set<String> features) {
	}

	/** Versão para criaturas do mod: partes sem estilo. */
	public static Result combine(Look look, int baseTint, Collection<Identifier> parts, Collection<String> extraFeatures, float mix) {
		return combineRefs(look, baseTint, parts.stream().map(id -> new PartRef(id, 1, 0)).toList(), extraFeatures, mix);
	}

	/** @param mix o quanto a cor das partes puxa a cor do corpo (0 a 1) */
	public static Result combineRefs(Look look, int baseTint, List<PartRef> parts, Collection<String> extraFeatures, float mix) {
		Set<String> features = new HashSet<>(extraFeatures);
		float sx = look.sx();
		float sy = look.sy();
		float sz = look.sz();
		int r = 0;
		int g = 0;
		int b = 0;
		int tinted = 0;
		for (PartRef ref : parts) {
			PartDefinition def = PartRegistry.get(ref.id());
			if (def == null || def.visual().isEmpty()) {
				continue;
			}
			PartDefinition.Visual visual = def.visual().get();
			List<String> partFeatures = visual.features();
			java.util.Optional<Integer> partTint = visual.tint();
			// o estilo escolhido troca a cor e/ou os acessórios, sem mudar os status
			if (ref.style() > 0 && ref.style() - 1 < visual.styles().size()) {
				PartDefinition.Visual.Style style = visual.styles().get(ref.style() - 1);
				partFeatures = style.features().orElse(partFeatures);
				partTint = style.tint().isPresent() ? style.tint() : partTint;
			}
			features.addAll(partFeatures);
			if (visual.scale().size() >= 3) {
				sx *= visual.scale().get(0);
				sy *= visual.scale().get(1);
				sz *= visual.scale().get(2);
			}
			if (partTint.isPresent()) {
				int color = partTint.get();
				r += ARGB.red(color);
				g += ARGB.green(color);
				b += ARGB.blue(color);
				tinted++;
			}
		}
		int tint = baseTint & 0xFFFFFF;
		if (tinted > 0) {
			int pr = ARGB.red(tint);
			int pg = ARGB.green(tint);
			int pb = ARGB.blue(tint);
			tint = ARGB.color(255,
				Math.round(pr + (r / tinted - pr) * mix),
				Math.round(pg + (g / tinted - pg) * mix),
				Math.round(pb + (b / tinted - pb) * mix)) & 0xFFFFFF;
		}
		return new Result(sx, sy, sz, 0xFF000000 | tint, features);
	}

	/** Cor derivada para criaturas do mod: secundária um pouco mais escura, detalhe um pouco mais clara. */
	public static int shade(int rgb, float factor) {
		int r = Math.min(255, Math.round(ARGB.red(rgb) * factor));
		int g = Math.min(255, Math.round(ARGB.green(rgb) * factor));
		int b = Math.min(255, Math.round(ARGB.blue(rgb) * factor));
		return (r << 16) | (g << 8) | b;
	}

	public static int lighten(int rgb, float amount) {
		int r = ARGB.red(rgb) + Math.round((255 - ARGB.red(rgb)) * amount);
		int g = ARGB.green(rgb) + Math.round((255 - ARGB.green(rgb)) * amount);
		int b = ARGB.blue(rgb) + Math.round((255 - ARGB.blue(rgb)) * amount);
		return (r << 16) | (g << 8) | b;
	}
}
