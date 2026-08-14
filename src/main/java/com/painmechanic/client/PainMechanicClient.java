package com.painmechanic.client;

import com.painmechanic.PainMechanic;
import com.painmechanic.PainSyncPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
public class PainMechanicClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PainClientState.reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PainClientTicker.reset(client));
		ClientPlayNetworking.registerGlobalReceiver(PainSyncPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				PainClientState.pain = payload.pain();
				PainClientState.maxHealth = payload.maxHealth();
				PainClientState.dying = payload.dying();
				PainClientState.shockRemainingTicks = payload.shockRemainingTicks();
				PainHud.onPainImpact(payload.painImpact(), payload.maxHealth());
			});
		});
		ClientTickEvents.END_CLIENT_TICK.register(PainClientTicker::tick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, PainMechanic.id("pain_bar"), new PainHud());
		// 有界面（背包等）时由 PainHud 自身跳过，避免覆盖其他模组的 GUI。
		HudElementRegistry.addLast(PainMechanic.id("pain_vignette"),
			(context, tickCounter) -> PainHud.renderVignette(context));
		HudElementRegistry.addLast(PainMechanic.id("dying_overlay"),
			(context, tickCounter) -> PainHud.renderDyingOverlay(context));
	}
}
