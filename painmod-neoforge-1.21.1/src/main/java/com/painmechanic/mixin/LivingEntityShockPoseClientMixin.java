package com.painmechanic.mixin;

import com.painmechanic.client.PainClientState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the local player's rendered shock pose stable after client pose updates. */
@Mixin(Player.class)
public abstract class LivingEntityShockPoseClientMixin {
	private boolean painmod$isLocalPlayerInShock() {
		return (Object) this instanceof LocalPlayer player
			&& player == Minecraft.getInstance().player
			&& PainClientState.pain > Math.max(1f, PainClientState.maxHealth)
			&& !PainClientState.adrenalineActive;
	}

	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void painmod$keepLocalShockSwimming(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			((LocalPlayer) (Object) this).setSwimming(true);
			ci.cancel();
		}
	}

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void painmod$keepLocalShockPose(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			((LocalPlayer) (Object) this).setPose(Pose.SWIMMING);
			ci.cancel();
		}
	}
}
