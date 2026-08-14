package com.painmechanic.mixin;

import com.painmechanic.client.PainClientState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 休克时阻止 vanilla 的站起逻辑（aiStep 内按跳跃/自动站起），tick 末尾强制恢复游泳姿势。 */
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerShockMixin {
	private boolean painmod$isLocalPlayerInShock() {
		return (Object) this == Minecraft.getInstance().player
			&& PainClientState.pain > Math.max(1f, PainClientState.maxHealth)
			&& !PainClientState.adrenalineActive;
	}

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void painmod$clearJumpInputWhileShock(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			((LocalPlayer) (Object) this).input.jumping = false;
		}
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void painmod$restoreShockPoseAfterMovement(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			((LocalPlayer) (Object) this).setSwimming(true);
			((LocalPlayer) (Object) this).setPose(Pose.SWIMMING);
		}
	}
}
