package com.painmechanic.mixin;

import com.painmechanic.client.PainHud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Direct fallback for mod screens that render outside the HUD path. */
@Mixin(Screen.class)
public abstract class ScreenGuiShakeMixin {
	@Inject(method = "renderWithTooltip", at = @At("HEAD"))
	private void painmod$beginScreenShake(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (Minecraft.getInstance().screen instanceof PauseScreen) {
			return;
		}
		PainHud.beginScreenGuiShake(context);
	}

	@Inject(method = "renderWithTooltip", at = @At("RETURN"))
	private void painmod$endScreenShake(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		PainHud.endScreenGuiShake(context);
	}
}
