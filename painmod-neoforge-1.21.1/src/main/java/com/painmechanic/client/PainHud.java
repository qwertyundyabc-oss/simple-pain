package com.painmechanic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.painmechanic.ModStatusEffects;
import com.painmechanic.PainConfig;
import com.painmechanic.PainMechanic;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 原版风格疼痛条：单张贴图（背景帧 + 覆盖帧），按 8x8 段平铺、不拉伸，
 * 覆盖层按疼痛百分比逐段覆盖。
 */
public class PainHud {
	private static final ResourceLocation BACKGROUND_BAR_TEXTURE = PainMechanic.id("textures/hud/pain_bar_background.png");
	private static final ResourceLocation OVERLAY_BAR_TEXTURE = PainMechanic.id("textures/hud/pain_bar_overlay.png");
	private static final ResourceLocation VIGNETTE_TEXTURE = PainMechanic.id("textures/hud/red_vignette.png");
	private static final int BAR_WIDTH = 80;
	private static final int BAR_HEIGHT = 8;
	private static final float GUI_SHAKE_X_FREQUENCY = 82.0f;
	private static final float GUI_SHAKE_Y_FREQUENCY = 101.0f;
	private static GuiGraphics globalGuiContext;
	private static GuiGraphics screenGuiContext;

	/** 将当前 GUI 的渲染矩阵平移，抖动不影响鼠标坐标和交互判定。 */
	public static void pushGuiShake(GuiGraphics context) {
		context.pose().pushPose();
		float amplitude = guiShakeAmplitude();
		if (amplitude <= 0f) {
			return;
		}
		float time = (float) (Util.getMillis() / 1000.0);
		float x = (float) Math.sin(time * GUI_SHAKE_X_FREQUENCY) * amplitude;
		float y = (float) Math.cos(time * GUI_SHAKE_Y_FREQUENCY) * amplitude * 0.75f;
		context.pose().translate(x, y, 0.0f);
	}

	public static void popGuiShake(GuiGraphics context) {
		context.pose().popPose();
	}

	public static void beginGlobalGuiShake(GuiGraphics context) {
		if (Minecraft.getInstance().screen != null) {
			return;
		}
		if (globalGuiContext == context) {
			return;
		}
		pushGuiShake(context);
		globalGuiContext = context;
	}

	public static void endGlobalGuiShake() {
		if (globalGuiContext != null) {
			popGuiShake(globalGuiContext);
			globalGuiContext = null;
		}
	}

	/** Fallback for Screen implementations that do not pass through the HUD path. */
	public static void beginScreenGuiShake(GuiGraphics context) {
		if (globalGuiContext == context) {
			return;
		}
		pushGuiShake(context);
		screenGuiContext = context;
	}

	public static void endScreenGuiShake(GuiGraphics context) {
		if (screenGuiContext == context) {
			popGuiShake(context);
			screenGuiContext = null;
		}
	}

	private static float guiShakeAmplitude() {
		if (Minecraft.getInstance().player == null) {
			return 0f;
		}
		float maxHealth = Math.max(1f, PainClientState.maxHealth);
		float ratio = PainClientState.pain / maxHealth;
		float threshold = PainConfig.get().guiShakeThreshold;
		if (ratio <= threshold) {
			return 0f;
		}
		float intensity = (ratio - threshold) / (1f - threshold);
		intensity = Math.max(0f, Math.min(1f, intensity));
		return intensity * PainConfig.get().guiShakeMaxAmplitude;
	}

	/**
	 * 屏幕红晕：疼痛比例超过阈值后出现，越痛越红，并随时间脉动起伏。
	 */
	public static void renderVignette(GuiGraphics context) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null || !PainConfig.get().redVignetteEnabled) {
			return;
		}
		float pain = PainClientState.pain;
		if (pain <= 0f) {
			return;
		}
		float max = Math.max(1f, PainClientState.maxHealth);
		float ratio = pain / max;
		float threshold = PainConfig.get().redVignetteThreshold;
		if (ratio <= threshold) {
			return;
		}
		// 疼痛比例从阈值到 100%（休克）之间线性增强，越痛红晕越强
		float t = (ratio - threshold) / (1.0f - threshold);
		t = Math.max(0f, Math.min(1f, t));
		// 透明度只由疼痛强度决定
		context.setColor(1.0f, 1.0f, 1.0f, t);
		// 此起彼伏的脉动：1.6 秒一次，改变红晕大小（中央可见圆圈呼吸式放大缩小）
		float time = (float) (Util.getMillis() / 1000.0);
		float k = 0.5f + 0.5f * (0.5f + 0.5f * (float) Math.sin(time * 3.926990817f));
		int sw = context.guiWidth();
		int sh = context.guiHeight();
		int halfRegion = (int) (k * 128.0f);
		int region = (int) (k * 256.0f);
		// 1.21.x 普通 blit 不启用 alpha 混合，需手动开启，否则透明区域会变成实体色块
		// 红晕不参与 GUI 抖动：反向平移抵消抖动矩阵（NeoForge 层渲染栈多一层 push，无法用 popPose 精确弹出）
		float shakeAmp = guiShakeAmplitude();
		boolean shakeActive = shakeAmp > 0f && globalGuiContext == context;
		if (shakeActive) {
			float shakeTime = (float) (Util.getMillis() / 1000.0);
			float shakeX = (float) Math.sin(shakeTime * GUI_SHAKE_X_FREQUENCY) * shakeAmp;
			float shakeY = (float) Math.cos(shakeTime * GUI_SHAKE_Y_FREQUENCY) * shakeAmp * 0.75f;
			context.pose().pushPose();
			context.pose().translate(-shakeX, -shakeY, 0.0f);
		}
		RenderSystem.enableBlend();
		context.blit(VIGNETTE_TEXTURE,
			0, 0, sw, sh, (float) (128 - halfRegion), (float) (128 - halfRegion), region, region, 256, 256);
		context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		if (shakeActive) {
			context.pose().popPose();
		}
	}

	public static void renderDyingOverlay(GuiGraphics context) {
		float alpha = PainClientTicker.dyingOverlayAlpha();
		if (alpha <= 0f) {
			return;
		}
		int color = ((int) (alpha * 255f) & 0xFF) << 24;
		context.fill(0, 0, context.guiWidth(), context.guiHeight(), color);
	}

	public static void render(GuiGraphics context) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.screen != null) {
			return;
		}
		float pain = PainClientState.pain;
		if (pain <= 0f) {
			return;
		}
		float max = Math.max(1f, PainClientState.maxHealth);
		float painRatio = Math.max(0f, pain / max);
		float ratio = Math.min(1f, painRatio);
		boolean blinkVisible = isBlinkVisible();
		boolean painTextVisible = ratio <= 0.8f || blinkVisible;

		int barWidth = BAR_WIDTH;
		int screenWidth = context.guiWidth();
		int screenHeight = context.guiHeight();
		int[] pos = position(barWidth, screenWidth, screenHeight);
		int x = pos[0];
		int y = pos[1];

		// 背景层
		context.blit(BACKGROUND_BAR_TEXTURE,
			x, y, 0f, 0f, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

		// 覆盖层：按百分比逐段覆盖，最后一段按余量裁剪（不拉伸）
		int fillWidth = Math.round(barWidth * ratio);
		if (fillWidth > 0) {
			context.blit(OVERLAY_BAR_TEXTURE,
				x, y, 0f, 0f, fillWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
		}

		// 休克提示
		Font textRenderer = client.font;
		if (painTextVisible) {
			int percentage = Math.round(painRatio * 100f);
			Component painText = Component.literal("💢 " + percentage + "%");
			int textX = x + barWidth / 2 - textRenderer.width(painText) / 2;
			int textY = Math.max(1, y - 10);
			context.drawString(textRenderer, painText, textX, textY, painTextColor(ratio), true);
		}

		if (pain > max && !client.player.hasEffect(ModStatusEffects.ADRENALINE) && blinkVisible) {
			Component shock = Component.translatable("pain_mechanic.hud.shock")
				.withStyle(style -> style.withBold(true));
			int shockX = x + barWidth / 2 - textRenderer.width(shock) / 2;
			context.drawString(textRenderer, shock, shockX, Math.max(1, y - 20), 0xFFFF5555, true);
		}

		if (PainClientState.shockRemainingTicks > 0 && blinkVisible) {
			int remainingSeconds = Math.max(1, (PainClientState.shockRemainingTicks + 19) / 20);
			Component countdown = Component.literal(Integer.toString(remainingSeconds))
				.withStyle(style -> style.withBold(true));
			int countdownWidth = textRenderer.width(countdown);
			int centerX = x + barWidth / 2;
			int countdownY = Math.max(1, y - 40);
			context.pose().pushPose();
			context.pose().translate(centerX, countdownY, 0.0f);
			context.pose().scale(2f, 2f, 1f);
			context.drawString(textRenderer, countdown, -countdownWidth / 2, 0, 0xFFFF5555, true);
			context.pose().popPose();
		}
	}

	private static boolean isBlinkVisible() {
		long intervalMs = Math.max(50L, Math.round(PainConfig.get().painTextBlinkIntervalSeconds * 1000f));
		return (Util.getMillis() / intervalMs) % 2L == 0L;
	}

	private static int painTextColor(float ratio) {
		ratio = Math.max(0f, Math.min(1f, ratio));
		int red = 255 - Math.round(75f * ratio);
		int green = 255 - Math.round(225f * ratio);
		int blue = green;
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private static int[] position(int barWidth, int screenWidth, int screenHeight) {
		PainConfig.PainConfigData cfg = PainConfig.get();
		int x;
		int y;
		switch (cfg.guiPosition) {
			case "top_left" -> {
				x = 4;
				y = 4;
			}
			case "top_right" -> {
				x = screenWidth - barWidth - 4;
				y = 4;
			}
			case "bottom_left" -> {
				x = 4;
				y = screenHeight - BAR_HEIGHT - 4;
			}
			case "bottom_right" -> {
				x = screenWidth - barWidth - 4;
				y = screenHeight - BAR_HEIGHT - 4;
			}
			default -> {
				// bottom_center：默认在经验条上方居中
				x = (screenWidth - barWidth) / 2;
				y = screenHeight - 58;
			}
		}
		return new int[] { x + cfg.guiXOffset, y + cfg.guiYOffset };
	}
}
