package com.painmechanic;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public final class ModStatusEffects {
	public static RegistryEntry<StatusEffect> PAIN_RELIEF;
	public static RegistryEntry<StatusEffect> ADRENALINE;
	public static RegistryEntry<StatusEffect> ADRENALINE_COOLDOWN;

	private ModStatusEffects() {
	}

	public static void register() {
		PAIN_RELIEF = Registry.registerReference(Registries.STATUS_EFFECT, PainMechanic.id("pain_relief"),
			new PainReliefStatusEffect(StatusEffectCategory.BENEFICIAL, 0xFF9BB8));
		ADRENALINE = Registry.registerReference(Registries.STATUS_EFFECT, PainMechanic.id("adrenaline"),
			new AdrenalineStatusEffect(StatusEffectCategory.BENEFICIAL, 0xFF4B1F));
		ADRENALINE_COOLDOWN = Registry.registerReference(Registries.STATUS_EFFECT, PainMechanic.id("adrenaline_cooldown"),
			new AdrenalineCooldownStatusEffect(StatusEffectCategory.NEUTRAL, 0x552B2B));
	}
}
