package com.painmechanic;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record PainSyncPayload(float pain, float maxHealth, boolean dying, int shockRemainingTicks,
		float painImpact) implements CustomPayload {
	public static final CustomPayload.Id<PainSyncPayload> ID = new CustomPayload.Id<>(PainMechanic.id("pain_sync"));
	public static final PacketCodec<RegistryByteBuf, PainSyncPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.FLOAT, PainSyncPayload::pain,
		PacketCodecs.FLOAT, PainSyncPayload::maxHealth,
		PacketCodecs.BOOLEAN, PainSyncPayload::dying,
		PacketCodecs.VAR_INT, PainSyncPayload::shockRemainingTicks,
		PacketCodecs.FLOAT, PainSyncPayload::painImpact,
		PainSyncPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
