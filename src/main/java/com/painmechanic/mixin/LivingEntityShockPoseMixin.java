package com.painmechanic.mixin;

import com.painmechanic.PainSystem;

import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the server-side shock pose from being reset by vanilla pose updates. */
@Mixin(PlayerEntity.class)
public abstract class LivingEntityShockPoseMixin {
	@Inject(method = "updateSwimming", at = @At("TAIL"))
	private void painmod$keepShockSwimming(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayerEntity player && PainSystem.isInShock(player)) {
			player.setSwimming(true);
		}
	}

	@Inject(method = "updatePose", at = @At("TAIL"))
	private void painmod$keepShockPose(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayerEntity player && PainSystem.isInShock(player)) {
			player.setPose(EntityPose.SWIMMING);
		}
	}
}
