package com.painmechanic.mixin;

import com.painmechanic.client.PainHud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies GUI shake around the vanilla HUD render pass. */
@Mixin(InGameHud.class)
public abstract class InGameHudGuiShakeMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void painmod$beginGuiShake(DrawContext context, float tickDelta, CallbackInfo ci) {
		PainHud.beginGlobalGuiShake(context);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void painmod$endGuiShake(DrawContext context, float tickDelta, CallbackInfo ci) {
		PainHud.endGlobalGuiShake();
	}
}