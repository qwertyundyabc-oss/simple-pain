package com.painmechanic.client;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;

/** One-shot local playback for the final death sequence. */
public final class DyingSoundInstance extends AbstractSoundInstance {
	public DyingSoundInstance(SoundEvent sound) {
		super(sound, SoundCategory.PLAYERS, Random.create());
		this.repeat = false;
		this.repeatDelay = 0;
		this.relative = true;
		this.attenuationType = SoundInstance.AttenuationType.NONE;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
		this.volume = 1.0f;
		this.pitch = 1.0f;
	}
}
