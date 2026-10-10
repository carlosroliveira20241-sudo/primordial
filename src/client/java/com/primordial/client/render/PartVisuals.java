package com.primordial.client.render;

import com.primordial.look.Look;
import com.primordial.part.PartDefinition;
import com.primordial.part.PartRegistry;
import java.util.Collection;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Junta a aparência base com o que as partes mudam no corpo (campo "visual" do JSON da parte).
 * Serve o jogador (partes equipadas) e as criaturas (partes que elas dropam), então um bicho que "tem" uma
 * habilidade se parece com quem a equipa.
 */
public final class PartVisuals {
	private PartVisuals() {
	}

	public record Result(float sx, float sy, float sz, int tint) {
	}

	/** @param mix o quanto a cor das partes puxa a cor do corpo (0 a 1) */
	public static Result combine(Look look, int baseTint, Collection<Identifier> parts, float mix) {
		float sx = look.sx();
		float sy = look.sy();
		float sz = look.sz();
		int r = 0;
		int g = 0;
		int b = 0;
		int tinted = 0;
		for (Identifier id : parts) {
			PartDefinition def = PartRegistry.get(id);
			if (def == null || def.visual().isEmpty()) {
				continue;
			}
			PartDefinition.Visual visual = def.visual().get();
			if (visual.scale().size() >= 3) {
				sx *= visual.scale().get(0);
				sy *= visual.scale().get(1);
				sz *= visual.scale().get(2);
			}
			if (visual.tint().isPresent()) {
				int color = visual.tint().get();
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
		return new Result(sx, sy, sz, 0xFF000000 | tint);
	}
}
