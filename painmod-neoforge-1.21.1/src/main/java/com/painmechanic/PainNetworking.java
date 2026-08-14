package com.painmechanic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.painmechanic.client.PainClientState;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PainNetworking {
	private static final Map<UUID, PainSyncPayload> LAST_SENT = new HashMap<>();

	private PainNetworking() {
	}

	public static void register(IEventBus modEventBus) {
		modEventBus.addListener(PainNetworking::onRegisterPayloads);
	}

	private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(PainSyncPayload.TYPE, PainSyncPayload.STREAM_CODEC, PainNetworking::onClientPayload);
	}

	private static void onClientPayload(PainSyncPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			PainClientState.pain = payload.pain();
			PainClientState.maxHealth = payload.maxHealth();
			PainClientState.dying = payload.dying();
			PainClientState.shockRemainingTicks = payload.shockRemainingTicks();
		});
	}

	public static void sendTo(ServerPlayer player) {
		sendTo(player, false);
	}

	public static void sendImmediate(ServerPlayer player) {
		sendTo(player, true);
	}

	private static void sendTo(ServerPlayer player, boolean force) {
		UUID id = player.getUUID();
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
		PacketDistributor.sendToPlayer(player, payload);
	}

	public static void clear(UUID id) {
		LAST_SENT.remove(id);
	}

	public static void clearAll() {
		LAST_SENT.clear();
	}
}
