package com.painmechanic.mixin;

import com.painmechanic.client.PainClientState;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端：本地玩家休克时禁止跳跃（阻止客户端预测跳起后再被纠正）。
 */
@Mixin(LivingEntity.class)
public abstract class PlayerJumpClientMixin {
	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void painmod$blockClientJump(CallbackInfo ci) {
		if (FMLEnvironment.dist == Dist.CLIENT
			&& (Object) this == Minecraft.getInstance().player
			&& PainClientState.pain > PainClientState.maxHealth
			&& !PainClientState.adrenalineActive) {
			ci.cancel();
		}
	}
}
