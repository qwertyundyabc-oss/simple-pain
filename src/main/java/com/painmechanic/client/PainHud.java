package com.painmechanic.client;

import com.painmechanic.PainConfig;
import com.painmechanic.PainMechanic;
import com.painmechanic.ModStatusEffects;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.minecraft.util.Util;

/**
 * 原版风格疼痛条：单张贴图（背景帧 + 覆盖帧），按 8x8 段平铺、不拉伸，
 * 覆盖层按疼痛百分比逐段覆盖。
 */
public class PainHud implements HudElement {
	private static final Identifier BACKGROUND_BAR_TEXTURE = PainMechanic.id("textures/hud/pain_bar_background.png");
	private static final Identifier OVERLAY_BAR_TEXTURE = PainMechanic.id("textures/hud/pain_bar_overlay.png");
	private static final Identifier VIGNETTE_TEXTURE = PainMechanic.id("textures/hud/red_vignette.png");
	private static final int BAR_WIDTH = 80;
	private static final int BAR_HEIGHT = 8;
	private static final float GUI_SHAKE_X_FREQUENCY = 82.0f;
	private static final float GUI_SHAKE_Y_FREQUENCY = 101.0f;
	private static DrawContext globalGuiContext;
	private static DrawContext screenGuiContext;

	/** 将当前 GUI 的渲染矩阵平移，抖动不影响鼠标坐标和交互判定。 */
	public static void pushGuiShake(DrawContext context) {
		context.getMatrices().pushMatrix();
		float amplitude = guiShakeAmplitude();
		if (amplitude <= 0f) {
			return;
		}
		float time = (float) (Util.getMeasuringTimeMs() / 1000.0);
		float x = (float) Math.sin(time * GUI_SHAKE_X_FREQUENCY) * amplitude;
		float y = (float) Math.cos(time * GUI_SHAKE_Y_FREQUENCY) * amplitude * 0.75f;
		context.getMatrices().translate(x, y);
	}

	public static void popGuiShake(DrawContext context) {
		context.getMatrices().popMatrix();
	}

	public static void beginGlobalGuiShake(DrawContext context) {
		if (MinecraftClient.getInstance().currentScreen != null) {
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
	public static void beginScreenGuiShake(DrawContext context) {
		if (globalGuiContext == context) {
			return;
		}
		pushGuiShake(context);
		screenGuiContext = context;
	}

	public static void endScreenGuiShake(DrawContext context) {
		if (screenGuiContext == context) {
			popGuiShake(context);
			screenGuiContext = null;
		}
	}

	private static float guiShakeAmplitude() {
		if (MinecraftClient.getInstance().player == null) {
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
	public static void renderVignette(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || !PainConfig.get().redVignetteEnabled) {
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
		int color = ((int) (t * 255.0f) & 0xFF) << 24 | 0x00C01010;
		// 此起彼伏的脉动：1.6 秒一次，改变红晕大小（中央可见圆圈呼吸式放大缩小）
		float time = (float) (Util.getMeasuringTimeMs() / 1000.0);
		float k = 0.5f + 0.5f * (0.5f + 0.5f * (float) Math.sin(time * 3.926990817f));
		int sw = context.getScaledWindowWidth();
		int sh = context.getScaledWindowHeight();
		int halfRegion = (int) (k * 128.0f);
		int region = (int) (k * 256.0f);
		boolean restoreGuiShake = globalGuiContext == context;
		if (restoreGuiShake) {
			context.getMatrices().popMatrix();
		}
		context.drawTexture(RenderPipelines.GUI_TEXTURED, VIGNETTE_TEXTURE,
			0, 0, 128 - halfRegion, 128 - halfRegion, sw, sh, region, region, 256, 256, color);
		if (restoreGuiShake) {
			context.getMatrices().pushMatrix();
		}
	}

	public static void renderDyingOverlay(DrawContext context) {
		float alpha = PainClientTicker.dyingOverlayAlpha();
		if (alpha <= 0f) {
			return;
		}
		int color = ((int) (alpha * 255f) & 0xFF) << 24;
		context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color);
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.currentScreen != null) {
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
		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		int[] pos = position(barWidth, screenWidth, screenHeight);
		int x = pos[0];
		int y = pos[1];

		// 背景层
		context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND_BAR_TEXTURE,
			x, y, 0f, 0f, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

		// 覆盖层：按百分比逐段覆盖，最后一段按余量裁剪（不拉伸）
		int fillWidth = Math.round(barWidth * ratio);
		if (fillWidth > 0) {
			context.drawTexture(RenderPipelines.GUI_TEXTURED, OVERLAY_BAR_TEXTURE,
				x, y, 0f, 0f, fillWidth, BAR_HEIGHT, fillWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
		}

		// 休克提示
		TextRenderer textRenderer = client.textRenderer;
		if (painTextVisible) {
			int percentage = Math.round(painRatio * 100f);
			Text painText = Text.literal("💢 " + percentage + "%");
			int textX = x + barWidth / 2 - textRenderer.getWidth(painText) / 2;
			int textY = Math.max(1, y - 10);
			context.drawText(textRenderer, painText, textX, textY, painTextColor(ratio), true);
		}

		if (pain > max && !client.player.hasStatusEffect(ModStatusEffects.ADRENALINE) && blinkVisible) {
			Text shock = Text.translatable("pain_mechanic.hud.shock")
				.styled(style -> style.withBold(true));
			int shockX = x + barWidth / 2 - textRenderer.getWidth(shock) / 2;
			context.drawText(textRenderer, shock, shockX, Math.max(1, y - 20), 0xFFFF5555, true);
		}

		if (PainClientState.shockRemainingTicks > 0 && blinkVisible) {
			int remainingSeconds = Math.max(1, (PainClientState.shockRemainingTicks + 19) / 20);
			Text countdown = Text.literal(Integer.toString(remainingSeconds))
				.styled(style -> style.withBold(true));
			int countdownWidth = textRenderer.getWidth(countdown);
			int centerX = x + barWidth / 2;
			int countdownY = Math.max(1, y - 40);
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(centerX, countdownY);
			context.getMatrices().scale(2f, 2f);
			context.drawText(textRenderer, countdown, -countdownWidth / 2, 0, 0xFFFF5555, true);
			context.getMatrices().popMatrix();
		}
	}

	private static boolean isBlinkVisible() {
		long intervalMs = Math.max(50L, Math.round(PainConfig.get().painTextBlinkIntervalSeconds * 1000f));
		return (Util.getMeasuringTimeMs() / intervalMs) % 2L == 0L;
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
