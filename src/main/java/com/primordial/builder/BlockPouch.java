package com.primordial.builder;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** O "baú de areia" da criatura: quantos blocos de cada tipo ela carrega. Só o próprio jogador recebe. */
public record BlockPouch(Map<Identifier, Integer> blocks) {
	public static final BlockPouch DEFAULT = new BlockPouch(Map.of());
	public static final Codec<BlockPouch> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.INT).xmap(BlockPouch::new, BlockPouch::blocks);

	public int total() {
		int sum = 0;
		for (int count : this.blocks.values()) {
			sum += count;
		}
		return sum;
	}

	public int count(Identifier block) {
		return this.blocks.getOrDefault(block, 0);
	}

	public BlockPouch with(Identifier block, int count) {
		Map<Identifier, Integer> copy = new HashMap<>(this.blocks);
		if (count <= 0) {
			copy.remove(block);
		} else {
			copy.put(block, count);
		}
		return new BlockPouch(copy);
	}
}
