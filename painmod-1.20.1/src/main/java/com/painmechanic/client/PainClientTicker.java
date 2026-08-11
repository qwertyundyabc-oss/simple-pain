package com.painmechanic.client;

import com.painmechanic.PainConfig;
import com.painmechanic.PainMechanic;
import com.painmechanic.ModSounds;
import com.painmechanic.ModStatusEffects;
import com.painmechanic.PainSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * 客户端镜像：让本地玩家的移动/攻击预测与服务器端的疼痛限制一致，
 * 避免客户端先跳/先跑再被服务器纠正。
 */
public final class PainClientTicker {
	private static final Identifier SPEED_MODIFIER = PainMechanic.id("pain_speed");
	private static final Identifier DAMAGE_MODIFIER = PainMechanic.id("pain_damage");
	private static final Identifier ATTACK_SPEED_MODIFIER = PainMechanic.id("pain_attack_speed");
	private static final Identifier ADRENALINE_SPEED_MODIFIER = PainMechanic.id("adrenaline_speed");
	private static final Identifier ADRENALINE_DAMAGE_MODIFIER = PainMechanic.id("adrenaline_damage");
	private static final Identifier ADRENALINE_ATTACK_SPEED_MODIFIER = PainMechanic.id("adrenaline_attack_speed");
	private static PainDroneSoundInstance painDrone;
	private static float painDroneVolume;
	private static PainDroneSoundInstance criticalSound;
	private static SoundEvent criticalSoundEvent;
	private static float criticalSoundVolume;
	private static PainDroneSoundInstance endSound;
	private static float endSoundVolume;
	private static DyingSoundInstance dyingSound;
	private static DeathSoundInstance deathSound;
	private static boolean deathAudioPlayed;
	private static boolean dyingSequenceActive;
	private static boolean dyingAudioStarted;
	private static long dyingFadeStartMs;
	private static long dyingAudioStartMs;
	private static final float CRITICAL_FADE_SECONDS = 0.3f;
	private static final float END_FADE_SECONDS = 2.0f;
	private static final float END_START_VOLUME = 0.001f;
	private static final long DYING_FADE_MILLIS = 500L;
	private static final long DYING_AUDIO_MILLIS = 19_929L;

	private PainClientTicker() {
	}

	public static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			reset(client);
			return;
		}
		if (!player.isAlive()) {
			tickDeathAudio(client);
			return;
		}
		if (deathAudioPlayed) {
			resetDeathAudio(client);
		}
		if (PainClientState.dying) {
			tickDying(client);
			return;
		}
		if (dyingSequenceActive) {
			resetDying(client);
		}
		float pain = PainClientState.pain;
		float maxHealth = Math.max(1f, PainClientState.maxHealth);
		float painRatio = pain / maxHealth;
		updateAdrenalineState(player);
		boolean adrenaline = PainClientState.adrenalineActive;
		boolean shock = PainConfig.get().shockEnabled && painRatio > 1f && !adrenaline;
		boolean endAudio = player.getHealth() <= 1.0f || shock;

		float reduction;
		if (adrenaline) {
			reduction = 0f;
		} else if (shock) {
			reduction = 1f;
		} else {
			float threshold = PainConfig.get().debuffThreshold;
			float maxPercent = PainConfig.get().debuffMaxPercent;
			reduction = (pain >= threshold)
				? Math.min(maxPercent / 100f, (pain - threshold) / maxHealth * (maxPercent / 100f))
				: 0f;
		}

		PainSystem.applyModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER, reduction);
		PainSystem.applyModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_MODIFIER, reduction);
		PainSystem.applyModifier(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED_MODIFIER, reduction);
		float adrenalineBonus = adrenaline ? 0.25f : 0f;
		PainSystem.applyBonusModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, ADRENALINE_SPEED_MODIFIER, adrenalineBonus);
		PainSystem.applyBonusModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ADRENALINE_DAMAGE_MODIFIER, adrenalineBonus);
		PainSystem.applyBonusModifier(player, EntityAttributes.GENERIC_ATTACK_SPEED, ADRENALINE_ATTACK_SPEED_MODIFIER, adrenalineBonus);

		if (shock) {
			player.setJumping(false);
		}

		// 两条音轨独立运行：疼痛底噪不会因为临界/休克音效启动而停止。
		updatePainDrone(client, pain, maxHealth);
		if (shock || painRatio > 0.8f) {
			updateCriticalSound(client, shock);
		} else {
			stopCriticalSound(client);
		}
		updateEndSound(client, endAudio);
	}

	private static void updateAdrenalineState(ClientPlayerEntity player) {
		boolean active = player.hasStatusEffect(ModStatusEffects.ADRENALINE);
		PainClientState.adrenalineActive = active;
	}

	public static void reset(MinecraftClient client) {
		stopPainDrone(client);
		stopCriticalSound(client);
		stopEndSound(client);
		resetDying(client);
		resetDeathAudio(client);
		painDroneVolume = 0f;
		PainClientState.reset();
	}

	private static void tickDeathAudio(MinecraftClient client) {
		if (deathAudioPlayed) {
			return;
		}
		client.getSoundManager().stopAll();
		deathSound = new DeathSoundInstance(ModSounds.DEATH);
		client.getSoundManager().play(deathSound);
		deathAudioPlayed = true;
	}

	private static void resetDeathAudio(MinecraftClient client) {
		if (deathSound != null) {
			client.getSoundManager().stop(deathSound);
			deathSound = null;
		}
		deathAudioPlayed = false;
	}

	private static void tickDying(MinecraftClient client) {
		long now = Util.getMeasuringTimeMs();
		if (!dyingSequenceActive) {
			dyingSequenceActive = true;
			dyingAudioStarted = false;
			dyingFadeStartMs = now;
			dyingAudioStartMs = -1L;
		}

		float fade = Math.min(1f, Math.max(0f, (now - dyingFadeStartMs) / (float) DYING_FADE_MILLIS));
		if (painDrone != null) {
			painDrone.setVolume(painDroneVolume * (1f - fade));
		}
		if (criticalSound != null) {
			criticalSound.setVolume(criticalSoundVolume * (1f - fade));
		}
		if (endSound != null) {
			endSound.setVolume(endSoundVolume * (1f - fade));
		}

		if (!dyingAudioStarted && now - dyingFadeStartMs >= DYING_FADE_MILLIS) {
			// Pause and clear every sound category before starting the one-shot death audio.
			client.getSoundManager().pauseAll();
			client.getSoundManager().stopAll();
			painDrone = null;
			criticalSound = null;
			criticalSoundEvent = null;
			endSound = null;
			dyingSound = new DyingSoundInstance(ModSounds.DYING);
			client.getSoundManager().play(dyingSound);
			dyingAudioStarted = true;
			dyingAudioStartMs = now;
		}
	}

	public static float dyingOverlayAlpha() {
		if (!dyingSequenceActive || !dyingAudioStarted || dyingAudioStartMs < 0L) {
			return 0f;
		}
		long elapsed = Util.getMeasuringTimeMs() - dyingAudioStartMs;
		return Math.min(1f, Math.max(0f, elapsed / (float) DYING_AUDIO_MILLIS));
	}

	private static void resetDying(MinecraftClient client) {
		if (dyingSound != null) {
			client.getSoundManager().stop(dyingSound);
			dyingSound = null;
		}
		dyingSequenceActive = false;
		dyingAudioStarted = false;
		dyingFadeStartMs = 0L;
		dyingAudioStartMs = -1L;
	}

	/**
	 * 疼痛无人机：疼痛比例超过阈值后播放，音量 = (疼痛% - 阈值%) / (100% - 阈值%)，
	 * 最高 100%；音量为 0 时停止，直到音量再次大于 0 才继续。
	 */
	private static void updatePainDrone(MinecraftClient client, float pain, float maxHealth) {
		if (!PainConfig.get().painDroneEnabled) {
			stopPainDrone(client);
			return;
		}
		float ratio = maxHealth > 0f ? pain / maxHealth : 0f;
		float threshold = PainConfig.get().painDroneThreshold;
		if (ratio > threshold) {
			float volume = Math.min(1f, (ratio - threshold) / (1f - threshold));
			if (painDrone == null) {
				painDrone = new PainDroneSoundInstance(ModSounds.PAIN_DRONE, SoundCategory.AMBIENT);
				// 必须先设音量再播放：原版会在音量为 0 时跳过播放
				painDroneVolume = volume;
				painDrone.setVolume(painDroneVolume);
				client.getSoundManager().play(painDrone);
			} else {
				// 音量突变时线性淡入/淡出，避免瞬间跳变
				float fadeSeconds = Math.max(0.05f, PainConfig.get().painDroneFadeSeconds);
				float step = 1f / (fadeSeconds * 20f);
				if (painDroneVolume < volume) {
					painDroneVolume = Math.min(volume, painDroneVolume + step);
				} else if (painDroneVolume > volume) {
					painDroneVolume = Math.max(volume, painDroneVolume - step);
				}
				painDrone.setVolume(painDroneVolume);
			}
		} else {
			stopPainDrone(client);
			painDroneVolume = 0f;
		}
	}

	private static void updateCriticalSound(MinecraftClient client, boolean shock) {
		SoundEvent desired = shock ? ModSounds.PAIN_UNCONSCIOUS : ModSounds.PAIN_CRITICAL;
		if (criticalSound == null || criticalSoundEvent != desired) {
			stopCriticalSound(client);
			criticalSoundEvent = desired;
			criticalSound = new PainDroneSoundInstance(desired, SoundCategory.AMBIENT);
			// SoundManager 可能跳过音量为 0 的声音，因此从极低音量开始淡入。
			criticalSoundVolume = 0.001f;
			criticalSound.setVolume(criticalSoundVolume);
			client.getSoundManager().play(criticalSound);
			return;
		}

		float step = 1f / (CRITICAL_FADE_SECONDS * 20f);
		criticalSoundVolume = Math.min(1f, criticalSoundVolume + step);
		criticalSound.setVolume(criticalSoundVolume);
	}

	private static void stopCriticalSound(MinecraftClient client) {
		if (criticalSound != null) {
			client.getSoundManager().stop(criticalSound);
			criticalSound = null;
		}
		criticalSoundEvent = null;
		criticalSoundVolume = 0f;
	}

	private static void updateEndSound(MinecraftClient client, boolean shouldPlay) {
		float step = 1f / (END_FADE_SECONDS * 20f);
		if (shouldPlay) {
			if (endSound == null) {
				endSound = new PainDroneSoundInstance(ModSounds.PAIN_END, SoundCategory.PLAYERS);
				// Minecraft may skip a sound instance that starts at exactly zero volume.
				endSoundVolume = END_START_VOLUME;
				endSound.setVolume(endSoundVolume);
				client.getSoundManager().play(endSound);
			}
			endSoundVolume = Math.min(1f, endSoundVolume + step);
			endSound.setVolume(endSoundVolume);
			return;
		}

		if (endSound != null) {
			endSoundVolume = Math.max(0f, endSoundVolume - step);
			endSound.setVolume(endSoundVolume);
			if (endSoundVolume <= 0f) {
				client.getSoundManager().stop(endSound);
				endSound = null;
			}
		}
	}

	private static void stopEndSound(MinecraftClient client) {
		if (endSound != null) {
			client.getSoundManager().stop(endSound);
			endSound = null;
		}
		endSoundVolume = 0f;
	}

	private static void stopPainDrone(MinecraftClient client) {
		if (painDrone != null) {
			client.getSoundManager().stop(painDrone);
			painDrone = null;
		}
		painDroneVolume = 0f;
	}
}
