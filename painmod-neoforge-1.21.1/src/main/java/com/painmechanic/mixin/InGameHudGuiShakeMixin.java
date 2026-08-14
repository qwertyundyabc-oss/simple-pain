package com.painmechanic.mixin;

import com.painmechanic.client.PainHud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies GUI shake around the vanilla HUD render pass. */
@Mixin(Gui.class)
public abstract class InGameHudGuiShakeMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void painmod$beginGuiShake(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
		PainHud.beginGlobalGuiShake(context);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void painmod$endGuiShake(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
		PainHud.endGlobalGuiShake();
	}
}
