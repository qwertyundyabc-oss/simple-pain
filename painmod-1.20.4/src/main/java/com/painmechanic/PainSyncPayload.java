package com.painmechanic;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

public final class PainSyncPayload implements FabricPacket {
	public static final PacketType<PainSyncPayload> TYPE =
		PacketType.create(PainMechanic.id("pain_sync"), PainSyncPayload::new);

	private final float pain;
	private final float maxHealth;
	private final boolean dying;
	private final int shockRemainingTicks;

	public PainSyncPayload(float pain, float maxHealth, boolean dying, int shockRemainingTicks) {
		this.pain = pain;
		this.maxHealth = maxHealth;
		this.dying = dying;
		this.shockRemainingTicks = shockRemainingTicks;
	}

	private PainSyncPayload(PacketByteBuf buf) {
		this(buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readVarInt());
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeFloat(pain);
		buf.writeFloat(maxHealth);
		buf.writeBoolean(dying);
		buf.writeVarInt(shockRemainingTicks);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}

	public float pain() {
		return pain;
	}

	public float maxHealth() {
		return maxHealth;
	}

	public boolean dying() {
		return dying;
	}

	public int shockRemainingTicks() {
		return shockRemainingTicks;
	}
}
