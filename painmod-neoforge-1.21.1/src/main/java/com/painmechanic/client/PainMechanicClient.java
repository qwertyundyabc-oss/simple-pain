package com.painmechanic.client;

import com.painmechanic.PainMechanic;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class PainMechanicClient {
	private PainMechanicClient() {
	}

	public static void init(IEventBus modEventBus) {
		modEventBus.addListener(PainMechanicClient::registerGuiLayers);
		NeoForge.EVENT_BUS.addListener(PainMechanicClient::onClientTick);
	}

	private static void registerGuiLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(PainMechanic.id("pain_bar"),
			(guiGraphics, deltaTracker) -> PainHud.render(guiGraphics));
		event.registerAboveAll(PainMechanic.id("pain_vignette"),
			(guiGraphics, deltaTracker) -> PainHud.renderVignette(guiGraphics));
		event.registerAboveAll(PainMechanic.id("pain_dying_overlay"),
			(guiGraphics, deltaTracker) -> PainHud.renderDyingOverlay(guiGraphics));
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		PainClientTicker.tick(Minecraft.getInstance());
	}
}
