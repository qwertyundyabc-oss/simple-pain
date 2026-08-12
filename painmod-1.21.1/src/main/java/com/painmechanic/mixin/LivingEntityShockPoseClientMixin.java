package com.painmechanic.mixin;

import com.painmechanic.client.PainClientState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the local player's rendered shock pose stable after client pose updates. */
@Mixin(PlayerEntity.class)
public abstract class LivingEntityShockPoseClientMixin {
	private boolean painmod$isLocalPlayerInShock() {
		return (Object) this instanceof ClientPlayerEntity player
			&& player == MinecraftClient.getInstance().player
			&& PainClientState.pain > Math.max(1f, PainClientState.maxHealth);
	}

	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void painmod$keepLocalShockSwimming(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			((ClientPlayerEntity) (Object) this).setSwimming(true);
			ci.cancel();
		}
	}

	@Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
	private void painmod$keepLocalShockPose(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			((ClientPlayerEntity) (Object) this).setPose(EntityPose.SWIMMING);
			ci.cancel();
		}
	}
}
