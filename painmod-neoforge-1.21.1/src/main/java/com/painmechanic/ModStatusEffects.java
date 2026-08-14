package com.painmechanic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStatusEffects {
	public static final DeferredRegister<MobEffect> EFFECTS =
		DeferredRegister.create(Registries.MOB_EFFECT, PainMechanic.MOD_ID);
	public static final DeferredHolder<MobEffect, PainReliefStatusEffect> PAIN_RELIEF = EFFECTS.register("pain_relief",
		() -> new PainReliefStatusEffect(MobEffectCategory.BENEFICIAL, 0xFF9BB8));
	public static final DeferredHolder<MobEffect, AdrenalineStatusEffect> ADRENALINE = EFFECTS.register("adrenaline",
		() -> new AdrenalineStatusEffect(MobEffectCategory.BENEFICIAL, 0xFF4B1F));
	public static final DeferredHolder<MobEffect, AdrenalineCooldownStatusEffect> ADRENALINE_COOLDOWN =
		EFFECTS.register("adrenaline_cooldown",
			() -> new AdrenalineCooldownStatusEffect(MobEffectCategory.NEUTRAL, 0x552B2B));

	private ModStatusEffects() {
	}

	public static void register(IEventBus modEventBus) {
		EFFECTS.register(modEventBus);
	}
}
