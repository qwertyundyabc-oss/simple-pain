package com.painmechanic.mixin;

import com.painmechanic.client.PainClientState;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端：本地玩家休克时禁止跳跃（阻止客户端预测跳起后再被纠正）。
 */
@Mixin(LivingEntity.class)
public abstract class PlayerJumpClientMixin {
	@Inject(method = "jump", at = @At("HEAD"), cancellable = true)
	private void painmod$blockClientJump(CallbackInfo ci) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
			&& (Object) this == MinecraftClient.getInstance().player
			&& PainClientState.pain > PainClientState.maxHealth) {
			ci.cancel();
		}
	}
}
