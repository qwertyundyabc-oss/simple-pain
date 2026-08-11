package com.painmechanic.client;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

/**
 * 疼痛无人机音效：循环播放、无衰减、音量可动态调整。
 */
public class PainDroneSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
	public PainDroneSoundInstance(SoundEvent sound, SoundCategory category) {
		super(sound, category, Random.create());
		this.repeat = true;
		this.repeatDelay = 0;
		this.relative = true;
		this.attenuationType = SoundInstance.AttenuationType.NONE;
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
	public void tick() {
		// 音量通过 setVolume 实时更新，由 SoundSystem 每 tick 重新读取
	}

	@Override
	public boolean isDone() {
		return false;
	}
}
