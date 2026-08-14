package com.painmechanic.client;

import com.painmechanic.PainConfig;
import com.painmechanic.PainMechanic;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Cloth Config 配置界面（NeoForge 版）。
 * 通过 NeoForge IConfigScreenFactory 扩展点注册，Mod Menu (NeoForge) 和模组列表中会出现"配置"按钮。
 */
public final class PainConfigScreen {
	private PainConfigScreen() {
	}

	public static Screen create(Screen parent) {
		if (!isClothConfigAvailable()) {
			PainMechanic.LOGGER.warn("[Simple Pain] Cloth Config 未安装，无法打开配置界面。");
			return parent;
		}
		try {
			return createInternal(parent);
		} catch (LinkageError | RuntimeException e) {
			PainMechanic.LOGGER.error("[Simple Pain] 创建配置界面失败，请检查 Cloth Config 版本。", e);
			return parent;
		}
	}

	private static boolean isClothConfigAvailable() {
		try {
			Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder", false, PainConfigScreen.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}

	private static Screen createInternal(Screen parent) {
		PainConfig.PainConfigData pending = PainConfig.copy();
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable("pain_mechanic.config.title"))
			.setDoesConfirmSave(true)
			.setSavingRunnable(() -> PainConfig.savePending(pending));
		ConfigEntryBuilder entries = builder.entryBuilder();

		addNotice(builder.getOrCreateCategory(Component.translatable("pain_mechanic.config.category.general")), entries);
		addGameplay(builder.getOrCreateCategory(Component.translatable("pain_mechanic.config.category.gameplay")), entries, pending);
		addVisuals(builder.getOrCreateCategory(Component.translatable("pain_mechanic.config.category.visuals")), entries, pending);
		if (hasSoundResources()) {
			addAudio(builder.getOrCreateCategory(Component.translatable("pain_mechanic.config.category.audio")), entries, pending);
		}
		addRescue(builder.getOrCreateCategory(Component.translatable("pain_mechanic.config.category.rescue")), entries, pending);
		addDebug(builder.getOrCreateCategory(Component.translatable("pain_mechanic.config.category.debug")), entries, pending);
		return builder.build();
	}

	private static void addNotice(ConfigCategory category, ConfigEntryBuilder entries) {
		category.addEntry(entries.startTextDescription(
			Component.translatable("pain_mechanic.config.restart_notice")).build());
	}

	private static void addGameplay(ConfigCategory category, ConfigEntryBuilder e, PainConfig.PainConfigData c) {
		category.addEntry(e.startFloatField(key("damageToPainMultiplier"), c.damageToPainMultiplier).setMin(0f).setSaveConsumer(v -> c.damageToPainMultiplier = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("lethalDamagePainMultiplier"), c.lethalDamagePainMultiplier).setMin(0f).setSaveConsumer(v -> c.lethalDamagePainMultiplier = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("maxPainMultiplier"), c.maxPainMultiplier).setMin(0f).setSaveConsumer(v -> c.maxPainMultiplier = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("painDecayPerSecond"), c.painDecayPerSecond).setMin(0f).setMax(100f).setSaveConsumer(v -> c.painDecayPerSecond = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("fullHealthPainDecayMultiplier"), c.fullHealthPainDecayMultiplier).setMin(0f).setSaveConsumer(v -> c.fullHealthPainDecayMultiplier = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("painReliefDecayPerSecond"), c.painReliefDecayPerSecond).setMin(0f).setMax(100f).setSaveConsumer(v -> c.painReliefDecayPerSecond = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("lowHealthPainPerTick"), c.lowHealthPainPerTick).setMin(0f).setSaveConsumer(v -> c.lowHealthPainPerTick = v).requireRestart().build());
		category.addEntry(e.startIntField(key("painReliefDurationSeconds"), c.painReliefDurationSeconds).setMin(1).setSaveConsumer(v -> c.painReliefDurationSeconds = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("adrenalineThresholdPercent"), c.adrenalineThresholdPercent).setMin(0f).setMax(100f).setSaveConsumer(v -> c.adrenalineThresholdPercent = v).requireRestart().build());
		category.addEntry(e.startIntField(key("adrenalineDurationSeconds"), c.adrenalineDurationSeconds).setMin(1).setSaveConsumer(v -> c.adrenalineDurationSeconds = v).requireRestart().build());
		category.addEntry(e.startIntField(key("adrenalineCooldownSeconds"), c.adrenalineCooldownSeconds).setMin(1).setSaveConsumer(v -> c.adrenalineCooldownSeconds = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("debuffThreshold"), c.debuffThreshold).setMin(0f).setSaveConsumer(v -> c.debuffThreshold = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("debuffMaxPercent"), c.debuffMaxPercent).setMin(0f).setMax(100f).setSaveConsumer(v -> c.debuffMaxPercent = v).requireRestart().build());
		category.addEntry(e.startBooleanToggle(key("protectionEnabled"), c.protectionEnabled).setSaveConsumer(v -> c.protectionEnabled = v).requireRestart().build());
		category.addEntry(e.startBooleanToggle(key("protectionBypassUnavoidable"), c.protectionBypassUnavoidable).setSaveConsumer(v -> c.protectionBypassUnavoidable = v).requireRestart().build());
		category.addEntry(e.startBooleanToggle(key("shockEnabled"), c.shockEnabled)
			.setTooltip(Component.translatable("pain_mechanic.config.shockEnabled.tooltip"))
			.setSaveConsumer(v -> c.shockEnabled = v).requireRestart().build());
		category.addEntry(e.startIntField(key("shockDurationSeconds"), c.shockDurationSeconds).setMin(1).setSaveConsumer(v -> c.shockDurationSeconds = v).requireRestart().build());
		category.addEntry(e.startBooleanToggle(key("shockDarkness"), c.shockDarkness).setSaveConsumer(v -> c.shockDarkness = v).requireRestart().build());
	}

	private static void addVisuals(ConfigCategory category, ConfigEntryBuilder e, PainConfig.PainConfigData c) {
		category.addEntry(e.startBooleanToggle(key("redVignetteEnabled"), c.redVignetteEnabled).setSaveConsumer(v -> c.redVignetteEnabled = v).build());
		category.addEntry(e.startFloatField(key("redVignetteThreshold"), c.redVignetteThreshold).setMin(0f).setMax(0.99f).setSaveConsumer(v -> c.redVignetteThreshold = v).build());
		category.addEntry(e.startFloatField(key("guiShakeThreshold"), c.guiShakeThreshold).setMin(0f).setMax(0.99f).setSaveConsumer(v -> c.guiShakeThreshold = v).build());
		category.addEntry(e.startFloatField(key("guiShakeMaxAmplitude"), c.guiShakeMaxAmplitude).setMin(0f).setSaveConsumer(v -> c.guiShakeMaxAmplitude = v).build());
		category.addEntry(e.startFloatField(key("painTextBlinkIntervalSeconds"), c.painTextBlinkIntervalSeconds).setMin(0.05f).setMax(10f).setSaveConsumer(v -> c.painTextBlinkIntervalSeconds = v).build());
		category.addEntry(e.startSelector(key("guiPosition"), new String[] { "bottom_center", "top_left", "top_right", "bottom_left", "bottom_right" }, c.guiPosition).setSaveConsumer(v -> c.guiPosition = v).build());
		category.addEntry(e.startIntField(key("guiXOffset"), c.guiXOffset).setSaveConsumer(v -> c.guiXOffset = v).build());
		category.addEntry(e.startIntField(key("guiYOffset"), c.guiYOffset).setSaveConsumer(v -> c.guiYOffset = v).build());
	}

	private static void addAudio(ConfigCategory category, ConfigEntryBuilder e, PainConfig.PainConfigData c) {
		category.addEntry(e.startBooleanToggle(key("painDroneEnabled"), c.painDroneEnabled).setSaveConsumer(v -> c.painDroneEnabled = v).requireRestart().build());
		category.addEntry(e.startBooleanToggle(key("breathingEnabled"), c.breathingEnabled).setSaveConsumer(v -> c.breathingEnabled = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("painDroneThreshold"), c.painDroneThreshold).setMin(0f).setMax(0.99f).setSaveConsumer(v -> c.painDroneThreshold = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("painDroneFadeSeconds"), c.painDroneFadeSeconds).setMin(0.05f).setSaveConsumer(v -> c.painDroneFadeSeconds = v).requireRestart().build());
	}

	private static void addRescue(ConfigCategory category, ConfigEntryBuilder e, PainConfig.PainConfigData c) {
		category.addEntry(e.startBooleanToggle(key("rescueEnabled"), c.rescueEnabled).setSaveConsumer(v -> c.rescueEnabled = v).requireRestart().build());
		category.addEntry(e.startIntField(key("rescueDurationSeconds"), c.rescueDurationSeconds).setMin(1).setSaveConsumer(v -> c.rescueDurationSeconds = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("rescuePainRatio"), c.rescuePainRatio).setMin(0f).setMax(1f).setSaveConsumer(v -> c.rescuePainRatio = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("rescueHealthRatio"), c.rescueHealthRatio).setMin(0.05f).setMax(1f).setSaveConsumer(v -> c.rescueHealthRatio = v).requireRestart().build());
		category.addEntry(e.startFloatField(key("rescueMaxDistance"), c.rescueMaxDistance).setMin(1f).setSaveConsumer(v -> c.rescueMaxDistance = v).requireRestart().build());
	}

	private static void addDebug(ConfigCategory category, ConfigEntryBuilder e, PainConfig.PainConfigData c) {
		category.addEntry(e.startBooleanToggle(key("debugLogging"), c.debugLogging).setSaveConsumer(v -> c.debugLogging = v).requireRestart().build());
	}

	private static boolean hasSoundResources() {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.getResourceManager() == null) {
				return true;
			}
			return client.getResourceManager().getResource(PainMechanic.id("sounds.json")).isPresent();
		} catch (RuntimeException e) {
			return true;
		}
	}

	private static Component key(String name) {
		return Component.translatable("pain_mechanic.config." + name);
	}
}