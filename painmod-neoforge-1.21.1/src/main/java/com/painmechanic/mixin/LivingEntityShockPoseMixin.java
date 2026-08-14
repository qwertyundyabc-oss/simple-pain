package com.painmechanic.mixin;

import com.painmechanic.PainSystem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the server-side shock pose from being reset by vanilla pose updates. */
@Mixin(Player.class)
public abstract class LivingEntityShockPoseMixin {
	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void painmod$keepShockSwimming(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && PainSystem.isInShock(player)) {
			player.setSwimming(true);
			ci.cancel();
		}
	}

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void painmod$keepShockPose(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && PainSystem.isInShock(player)) {
			player.setPose(Pose.SWIMMING);
			ci.cancel();
		}
	}
}
