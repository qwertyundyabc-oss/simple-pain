package com.painmechanic.mixin;

import com.painmechanic.client.PainHud;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在统一 GUI 提取阶段包住 DrawContext，兼容大多数原版和模组 Screen。
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererGuiShakeMixin {
	@Inject(method = "render",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private void painmod$endGuiShake(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
		PainHud.endGlobalGuiShake();
	}
}
