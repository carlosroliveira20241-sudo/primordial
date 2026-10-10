package com.primordial.registry;

import com.primordial.Primordial;
import com.primordial.creature.CreatureBody;
import com.primordial.creature.CreatureProgress;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class PrimordialAttachments {
	/** Corpo e aparência: todo mundo recebe (para desenhar sua criatura). */
	public static final AttachmentType<CreatureBody> BODY = AttachmentRegistry.<CreatureBody>builder()
		.persistent(CreatureBody.CODEC)
		.copyOnDeath()
		.initializer(() -> CreatureBody.DEFAULT)
		.syncWith(ByteBufCodecs.fromCodec(CreatureBody.CODEC), AttachmentSyncPredicate.all())
		.buildAndRegister(Primordial.id("body"));

	/** DNA, nível, desbloqueios: só o próprio jogador recebe. */
	public static final AttachmentType<CreatureProgress> PROGRESS = AttachmentRegistry.<CreatureProgress>builder()
		.persistent(CreatureProgress.CODEC)
		.copyOnDeath()
		.initializer(() -> CreatureProgress.DEFAULT)
		.syncWith(ByteBufCodecs.fromCodec(CreatureProgress.CODEC), AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(Primordial.id("progress"));

	/** Mutações escolhidas e a próxima oferta: só o próprio jogador precisa ver. */
	public static final AttachmentType<com.primordial.mutation.MutationState> MUTATIONS = AttachmentRegistry.<com.primordial.mutation.MutationState>builder()
		.persistent(com.primordial.mutation.MutationState.CODEC)
		.copyOnDeath()
		.initializer(() -> com.primordial.mutation.MutationState.DEFAULT)
		.syncWith(ByteBufCodecs.fromCodec(com.primordial.mutation.MutationState.CODEC), AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(Primordial.id("mutations"));

	private PrimordialAttachments() {
	}

	public static void init() {
	}
}
