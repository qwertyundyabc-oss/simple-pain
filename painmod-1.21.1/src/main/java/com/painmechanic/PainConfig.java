package com.painmechanic;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

/**
 * 模组配置：读取/写入 config/pain_mechanic.json。
 */
public final class PainConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static PainConfigData data = new PainConfigData();

	private PainConfig() {
	}

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("pain_mechanic.json");
		try {
			Path dir = FabricLoader.getInstance().getConfigDir();
			Files.createDirectories(dir);
			PainConfigData defaults = new PainConfigData();
			if (Files.exists(file)) {
				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					JsonElement root = JsonParser.parseReader(reader);
					if (!root.isJsonObject()) {
						throw new JsonParseException("配置根节点必须是对象");
					}
					data = readConfig(root.getAsJsonObject(), defaults);
				}
			} else {
				data = defaults;
			}
			normalize(data);
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
			PainMechanic.LOGGER.info("[Simple Pain] 配置文件: {}", file);
		} catch (IOException | RuntimeException e) {
			PainMechanic.LOGGER.warn("[Simple Pain] 无法读取配置文件，使用默认值", e);
			data = new PainConfigData();
			normalize(data);
		}
	}

	private static PainConfigData readConfig(JsonObject json, PainConfigData defaults) {
		PainConfigData value = new PainConfigData();
		value.damageToPainMultiplier = floatValue(json, "damageToPainMultiplier", defaults.damageToPainMultiplier);
		value.lethalDamagePainMultiplier = floatValue(json, "lethalDamagePainMultiplier", defaults.lethalDamagePainMultiplier);
		value.maxPainMultiplier = floatValue(json, "maxPainMultiplier", defaults.maxPainMultiplier);
		value.painDecayPerSecond = floatValue(json, "painDecayPerSecond", defaults.painDecayPerSecond);
		value.fullHealthPainDecayMultiplier = floatValue(json, "fullHealthPainDecayMultiplier", defaults.fullHealthPainDecayMultiplier);
		value.painReliefDecayPerSecond = floatValue(json, "painReliefDecayPerSecond", defaults.painReliefDecayPerSecond);
		value.lowHealthPainPerTick = floatValue(json, "lowHealthPainPerTick", defaults.lowHealthPainPerTick);
		value.painReliefDurationSeconds = intValue(json, "painReliefDurationSeconds", defaults.painReliefDurationSeconds);
		value.adrenalineThresholdPercent = floatValue(json, "adrenalineThresholdPercent", defaults.adrenalineThresholdPercent);
		value.adrenalineDurationSeconds = intValue(json, "adrenalineDurationSeconds", defaults.adrenalineDurationSeconds);
		value.adrenalineCooldownSeconds = intValue(json, "adrenalineCooldownSeconds", defaults.adrenalineCooldownSeconds);
		value.debuffThreshold = floatValue(json, "debuffThreshold", defaults.debuffThreshold);
		value.debuffMaxPercent = floatValue(json, "debuffMaxPercent", defaults.debuffMaxPercent);
		value.protectionEnabled = booleanValue(json, "protectionEnabled", defaults.protectionEnabled);
		value.protectionBypassUnavoidable = booleanValue(json, "protectionBypassUnavoidable", defaults.protectionBypassUnavoidable);
		value.shockEnabled = booleanValue(json, "shockEnabled", defaults.shockEnabled);
		value.shockDurationSeconds = intValue(json, "shockDurationSeconds", defaults.shockDurationSeconds);
		value.shockDarkness = booleanValue(json, "shockDarkness", defaults.shockDarkness);
		value.redVignetteEnabled = booleanValue(json, "redVignetteEnabled", defaults.redVignetteEnabled);
		value.redVignetteThreshold = floatValue(json, "redVignetteThreshold", defaults.redVignetteThreshold);
		value.painDroneEnabled = booleanValue(json, "painDroneEnabled", defaults.painDroneEnabled);
		value.breathingEnabled = booleanValue(json, "breathingEnabled", defaults.breathingEnabled);
		value.painDroneThreshold = floatValue(json, "painDroneThreshold", defaults.painDroneThreshold);
		value.painDroneFadeSeconds = floatValue(json, "painDroneFadeSeconds", defaults.painDroneFadeSeconds);
		value.guiShakeThreshold = floatValue(json, "guiShakeThreshold", defaults.guiShakeThreshold);
		value.guiShakeMaxAmplitude = floatValue(json, "guiShakeMaxAmplitude", defaults.guiShakeMaxAmplitude);
		value.painTextBlinkIntervalSeconds = floatValue(json, "painTextBlinkIntervalSeconds", defaults.painTextBlinkIntervalSeconds);
		value.guiPosition = stringValue(json, "guiPosition", defaults.guiPosition);
		value.guiXOffset = intValue(json, "guiXOffset", defaults.guiXOffset);
		value.guiYOffset = intValue(json, "guiYOffset", defaults.guiYOffset);
		value.rescueEnabled = booleanValue(json, "rescueEnabled", defaults.rescueEnabled);
		value.rescueDurationSeconds = intValue(json, "rescueDurationSeconds", defaults.rescueDurationSeconds);
		value.rescuePainRatio = floatValue(json, "rescuePainRatio", defaults.rescuePainRatio);
		value.rescueHealthRatio = floatValue(json, "rescueHealthRatio", defaults.rescueHealthRatio);
		value.rescueMaxDistance = floatValue(json, "rescueMaxDistance", defaults.rescueMaxDistance);
		value.debugLogging = booleanValue(json, "debugLogging", defaults.debugLogging);
		return value;
	}

	private static float floatValue(JsonObject json, String key, float fallback) {
		JsonElement element = json.get(key);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
			? element.getAsFloat() : fallback;
	}

	private static int intValue(JsonObject json, String key, int fallback) {
		JsonElement element = json.get(key);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
			? element.getAsInt() : fallback;
	}

	private static boolean booleanValue(JsonObject json, String key, boolean fallback) {
		JsonElement element = json.get(key);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
			? element.getAsBoolean() : fallback;
	}

	private static String stringValue(JsonObject json, String key, String fallback) {
		JsonElement element = json.get(key);
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
			? element.getAsString() : fallback;
	}

	private static void normalize(PainConfigData value) {
		value.damageToPainMultiplier = nonNegative(value.damageToPainMultiplier, 1f);
		value.lethalDamagePainMultiplier = nonNegative(value.lethalDamagePainMultiplier, 2f);
		value.maxPainMultiplier = finite(value.maxPainMultiplier) ? value.maxPainMultiplier : 2f;
		value.painDecayPerSecond = nonNegative(value.painDecayPerSecond, 0.5f);
		value.fullHealthPainDecayMultiplier = nonNegative(value.fullHealthPainDecayMultiplier, 1.5f);
		value.painReliefDecayPerSecond = nonNegative(value.painReliefDecayPerSecond, 2f);
		value.lowHealthPainPerTick = nonNegative(value.lowHealthPainPerTick, 0.01f);
		value.painReliefDurationSeconds = Math.max(1, value.painReliefDurationSeconds);
		value.adrenalineThresholdPercent = clamp(value.adrenalineThresholdPercent, 0f, 100f, 50f);
		value.adrenalineDurationSeconds = Math.max(1, value.adrenalineDurationSeconds);
		value.adrenalineCooldownSeconds = Math.max(1, value.adrenalineCooldownSeconds);
		value.debuffThreshold = nonNegative(value.debuffThreshold, 4f);
		value.debuffMaxPercent = clamp(value.debuffMaxPercent, 0f, 100f, 50f);
		value.shockDurationSeconds = Math.max(1, value.shockDurationSeconds);
		value.redVignetteThreshold = clamp(value.redVignetteThreshold, 0f, 0.99f, 0.3f);
		value.painDroneThreshold = clamp(value.painDroneThreshold, 0f, 0.99f, 0.6f);
		value.painDroneFadeSeconds = Math.max(0.05f, finite(value.painDroneFadeSeconds) ? value.painDroneFadeSeconds : 0.3f);
		value.guiShakeThreshold = clamp(value.guiShakeThreshold, 0f, 0.99f, 0.65f);
		value.guiShakeMaxAmplitude = Math.max(0f, finite(value.guiShakeMaxAmplitude) ? value.guiShakeMaxAmplitude : 4f);
		value.painTextBlinkIntervalSeconds = Math.max(0.05f,
			finite(value.painTextBlinkIntervalSeconds) ? value.painTextBlinkIntervalSeconds : 0.272f);
		if (!isGuiPosition(value.guiPosition)) {
			value.guiPosition = "bottom_center";
		}
		value.rescueDurationSeconds = Math.max(1, value.rescueDurationSeconds);
		value.rescuePainRatio = clamp(value.rescuePainRatio, 0f, 1f, 0.75f);
		value.rescueHealthRatio = clamp(value.rescueHealthRatio, 0.05f, 1f, 0.25f);
		value.rescueMaxDistance = Math.max(1f, finite(value.rescueMaxDistance) ? value.rescueMaxDistance : 3f);
	}

	private static boolean finite(float value) {
		return Float.isFinite(value);
	}

	private static float nonNegative(float value, float fallback) {
		return finite(value) ? Math.max(0f, value) : fallback;
	}

	private static float clamp(float value, float min, float max, float fallback) {
		return finite(value) ? Math.max(min, Math.min(max, value)) : fallback;
	}

	private static boolean isGuiPosition(String value) {
		return "bottom_center".equals(value) || "top_left".equals(value) || "top_right".equals(value)
			|| "bottom_left".equals(value) || "bottom_right".equals(value);
	}

	public static PainConfigData get() {
		return data;
	}

	/** Returns an editable snapshot; changes to it do not affect the running game. */
	public static PainConfigData copy() {
		return copyOf(data);
	}

	/** Saves pending values and applies client visual settings immediately. */
	public static void savePending(PainConfigData pending) {
		PainConfigData normalized = copyOf(pending);
		normalize(normalized);
		applyClientVisuals(normalized);
		Path file = FabricLoader.getInstance().getConfigDir().resolve("pain_mechanic.json");
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(normalized, writer);
			}
			PainMechanic.LOGGER.info("[Simple Pain] Configuration saved; client visual settings applied immediately: {}", file);
		} catch (IOException e) {
			PainMechanic.LOGGER.warn("[Simple Pain] 无法保存配置", e);
		}
	}

	private static void applyClientVisuals(PainConfigData source) {
		data.redVignetteEnabled = source.redVignetteEnabled;
		data.redVignetteThreshold = source.redVignetteThreshold;
		data.guiShakeThreshold = source.guiShakeThreshold;
		data.guiShakeMaxAmplitude = source.guiShakeMaxAmplitude;
		data.painTextBlinkIntervalSeconds = source.painTextBlinkIntervalSeconds;
		data.guiPosition = source.guiPosition;
		data.guiXOffset = source.guiXOffset;
		data.guiYOffset = source.guiYOffset;
	}

	private static PainConfigData copyOf(PainConfigData source) {
		PainConfigData value = new PainConfigData();
		value.damageToPainMultiplier = source.damageToPainMultiplier;
		value.lethalDamagePainMultiplier = source.lethalDamagePainMultiplier;
		value.maxPainMultiplier = source.maxPainMultiplier;
		value.painDecayPerSecond = source.painDecayPerSecond;
		value.fullHealthPainDecayMultiplier = source.fullHealthPainDecayMultiplier;
		value.painReliefDecayPerSecond = source.painReliefDecayPerSecond;
		value.lowHealthPainPerTick = source.lowHealthPainPerTick;
		value.painReliefDurationSeconds = source.painReliefDurationSeconds;
		value.adrenalineThresholdPercent = source.adrenalineThresholdPercent;
		value.adrenalineDurationSeconds = source.adrenalineDurationSeconds;
		value.adrenalineCooldownSeconds = source.adrenalineCooldownSeconds;
		value.debuffThreshold = source.debuffThreshold;
		value.debuffMaxPercent = source.debuffMaxPercent;
		value.protectionEnabled = source.protectionEnabled;
		value.protectionBypassUnavoidable = source.protectionBypassUnavoidable;
		value.shockEnabled = source.shockEnabled;
		value.shockDurationSeconds = source.shockDurationSeconds;
		value.shockDarkness = source.shockDarkness;
		value.redVignetteEnabled = source.redVignetteEnabled;
		value.redVignetteThreshold = source.redVignetteThreshold;
		value.painDroneEnabled = source.painDroneEnabled;
		value.breathingEnabled = source.breathingEnabled;
		value.painDroneThreshold = source.painDroneThreshold;
		value.painDroneFadeSeconds = source.painDroneFadeSeconds;
		value.guiShakeThreshold = source.guiShakeThreshold;
		value.guiShakeMaxAmplitude = source.guiShakeMaxAmplitude;
		value.painTextBlinkIntervalSeconds = source.painTextBlinkIntervalSeconds;
		value.guiPosition = source.guiPosition;
		value.guiXOffset = source.guiXOffset;
		value.guiYOffset = source.guiYOffset;
		value.rescueEnabled = source.rescueEnabled;
		value.rescueDurationSeconds = source.rescueDurationSeconds;
		value.rescuePainRatio = source.rescuePainRatio;
		value.rescueHealthRatio = source.rescueHealthRatio;
		value.rescueMaxDistance = source.rescueMaxDistance;
		value.debugLogging = source.debugLogging;
		return value;
	}

	public static class PainConfigData {
		/** 每点伤害转化的疼痛值倍数 */
		public float damageToPainMultiplier = 1.0f;
		/** 致命伤害部分的疼痛倍数（默认 2 = 翻倍） */
		public float lethalDamagePainMultiplier = 2.0f;
		/** 疼痛值上限 = 最大生命值 × 此倍数（默认 2 倍），<=0 表示不限制 */
		public float maxPainMultiplier = 2.0f;
		/** 每秒自然减少的疼痛百分比（相对于最大生命值） */
		public float painDecayPerSecond = 0.5f;
		public float fullHealthPainDecayMultiplier = 1.5f;
		/** 止痛效果期间每秒减少的疼痛百分比（相对于最大生命值） */
		public float painReliefDecayPerSecond = 2.0f;
		/** Pain added every tick while health is at or below 1 without pain relief or adrenaline. */
		public float lowHealthPainPerTick = 0.01f;
		/** 止痛效果持续时间（秒） */
		public int painReliefDurationSeconds = 180;
		public float adrenalineThresholdPercent = 50.0f;
		public int adrenalineDurationSeconds = 20;
		public int adrenalineCooldownSeconds = 300;
		/** 减益起始阈值（疼痛 >= 此值开始扣除） */
		public float debuffThreshold = 4.0f;
		/** 减益最多扣除的百分比 */
		public float debuffMaxPercent = 50.0f;
		/** 是否启用濒死保护机制 */
		public boolean protectionEnabled = true;
		/** 是否让虚空和 /kill 等不可避免伤害绕过濒死保护 */
		public boolean protectionBypassUnavoidable = true;
		/** 是否启用休克、休克锁定和休克后的死亡流程 */
		public boolean shockEnabled = true;
		/** 进入死亡音效阶段前的休克持续时间（秒） */
		public int shockDurationSeconds = 20;
		/** 休克时是否持续获得黑暗效果 */
		public boolean shockDarkness = true;
		/** 是否启用屏幕红晕效果 */
		public boolean redVignetteEnabled = true;
		/** 红晕出现的疼痛比例阈值（疼痛/最大生命值） */
		public float redVignetteThreshold = 0.3f;
		/** 是否启用疼痛无人机音效 */
		public boolean painDroneEnabled = true;
		public boolean breathingEnabled = false;
		/** 疼痛无人机音效触发阈值（疼痛/最大生命值） */
		public float painDroneThreshold = 0.6f;
		/** 疼痛无人机音量变化时的淡入/淡出时长（秒） */
		public float painDroneFadeSeconds = 0.3f;
		/** GUI 开始颤抖的疼痛比例 */
		public float guiShakeThreshold = 0.65f;
		/** GUI 颤抖在满疼痛时的最大像素幅度 */
		public float guiShakeMaxAmplitude = 4.0f;
		/** Pain percentage and shock text blink interval in seconds. */
		public float painTextBlinkIntervalSeconds = 0.272f;
		/** GUI 位置: bottom_center / top_left / top_right / bottom_left / bottom_right */
		public String guiPosition = "bottom_center";
		/** GUI 横向偏移（像素） */
		public int guiXOffset = 0;
		/** GUI 纵向偏移（像素） */
		public int guiYOffset = 0;
		/** 是否允许其他玩家救援休克玩家 */
		public boolean rescueEnabled = true;
		/** 救援持续时间（秒） */
		public int rescueDurationSeconds = 3;
		/** 救援完成后保留的疼痛比例 */
		public float rescuePainRatio = 0.75f;
		/** 救援完成后恢复的生命比例 */
		public float rescueHealthRatio = 0.25f;
		/** 救援者与目标允许保持的最大距离 */
		public float rescueMaxDistance = 3.0f;
	public boolean debugLogging = false;
	}
}
