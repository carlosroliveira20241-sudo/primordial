package com.primordial.builder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Construções temporárias: depois de um tempo, o bloco original (água, ar...) volta ao lugar. */
public final class TempBlocks {
	private record Entry(BlockState original, long expires) {
	}

	private static final Map<Identifier, Map<Long, Entry>> ALL = new HashMap<>();

	private TempBlocks() {
	}

	/** Coloca um bloco que some sozinho. Só substitui ar, água e plantas moles. */
	public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
		BlockState current = level.getBlockState(pos);
		if (!(current.isAir() || current.is(Blocks.WATER) || current.canBeReplaced())) {
			return false;
		}
		Map<Long, Entry> map = ALL.computeIfAbsent(level.dimension().identifier(), k -> new HashMap<>());
		if (map.containsKey(pos.asLong())) {
			return false;
		}
		map.put(pos.asLong(), new Entry(current, level.getGameTime() + ticks));
		level.setBlock(pos, state, 3);
		return true;
	}

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % 10 != 0) {
			return;
		}
		Map<Long, Entry> map = ALL.get(level.dimension().identifier());
		if (map == null || map.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		Iterator<Map.Entry<Long, Entry>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Entry> entry = it.next();
			if (now >= entry.getValue().expires()) {
				level.setBlock(BlockPos.of(entry.getKey()), entry.getValue().original(), 3);
				it.remove();
			}
		}
	}
}
