package com.painmechanic.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * 疼痛无人机音效：循环播放、无衰减、音量可动态调整。
 */
public class PainDroneSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
	private boolean stopped;

	public PainDroneSoundInstance(SoundEvent sound, SoundSource category) {
		super(sound, category, RandomSource.create());
		this.looping = true;
		this.delay = 0;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
		this.volume = 0f;
		this.pitch = 1f;
	}

	public void setVolume(float v) {
		this.volume = Math.max(0f, Math.min(1f, v));
	}

	@Override
	public boolean isStopped() {
		return stopped;
	}

	public void stop() {
		this.stopped = true;
	}

	@Override
	public void tick() {
		// 音量通过 setVolume 实时更新，由 SoundSystem 每 tick 重新读取
	}
}