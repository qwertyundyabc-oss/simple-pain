package com.painmechanic.mixin;

import com.painmechanic.PainSystem;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces only the death message produced by an expired shock timer. */
@Mixin(DamageTracker.class)
public abstract class DamageTrackerDeathMessageMixin {
	@Shadow @Final private LivingEntity entity;

	@Inject(method = "getDeathMessage", at = @At("HEAD"), cancellable = true)
	private void painmod$useShockDeathMessage(CallbackInfoReturnable<Text> cir) {
		if (entity instanceof ServerPlayerEntity player && PainSystem.isShockDeathInProgress(player)) {
			cir.setReturnValue(Text.translatable("pain_mechanic.death.shock", player.getDisplayName()));
		}
	}
}
