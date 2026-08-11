package com.painmechanic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PainNetworking {
	private static final Map<UUID, PainSyncPayload> LAST_SENT = new HashMap<>();

	private PainNetworking() {
	}

	public static void registerServer() {
		// 1.20.4 使用 FabricPacket 通道，无需额外注册。
	}

	public static void sendTo(ServerPlayerEntity player) {
		sendTo(player, false);
	}

	public static void sendImmediate(ServerPlayerEntity player) {
		sendTo(player, true);
	}

	private static void sendTo(ServerPlayerEntity player, boolean force) {
		UUID id = player.getUuid();
		PainSyncPayload payload = new PainSyncPayload(PainData.get(id), player.getMaxHealth(),
			PainSystem.isDying(player), PainSystem.getShockRemainingTicks(player));
		PainSyncPayload previous = LAST_SENT.get(id);
		boolean shockChanged = previous != null
			&& ((previous.pain() > previous.maxHealth()) != (payload.pain() > payload.maxHealth())
				|| previous.dying() != payload.dying()
				|| (previous.shockRemainingTicks() + 19) / 20 != (payload.shockRemainingTicks() + 19) / 20);
		boolean changedEnough = previous == null
			|| Math.abs(previous.pain() - payload.pain()) >= 0.05f
			|| Math.abs(previous.maxHealth() - payload.maxHealth()) >= 0.01f
			|| (payload.pain() <= 0f && previous.pain() > 0f);
		if (!force && !shockChanged && !changedEnough) {
			return;
		}
		LAST_SENT.put(id, payload);
		ServerPlayNetworking.send(player, payload);
	}

	public static void clear(UUID id) {
		LAST_SENT.remove(id);
	}

	public static void clearAll() {
		LAST_SENT.clear();
	}
}
