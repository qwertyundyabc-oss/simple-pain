package com.painmechanic.mixin;

import java.util.ArrayList;

import com.painmechanic.ModStatusEffects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PreserveAdrenalineCooldownMixin {
	@Inject(method = "removeAllEffects", at = @At("HEAD"), cancellable = true)
	private void painmod$preserveCooldown(CallbackInfoReturnable<Boolean> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.hasEffect(ModStatusEffects.ADRENALINE_COOLDOWN)) {
			return;
		}

		boolean changed = false;
		for (Holder<MobEffect> effect : new ArrayList<>(entity.getActiveEffectsMap().keySet())) {
			if (!effect.equals(ModStatusEffects.ADRENALINE_COOLDOWN)) {
				changed |= entity.removeEffect(effect);
			}
		}
		cir.setReturnValue(changed);
	}
}
