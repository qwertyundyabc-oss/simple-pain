package com.painmechanic;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PainSyncPayload(float pain, float maxHealth, boolean dying, int shockRemainingTicks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PainSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(PainMechanic.id("pain_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PainSyncPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, PainSyncPayload::pain,
		ByteBufCodecs.FLOAT, PainSyncPayload::maxHealth,
		ByteBufCodecs.BOOL, PainSyncPayload::dying,
		ByteBufCodecs.VAR_INT, PainSyncPayload::shockRemainingTicks,
		PainSyncPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
