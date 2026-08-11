package com.painmechanic;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModStatusEffects {
	public static StatusEffect PAIN_RELIEF;
	public static StatusEffect ADRENALINE;
	public static StatusEffect ADRENALINE_COOLDOWN;

	private ModStatusEffects() {
	}

	public static void register() {
		PAIN_RELIEF = Registry.register(Registries.STATUS_EFFECT, PainMechanic.id("pain_relief"),
			new PainReliefStatusEffect(StatusEffectCategory.BENEFICIAL, 0xFF9BB8));
		ADRENALINE = Registry.register(Registries.STATUS_EFFECT, PainMechanic.id("adrenaline"),
			new AdrenalineStatusEffect(StatusEffectCategory.BENEFICIAL, 0xFF4B1F));
		ADRENALINE_COOLDOWN = Registry.register(Registries.STATUS_EFFECT, PainMechanic.id("adrenaline_cooldown"),
			new AdrenalineCooldownStatusEffect(StatusEffectCategory.NEUTRAL, 0x552B2B));
	}
}
