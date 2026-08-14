package com.painmechanic;

import java.util.function.Supplier;

import com.painmechanic.client.PainConfigScreen;
import com.painmechanic.client.PainMechanicClient;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PainMechanic.MOD_ID)
public class PainMechanic {
	public static final String MOD_ID = "pain_mechanic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	public PainMechanic(IEventBus modEventBus) {
		PainConfig.load();
		ModStatusEffects.register(modEventBus);
		ModItems.register(modEventBus);
		ModSounds.register(modEventBus);
		PainNetworking.register(modEventBus);
		PainSystem.register();
		// RegisterGuiLayersEvent fires during Minecraft.<init> (via ClientHooks.initClientHooks),
		// before FMLClientSetupEvent, so client listeners must be registered here.
		if (FMLEnvironment.dist == Dist.CLIENT) {
			PainMechanicClient.init(modEventBus);
			Supplier<IConfigScreenFactory> configScreenFactory =
				() -> (modContainer, screen) -> PainConfigScreen.create(screen);
			ModList.get().getModContainerById(MOD_ID)
				.ifPresent(container -> container.registerExtensionPoint(IConfigScreenFactory.class, configScreenFactory));
		}
		LOGGER.info("[Simple Pain] 疼痛机制已加载");
	}
}