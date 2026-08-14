package com.painmechanic.client;

import com.painmechanic.PainConfig;
import com.painmechanic.PainMechanic;
import com.painmechanic.ModStatusEffects;

import java.util.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.minecraft.util.Util;

/**
 * 原版风格疼痛条：单张贴图（背景帧 + 覆盖帧），按 8x8 段平铺、不拉伸，
 * 覆盖层按疼痛百分比逐段覆盖。
 */
public class PainHud {
	private static final Identifier BACKGROUND_BAR_TEXTURE = PainMechanic.id("textures/hud/pain_bar_background.png");
	private static final Identifier OVERLAY_BAR_TEXTURE = PainMechanic.id("textures/hud/pain_bar_overlay.png");
	private static final int BAR_WIDTH = 80;
	private static final int BAR_HEIGHT = 8;
	private static final float GUI_SHAKE_X_FREQUENCY = 82.0f;
	private static final float GUI_SHAKE_Y_FREQUENCY = 101.0f;
	private static final int VIGNETTE_RGB = 0xE03438;
	/** 旧版 NativeImage.setColor 使用 ABGR 像素格式（新版为 ARGB），这里把颜色分量交换好，避免红蓝互换。 */
	private static final int VIGNETTE_ABGR = (VIGNETTE_RGB & 0xFF) << 16 | (VIGNETTE_RGB & 0xFF00) | (VIGNETTE_RGB >> 16) & 0xFF;
	private static final int VIGNETTE_TEXTURE_WIDTH = 256;
	private static final float VIGNETTE_START_RATIO = 0.20f;
	private static final float VIGNETTE_PULSE_SECONDS = 1.28f;
	private static final int SPLATTER_RAY_COUNT = 18;
	private static final int ANIMATED_GRAIN_FRAME_COUNT = 24;
	private static final float GRAIN_FRAMES_PER_SECOND = 30.0f;
	private static DrawContext globalGuiContext;
	private static DrawContext screenGuiContext;
	private static NativeImageBackedTexture vignetteDynamicTexture;
	private static NativeImage vignetteImage;
	private static Identifier vignetteDynamicId;
	private static int vignetteTexW;
	private static int vignetteTexH;
	private static float[] distCache;
	private static float[] splatterProfileCache;
	private static float[] staticGrainCache;
	private static float[] contourWaveSinCache;
	private static float[][] animatedGrainFrames;
	private static int distCacheW;
	private static int distCacheH;
	private static long lastImpactMs = -1L;
	private static float impactStrength;
	private static long lastVignetteFrameMs = -1L;
	private static float smoothedPainRatio;
	private static long lastVisibilityFrameMs = -1L;
	private static float vignetteVisibility;

	/** 将当前 GUI 的渲染矩阵平移，抖动不影响鼠标坐标和交互判定。 */
	public static void pushGuiShake(DrawContext context) {
		context.getMatrices().push();
		float amplitude = guiShakeAmplitude();
		if (amplitude <= 0f) {
			return;
		}
		float time = (float) (Util.getMeasuringTimeMs() / 1000.0);
		float x = (float) Math.sin(time * GUI_SHAKE_X_FREQUENCY) * amplitude;
		float y = (float) Math.cos(time * GUI_SHAKE_Y_FREQUENCY) * amplitude * 0.75f;
		context.getMatrices().translate(x, y, 0.0f);
	}

	public static void popGuiShake(DrawContext context) {
		context.getMatrices().pop();
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
	 * 参考 Casualties Unknown 的像素伤痛遮罩：整屏红雾包围中央放射状裂口，
	 * 裂口快速开合并带有颗粒化边缘；真实受伤会重置相位并触发一次红闪。
	 */
	public static void renderVignette(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || !PainConfig.get().redVignetteEnabled) {
			return;
		}
		long now = Util.getMeasuringTimeMs();
		float max = Math.max(1f, PainClientState.maxHealth);
		float targetRatio = clamp01(PainClientState.pain / max);
		float ratio = smoothPainRatio(targetRatio, now);
		float elapsedSinceImpact = lastImpactMs >= 0L ? (now - lastImpactMs) / 1000.0f : Float.POSITIVE_INFINITY;
		float impact = lastImpactMs >= 0L
			? impactStrength * (float) Math.exp(-elapsedSinceImpact * 1.8f)
			: 0f;
		float visibility = smoothVignetteVisibility(
			targetRatio >= VIGNETTE_START_RATIO || impact >= 0.01f, now);
		float persistent = ratio >= VIGNETTE_START_RATIO
			? smootherstep(VIGNETTE_START_RATIO, 1.04f, ratio)
			: 0f;
		if (visibility < 0.002f && persistent <= 0f && impact < 0.01f) {
			return;
		}

		float phaseSeconds = now / 1000.0f;
		float openPulse = 0.5f + 0.5f * (float) Math.cos(
			phaseSeconds * (float) (Math.PI * 2.0) / VIGNETTE_PULSE_SECONDS);

		boolean restoreGuiShake = globalGuiContext == context;
		if (restoreGuiShake) {
			context.getMatrices().pop();
		}
		renderVignettePixels(context, ratio, persistent, impact, visibility, openPulse, phaseSeconds);
		if (restoreGuiShake) {
			context.getMatrices().push();
		}
	}

	/** 逐像素合成红雾与放射状颗粒裂口，最终只上传一张动态纹理。 */
	private static void renderVignettePixels(DrawContext context, float ratio, float persistent,
			float impact, float visibility, float openPulse, float phaseSeconds) {
		int sw = context.getScaledWindowWidth();
		int sh = context.getScaledWindowHeight();
		int texW = VIGNETTE_TEXTURE_WIDTH;
		int texH = Math.max(1, Math.round(texW * sh / (float) sw));
		ensureVignetteTexture(texW, texH);
		float cx = texW * 0.5f;
		float cy = texH * 0.5f;
		float farRadius = (float) Math.sqrt(cx * cx + cy * cy);
		ensureSplatterCache(texW, texH, cx, cy);

		float pulseStrength = smootherstep(0.32f, 0.54f, ratio);
		float washStrength = smootherstep(0.44f, 1.0f, ratio);
		float globalPulseStrength = smootherstep(0.46f, 0.78f, ratio);
		float snowStrength = smootherstep(0.38f, 0.80f, ratio);
		float centerCoverageStrength = smootherstep(0.56f, 0.98f, ratio);
		float geometryRatio = Math.min(ratio, 0.50f);
		float geometryStrength = smootherstep(VIGNETTE_START_RATIO, 1.04f, geometryRatio);
		float vesselStrength = 0.20f + 0.80f * smootherstep(0.48f, 0.68f, geometryRatio);

		// 参考画面主要通过浓度呼吸；裂口轮廓只轻微起伏，不再整圈骤然收缩。
		float lowPainSpread = smootherstep(0.20f, 0.50f, ratio);
		float lowPainWindowRadius = farRadius * (2.00f - 1.04f * lowPainSpread);
		float highPainWindowRadius = farRadius * (1.03f - 0.28f * persistent);
		float geometryBlend = smootherstep(0.42f, 0.58f, ratio);
		float windowRadius = lowPainWindowRadius * (1.0f - geometryBlend)
			+ highPainWindowRadius * geometryBlend;
		float shapeBreath = (openPulse - 0.5f) * (0.035f + 0.045f * persistent);
		windowRadius *= 1.0f + shapeBreath;
		float centerCompression = 0.28f * centerCoverageStrength * (1.0f - openPulse);
		windowRadius *= 1.0f - centerCompression;
		windowRadius *= 1.0f + 0.38f * (1.0f - visibility);

		float pulseLevel = 0.25f + 0.75f * (1.0f - openPulse);
		float pulseAlpha = (0.012f + 0.55f * pulseStrength) * pulseLevel;
		float edgeAlpha = 0.04f + 0.22f * persistent + pulseAlpha;
		float globalAlpha = 0.22f * washStrength
			+ 0.32f * globalPulseStrength * (1.0f - openPulse);
		edgeAlpha += (0.78f - edgeAlpha) * impact;
		globalAlpha += (0.55f - globalAlpha) * impact;
		float lowPainOpacityScale = 0.72f + 0.28f * smootherstep(0.20f, 0.40f, ratio);
		edgeAlpha *= lowPainOpacityScale;
		globalAlpha *= lowPainOpacityScale;
		edgeAlpha = Math.min(0.92f, edgeAlpha);
		globalAlpha = Math.min(0.76f, globalAlpha);
		float clearStrength = 0.94f - 0.10f * persistent;
		float edgeWidth = 6.0f + 18.0f * geometryStrength;
		float contourWaveOscillation = 1.0f - 2.0f * openPulse;
		float contourWaveAmount = 0.02f
			+ 0.08f * smootherstep(0.35f, 0.50f, ratio)
			+ 0.20f * smootherstep(0.50f, 1.0f, ratio);

		float grainT = phaseSeconds * GRAIN_FRAMES_PER_SECOND;
		int grainFrame = (int) Math.floor(grainT);
		int frameA = Math.floorMod(grainFrame, ANIMATED_GRAIN_FRAME_COUNT);
		int idx = 0;
		for (int py = 0; py < texH; py++) {
			for (int px = 0; px < texW; px++) {
				float d = distCache[idx++];
				float profile = 0.40f + (splatterProfileCache[idx - 1] - 0.40f) * vesselStrength;
				float staticGrain = staticGrainCache[idx - 1];
				float contourWave = contourWaveSinCache[idx - 1] * contourWaveOscillation;
				float roughBoundary = windowRadius * profile * (0.96f + 0.08f * staticGrain);
				roughBoundary *= 1.0f + contourWaveAmount * contourWave;
				float hole = windowRadius > 0f
					? 1.0f - smootherstep(roughBoundary - edgeWidth, roughBoundary + edgeWidth, d)
					: 0f;

				// 裂口内部保留密集红色颗粒，形成视频中的血块/噪点质感。
				float animatedGrain = animatedGrainFrames[frameA][idx - 1];
				float grainMix = staticGrain;
				float vignetteAlpha = edgeAlpha * (1.0f - hole * clearStrength);
				float alpha = 1.0f - (1.0f - globalAlpha) * (1.0f - vignetteAlpha);
				alpha *= 0.94f + 0.06f * grainMix;
				float breakupBand = 1.0f - smootherstep(edgeWidth * 0.5f,
					edgeWidth * 3.5f, Math.abs(d - roughBoundary));
				float breakup = smootherstep(0.25f, 0.85f, grainMix);
				alpha *= 1.0f - breakupBand * (1.0f - breakup) * 0.52f;
				float snowPulse = 0.35f + 0.65f * openPulse;
				float snowDensity = 0.60f * snowStrength * snowPulse;
				if (animatedGrain > 1.0f - snowDensity) {
					float snowAlpha = 0.14f + 0.12f * snowStrength;
					alpha = 1.0f - (1.0f - alpha) * (1.0f - snowAlpha);
				}
				alpha *= visibility;

				// 裂口边缘外侧的少量脱落像素，让轮廓不呈现干净的几何线。
				if (d > roughBoundary && d < roughBoundary + edgeWidth * 3.0f && staticGrain > 0.93f) {
					float fragment = (staticGrain - 0.93f) / 0.07f;
					alpha *= 1.0f - fragment * 0.45f * openPulse;
				}
				int alphaByte = Math.min(255, Math.round(alpha * 255f));
				vignetteImage.setColor(px, py, alphaByte << 24 | VIGNETTE_ABGR);
			}
		}

		vignetteDynamicTexture.upload();
		RenderSystem.enableBlend();
		context.drawTexture(vignetteDynamicId,
			0, 0, sw, sh, 0f, 0f, texW, texH, texW, texH);
	}

	/** 按屏幕比例创建/重建红晕动态纹理。 */
	private static void ensureVignetteTexture(int texW, int texH) {
		if (vignetteDynamicTexture != null && vignetteTexW == texW && vignetteTexH == texH) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (vignetteDynamicTexture != null) {
			client.getTextureManager().destroyTexture(vignetteDynamicId);
		}
		vignetteImage = new NativeImage(texW, texH, false);
		vignetteDynamicTexture = new NativeImageBackedTexture(vignetteImage);
		vignetteDynamicId = PainMechanic.id("vignette");
		client.getTextureManager().registerTexture(vignetteDynamicId, vignetteDynamicTexture);
		vignetteTexW = texW;
		vignetteTexH = texH;
	}

	/** 尺寸变化时重建放射裂口形状、距离与静态颗粒缓存。 */
	private static void ensureSplatterCache(int texW, int texH, float cx, float cy) {
		if (distCache != null && distCacheW == texW && distCacheH == texH) {
			return;
		}
		distCache = new float[texW * texH];
		splatterProfileCache = new float[texW * texH];
		staticGrainCache = new float[texW * texH];
		contourWaveSinCache = new float[texW * texH];
		animatedGrainFrames = new float[ANIMATED_GRAIN_FRAME_COUNT][texW * texH];
		float[] rayAngles = new float[SPLATTER_RAY_COUNT];
		float[] rayWidths = new float[SPLATTER_RAY_COUNT];
		float[] rayLengths = new float[SPLATTER_RAY_COUNT];
		Random random = new Random(20260814L);
		float rayStep = (float) (Math.PI * 2.0 / SPLATTER_RAY_COUNT);
		for (int i = 0; i < SPLATTER_RAY_COUNT; i++) {
			rayAngles[i] = i * rayStep + (random.nextFloat() - 0.5f) * rayStep * 0.48f;
			rayWidths[i] = 0.10f + random.nextFloat() * 0.10f;
			rayLengths[i] = 0.72f + random.nextFloat() * 0.30f;
		}

		for (int py = 0; py < texH; py++) {
			for (int px = 0; px < texW; px++) {
				float dx = px + 0.5f - cx;
				float dy = py + 0.5f - cy;
				int index = py * texW + px;
				distCache[index] = (float) Math.sqrt(dx * dx + dy * dy);
				float angle = (float) Math.atan2(dy, dx);
				float warpedAngle = angle + 0.025f * (float) Math.sin(
					distCache[index] * 0.065f + angle * 3.0f);
				float profile = 0.40f + 0.006f * (float) Math.sin(angle * 11.0f + 1.1f);
				for (int ray = 0; ray < SPLATTER_RAY_COUNT; ray++) {
					float delta = angleDistance(warpedAngle, rayAngles[ray]);
					float normalized = delta / rayWidths[ray];
					if (normalized < 1.8f) {
						float petal = (float) Math.exp(-0.5f * normalized * normalized
							* normalized * normalized);
						profile = Math.max(profile, 0.40f + 0.62f * petal * rayLengths[ray]);
					}
				}
				splatterProfileCache[index] = Math.max(0.38f, Math.min(1.10f, profile));
				float fineGrain = hashPx(px, py, 20260814);
				staticGrainCache[index] = fineGrain;
				float wavePhase = angle * 8.0f
					+ 0.55f * (float) Math.sin(angle * 14.0f + 1.1f);
				contourWaveSinCache[index] = (float) Math.sin(wavePhase);
				for (int frame = 0; frame < ANIMATED_GRAIN_FRAME_COUNT; frame++) {
					animatedGrainFrames[frame][index] = hashPx(px, py, 9109 + frame * 1297);
				}
			}
		}
		distCacheW = texW;
		distCacheH = texH;
	}

	private static float angleDistance(float a, float b) {
		float delta = Math.abs(a - b) % (float) (Math.PI * 2.0);
		return delta > Math.PI ? (float) (Math.PI * 2.0) - delta : delta;
	}

	/** 伪随机哈希：0~1，整数 xorshift，避免逐像素 Math.sin 的开销。 */
	private static float hashPx(int x, int y, int seed) {
		int h = x * 374761393 + y * 668265263 + seed * 1440657193;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFF) * (1.0f / 65535.0f);
	}

	public static void onPainImpact(float painIncrease, float maxHealth) {
		if (painIncrease <= 0f) {
			return;
		}
		float relativeImpact = painIncrease / Math.max(1f, maxHealth);
		impactStrength = clamp01(0.06f + relativeImpact / 0.50f * 0.54f);
		lastImpactMs = Util.getMeasuringTimeMs();
	}

	public static void resetVignetteAnimation() {
		lastImpactMs = -1L;
		impactStrength = 0f;
		lastVignetteFrameMs = -1L;
		smoothedPainRatio = 0f;
		lastVisibilityFrameMs = -1L;
		vignetteVisibility = 0f;
	}

	private static float smoothPainRatio(float target, long now) {
		if (lastVignetteFrameMs < 0L) {
			lastVignetteFrameMs = now;
			return smoothedPainRatio;
		}
		float deltaSeconds = Math.min(0.10f, Math.max(0f, (now - lastVignetteFrameMs) / 1000.0f));
		lastVignetteFrameMs = now;
		float followSpeed = target > smoothedPainRatio ? 2.4f : 1.8f;
		float blend = 1.0f - (float) Math.exp(-followSpeed * deltaSeconds);
		smoothedPainRatio += (target - smoothedPainRatio) * blend;
		if (Math.abs(target - smoothedPainRatio) < 0.0005f) {
			smoothedPainRatio = target;
		}
		return smoothedPainRatio;
	}

	private static float smoothVignetteVisibility(boolean visible, long now) {
		if (lastVisibilityFrameMs < 0L) {
			lastVisibilityFrameMs = now;
		}
		float deltaSeconds = Math.min(0.10f,
			Math.max(0f, (now - lastVisibilityFrameMs) / 1000.0f));
		lastVisibilityFrameMs = now;
		float target = visible ? 1.0f : 0f;
		float followSpeed = visible ? 5.0f : 1.25f;
		float blend = 1.0f - (float) Math.exp(-followSpeed * deltaSeconds);
		vignetteVisibility += (target - vignetteVisibility) * blend;
		if (Math.abs(target - vignetteVisibility) < 0.001f) {
			vignetteVisibility = target;
		}
		return vignetteVisibility;
	}

	private static float smoothstep(float edge0, float edge1, float value) {
		if (edge1 <= edge0) {
			return value >= edge1 ? 1f : 0f;
		}
		float t = clamp01((value - edge0) / (edge1 - edge0));
		return t * t * (3.0f - 2.0f * t);
	}

	private static float smootherstep(float edge0, float edge1, float value) {
		if (edge1 <= edge0) {
			return value >= edge1 ? 1f : 0f;
		}
		float t = clamp01((value - edge0) / (edge1 - edge0));
		return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
	}

	private static float clamp01(float value) {
		return Math.max(0f, Math.min(1f, value));
	}

	public static void renderDyingOverlay(DrawContext context) {
		float alpha = PainClientTicker.dyingOverlayAlpha();
		if (alpha <= 0f) {
			return;
		}
		int color = ((int) (alpha * 255f) & 0xFF) << 24;
		context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color);
	}

	public static void render(DrawContext context) {
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
		context.drawTexture(BACKGROUND_BAR_TEXTURE,
			x, y, 0f, 0f, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

		// 覆盖层：按百分比逐段覆盖，最后一段按余量裁剪（不拉伸）
		int fillWidth = Math.round(barWidth * ratio);
		if (fillWidth > 0) {
			context.drawTexture(OVERLAY_BAR_TEXTURE,
				x, y, 0f, 0f, fillWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
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
			context.getMatrices().push();
			context.getMatrices().translate(centerX, countdownY, 0.0f);
			context.getMatrices().scale(2f, 2f, 1f);
			context.drawText(textRenderer, countdown, -countdownWidth / 2, 0, 0xFFFF5555, true);
			context.getMatrices().pop();
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
