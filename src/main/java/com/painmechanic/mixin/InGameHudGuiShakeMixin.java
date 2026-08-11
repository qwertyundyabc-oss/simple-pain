package com.painmechanic.mixin;

import com.painmechanic.client.PainHud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在统一 GUI DrawContext 生成后开始抖动，覆盖标准 Screen 和大多数模组 GUI。
 */
@Mixin(InGameHud.class)
public abstract class InGameHudGuiShakeMixin {
	@Inject(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/DrawContext;createNewRootLayer()V", shift = At.Shift.AFTER))
	private void painmod$beginGuiShake(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		PainHud.beginGlobalGuiShake(context);
	}
}
