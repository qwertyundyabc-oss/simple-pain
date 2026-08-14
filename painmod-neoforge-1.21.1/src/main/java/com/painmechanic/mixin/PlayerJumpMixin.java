package com.painmechanic.mixin;

import com.painmechanic.PainSystem;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 休克状态下禁止跳跃（ServerPlayer 重写了 jump，需直接注入这里）。
 */
@Mixin(LivingEntity.class)
public abstract class PlayerJumpMixin {
	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void painmod$blockJump(CallbackInfo ci) {
		if ((Object) this instanceof Player player && PainSystem.isInShock(player)) {
			ci.cancel();
		}
	}
}
