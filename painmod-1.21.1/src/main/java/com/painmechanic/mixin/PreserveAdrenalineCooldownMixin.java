package com.painmechanic.mixin;

import java.util.ArrayList;

import com.painmechanic.ModStatusEffects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PreserveAdrenalineCooldownMixin {
	@Inject(method = "clearStatusEffects", at = @At("HEAD"), cancellable = true)
	private void painmod$preserveCooldown(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.hasStatusEffect(ModStatusEffects.ADRENALINE_COOLDOWN)) {
			return;
		}

		boolean changed = false;
		for (RegistryEntry<StatusEffect> effect : new ArrayList<>(entity.getActiveStatusEffects().keySet())) {
			if (!effect.equals(ModStatusEffects.ADRENALINE_COOLDOWN)) {
				changed |= entity.removeStatusEffect(effect);
			}
		}
		cir.setReturnValue(changed);
	}
}
