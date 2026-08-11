package com.painmechanic.mixin;

import com.painmechanic.client.PainHud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Direct fallback for mod screens that render outside the HUD path. */
@Mixin(Screen.class)
public abstract class ScreenGuiShakeMixin {
	@Inject(method = "renderWithTooltip", at = @At("HEAD"))
	private void painmod$beginScreenShake(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (MinecraftClient.getInstance().currentScreen instanceof GameMenuScreen) {
			return;
		}
		PainHud.beginScreenGuiShake(context);
	}

	@Inject(method = "renderWithTooltip", at = @At("RETURN"))
	private void painmod$endScreenShake(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		PainHud.endScreenGuiShake(context);
	}
}