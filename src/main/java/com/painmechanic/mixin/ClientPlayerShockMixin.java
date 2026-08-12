package com.painmechanic.mixin;

import com.painmechanic.client.PainClientState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 休克时阻止 vanilla 的站起逻辑（tickMovement 内按跳跃/自动站起），tick 末尾强制恢复游泳姿势。 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerShockMixin {
	private boolean painmod$isLocalPlayerInShock() {
		return (Object) this == MinecraftClient.getInstance().player
			&& PainClientState.pain > Math.max(1f, PainClientState.maxHealth)
			&& !PainClientState.adrenalineActive;
	}

	@Inject(method = "tickMovement", at = @At("HEAD"))
	private void painmod$clearJumpInputWhileShock(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			Input input = ((ClientPlayerEntity) (Object) this).input;
			PlayerInput playerInput = input.playerInput;
			input.playerInput = new PlayerInput(
				playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(),
				false, playerInput.sneak(), playerInput.sprint());
		}
	}

	@Inject(method = "tickMovement", at = @At("TAIL"))
	private void painmod$restoreShockPoseAfterMovement(CallbackInfo ci) {
		if (painmod$isLocalPlayerInShock()) {
			((ClientPlayerEntity) (Object) this).setSwimming(true);
			((ClientPlayerEntity) (Object) this).setPose(EntityPose.SWIMMING);
		}
	}
}
