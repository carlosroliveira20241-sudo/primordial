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

	/** Diário: bestiário, contadores e desafios cumpridos. */
	public static final AttachmentType<com.primordial.journal.JournalState> JOURNAL = AttachmentRegistry.<com.primordial.journal.JournalState>builder()
		.persistent(com.primordial.journal.JournalState.CODEC)
		.copyOnDeath()
		.initializer(() -> com.primordial.journal.JournalState.DEFAULT)
		.syncWith(ByteBufCodecs.fromCodec(com.primordial.journal.JournalState.CODEC), AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(Primordial.id("journal"));

	/** Ordem das habilidades escolhida pelo jogador (Z, X, ...). */
	public static final AttachmentType<com.primordial.ability.AbilityOrder> ABILITY_ORDER = AttachmentRegistry.<com.primordial.ability.AbilityOrder>builder()
		.persistent(com.primordial.ability.AbilityOrder.CODEC)
		.copyOnDeath()
		.initializer(() -> com.primordial.ability.AbilityOrder.DEFAULT)
		.syncWith(ByteBufCodecs.fromCodec(com.primordial.ability.AbilityOrder.CODEC), AttachmentSyncPredicate.targetOnly())
		.buildAndRegister(Primordial.id("ability_order"));

	/** Inteligência: cérebro, habilidades compradas e slots. Todos veem (o cérebro cresce no corpo). */
	public static final AttachmentType<com.primordial.intellect.IntellectState> INTELLECT = AttachmentRegistry.<com.primordial.intellect.IntellectState>builder()
		.persistent(com.primordial.intellect.IntellectState.CODEC)
		.copyOnDeath()
		.initializer(() -> com.primordial.intellect.IntellectState.DEFAULT)
		.syncWith(ByteBufCodecs.fromCodec(com.primordial.intellect.IntellectState.CODEC), AttachmentSyncPredicate.all())
		.buildAndRegister(Primordial.id("intellect"));

	private PrimordialAttachments() {
	}

	public static void init() {
	}
}
