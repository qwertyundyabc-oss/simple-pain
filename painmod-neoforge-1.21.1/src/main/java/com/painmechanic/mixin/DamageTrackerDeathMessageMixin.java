package com.painmechanic.mixin;

import com.painmechanic.PainSystem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces only the death message produced by an expired shock timer. */
@Mixin(CombatTracker.class)
public abstract class DamageTrackerDeathMessageMixin {
	@Shadow @Final private LivingEntity mob;

	@Inject(method = "getDeathMessage", at = @At("HEAD"), cancellable = true)
	private void painmod$useShockDeathMessage(CallbackInfoReturnable<Component> cir) {
		if (mob instanceof ServerPlayer player && PainSystem.isShockDeathInProgress(player)) {
			cir.setReturnValue(Component.translatable("pain_mechanic.death.shock", player.getDisplayName()));
		}
	}
}
