package com.painmechanic.mixin;

import com.painmechanic.PainSystem;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 休克状态下禁止跳跃（ServerPlayerEntity 重写了 jump，需直接注入这里）。
 */
@Mixin(LivingEntity.class)
public abstract class PlayerJumpMixin {
	@Inject(method = "jump", at = @At("HEAD"), cancellable = true)
	private void painmod$blockJump(CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity player && PainSystem.isInShock(player)) {
			ci.cancel();
		}
	}
}
