package com.painmechanic.mixin;

import com.painmechanic.PainSystem;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * 捕获 applyDamage 中护甲/魔抗减免后的最终伤害（未封顶、吸收前），
 * 供濒死保护的疼痛计算使用。
 */
@Mixin(LivingEntity.class)
public abstract class DamageCaptureMixin {
	@Inject(method = "applyDamage",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getAbsorptionAmount()F", ordinal = 0),
		locals = LocalCapture.CAPTURE_FAILHARD)
	private void painmod$captureFinalDamage(DamageSource source, float amount,
		CallbackInfo ci, float finalDamage) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			PainSystem.setFinalDamage(player.getUuid(), finalDamage);
		}
	}

	@Inject(method = "damage", at = @At("TAIL"))
	private void painmod$afterDamageApplied(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			PainSystem.onAfterDamageApplied(player, amount);
		}
	}
}
