package com.primordial.net;

import com.primordial.Primordial;
import com.primordial.creature.BodySlot;
import com.primordial.creature.Origin;
import com.primordial.creature.Pattern;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Todos os pacotes do mod. C2S = cliente → servidor, S2C = servidor → cliente. */
public final class Payloads {
	private Payloads() {
	}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String name) {
		return new CustomPacketPayload.Type<>(Primordial.id(name));
	}

	public static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(E[] values) {
		return ByteBufCodecs.VAR_INT.map(i -> values[Math.floorMod(i, values.length)], Enum::ordinal);
	}

	// ---------------------------------------------------------------- C2S

	public record ChooseOrigin(Origin origin, String speciesName, int primary, int secondary, Pattern pattern, Identifier look) implements CustomPacketPayload {
		public static final Type<ChooseOrigin> TYPE = typeOf("choose_origin");
		public static final StreamCodec<ByteBuf, ChooseOrigin> CODEC = StreamCodec.composite(
			enumCodec(Origin.values()), ChooseOrigin::origin,
			ByteBufCodecs.stringUtf8(32), ChooseOrigin::speciesName,
			ByteBufCodecs.INT, ChooseOrigin::primary,
			ByteBufCodecs.INT, ChooseOrigin::secondary,
			enumCodec(Pattern.values()), ChooseOrigin::pattern,
			Identifier.STREAM_CODEC, ChooseOrigin::look,
			ChooseOrigin::new
		);

		@Override
		public Type<ChooseOrigin> type() {
			return TYPE;
		}
	}

	public record EquipPart(BodySlot slot, Optional<Identifier> part) implements CustomPacketPayload {
		public static final Type<EquipPart> TYPE = typeOf("equip_part");
		public static final StreamCodec<ByteBuf, EquipPart> CODEC = StreamCodec.composite(
			enumCodec(BodySlot.values()), EquipPart::slot,
			ByteBufCodecs.optional(Identifier.STREAM_CODEC), EquipPart::part,
			EquipPart::new
		);

		@Override
		public Type<EquipPart> type() {
			return TYPE;
		}
	}

	public record Evolve() implements CustomPacketPayload {
		public static final Type<Evolve> TYPE = typeOf("evolve");
		public static final StreamCodec<ByteBuf, Evolve> CODEC = StreamCodec.unit(new Evolve());

		@Override
		public Type<Evolve> type() {
			return TYPE;
		}
	}

	public record SetLook(int primary, int secondary, int detail, Pattern pattern, Identifier look) implements CustomPacketPayload {
		public static final Type<SetLook> TYPE = typeOf("set_look");
		public static final StreamCodec<ByteBuf, SetLook> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, SetLook::primary,
			ByteBufCodecs.INT, SetLook::secondary,
			ByteBufCodecs.INT, SetLook::detail,
			enumCodec(Pattern.values()), SetLook::pattern,
			Identifier.STREAM_CODEC, SetLook::look,
			SetLook::new
		);

		@Override
		public Type<SetLook> type() {
			return TYPE;
		}
	}

	public record UseAbility(int slot) implements CustomPacketPayload {
		public static final Type<UseAbility> TYPE = typeOf("use_ability");
		public static final StreamCodec<ByteBuf, UseAbility> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, UseAbility::slot, UseAbility::new);

		@Override
		public Type<UseAbility> type() {
			return TYPE;
		}
	}

	public record EatPlant(BlockPos pos) implements CustomPacketPayload {
		public static final Type<EatPlant> TYPE = typeOf("eat_plant");
		public static final StreamCodec<ByteBuf, EatPlant> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, EatPlant::pos, EatPlant::new);

		@Override
		public Type<EatPlant> type() {
			return TYPE;
		}
	}

	public record MakeNest() implements CustomPacketPayload {
		public static final Type<MakeNest> TYPE = typeOf("make_nest");
		public static final StreamCodec<ByteBuf, MakeNest> CODEC = StreamCodec.unit(new MakeNest());

		@Override
		public Type<MakeNest> type() {
			return TYPE;
		}
	}

	// ---------------------------------------------------------------- S2C

	public record OpenOrigin() implements CustomPacketPayload {
		public static final Type<OpenOrigin> TYPE = typeOf("open_origin");
		public static final StreamCodec<ByteBuf, OpenOrigin> CODEC = StreamCodec.unit(new OpenOrigin());

		@Override
		public Type<OpenOrigin> type() {
			return TYPE;
		}
	}

	/** Recargas restantes (em ticks) de cada habilidade, para o HUD. */
	public record Cooldowns(Map<Identifier, Integer> remaining) implements CustomPacketPayload {
		public static final Type<Cooldowns> TYPE = typeOf("cooldowns");
		public static final StreamCodec<ByteBuf, Cooldowns> CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT), Cooldowns::remaining,
			Cooldowns::new
		);

		@Override
		public Type<Cooldowns> type() {
			return TYPE;
		}
	}

	/** Aviso bonito na tela (parte nova, subiu de nível, Despertar...). */
	public record PartToast(Identifier part, int level, Kind kind) implements CustomPacketPayload {
		public static final Type<PartToast> TYPE = typeOf("part_toast");
		public static final StreamCodec<ByteBuf, PartToast> CODEC = StreamCodec.composite(
			Identifier.STREAM_CODEC, PartToast::part,
			ByteBufCodecs.VAR_INT, PartToast::level,
			enumCodec(Kind.values()), PartToast::kind,
			PartToast::new
		);

		public enum Kind {
			UNLOCKED,
			LEVEL_UP,
			AWAKENED,
			STOLEN
		}

		@Override
		public Type<PartToast> type() {
			return TYPE;
		}
	}

	/** Toque de evolução: o cliente mostra o título e a tela pisca. */
	public record Evolved(int stage) implements CustomPacketPayload {
		public static final Type<Evolved> TYPE = typeOf("evolved");
		public static final StreamCodec<ByteBuf, Evolved> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Evolved::stage, Evolved::new);

		@Override
		public Type<Evolved> type() {
			return TYPE;
		}
	}
}
