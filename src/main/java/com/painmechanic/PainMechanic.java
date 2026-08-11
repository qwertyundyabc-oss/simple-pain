package com.painmechanic;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PainMechanic implements ModInitializer {
	public static final String MOD_ID = "pain_mechanic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PainConfig.load();
		ModStatusEffects.register();
		ModItems.register();
		ModSounds.register();
		PainNetworking.registerServer();
		PainSystem.register();
		LOGGER.info("[Simple Pain] 疼痛机制已加载");
	}
}
