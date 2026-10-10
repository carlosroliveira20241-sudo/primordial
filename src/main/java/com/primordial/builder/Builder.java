package com.primordial.builder;

import com.primordial.creature.Creatures;
import com.primordial.registry.PrimordialAttachments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/** Regras da bolsa de blocos: o que pode ser carregado, quanto cabe e como os blocos viram construção. */
public final class Builder {
	public static final int BASE_CAPACITY = 32;

	private Builder() {
	}

	public static BlockPouch pouch(Player player) {
		return player.getAttachedOrElse(PrimordialAttachments.POUCH, BlockPouch.DEFAULT);
	}

	/** A bolsa cresce com o nível da parte que dá "sand_pouch". */
	public static int capacity(Player player) {
		return BASE_CAPACITY + 64 * Creatures.passiveLevel(player, "sand_pouch");
	}

	/** Só materiais moles do fundo do mar: areia, cascalho, argila, lama e terra. */
	public static boolean canCarry(BlockState state) {
		return state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.MUD) || state.is(BlockTags.DIRT);
	}

	/** Areia e cascalho caem; para a construção ficar no lugar usamos o equivalente compactado. */
	public static BlockState buildState(Block block) {
		if (block == Blocks.SAND) {
			return Blocks.SANDSTONE.defaultBlockState();
		}
		if (block == Blocks.RED_SAND) {
			return Blocks.RED_SANDSTONE.defaultBlockState();
		}
		if (block == Blocks.GRAVEL) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		return block.defaultBlockState();
	}

	/** Coloca na bolsa. Retorna quantos couberam. */
	public static int add(ServerPlayer player, Block block, int amount) {
		BlockPouch pouch = pouch(player);
		int room = Math.max(0, capacity(player) - pouch.total());
		int added = Math.min(room, amount);
		if (added > 0) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(block);
			player.setAttached(PrimordialAttachments.POUCH, pouch.with(id, pouch.count(id) + added));
		}
		return added;
	}

	/** O tipo de bloco que o jogador mais tem (areia ganha o desempate), ou nulo se a bolsa está vazia. */
	public static @Nullable Block mostStocked(Player player) {
		BlockPouch pouch = pouch(player);
		Block best = null;
		int bestCount = 0;
		for (var entry : pouch.blocks().entrySet()) {
			int count = entry.getValue();
			Block block = BuiltInRegistries.BLOCK.getValue(entry.getKey());
			if (count > bestCount || (count == bestCount && block == Blocks.SAND)) {
				best = block;
				bestCount = count;
			}
		}
		return best;
	}

	/** Tira 1 bloco do tipo dado. */
	public static boolean take(ServerPlayer player, Block block) {
		BlockPouch pouch = pouch(player);
		Identifier id = BuiltInRegistries.BLOCK.getKey(block);
		int count = pouch.count(id);
		if (count <= 0) {
			return false;
		}
		player.setAttached(PrimordialAttachments.POUCH, pouch.with(id, count - 1));
		return true;
	}

	/** O bloco que o jogador está olhando, até {@code range} blocos. */
	public static @Nullable BlockHitResult looking(ServerPlayer player, double range) {
		HitResult hit = player.pick(range, 0.0F, false);
		return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block : null;
	}
}
