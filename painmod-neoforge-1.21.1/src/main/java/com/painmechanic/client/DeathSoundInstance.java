package com.painmechanic.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** One-shot local playback for the player's death event. */
public final class DeathSoundInstance extends AbstractSoundInstance {
	public DeathSoundInstance(SoundEvent sound) {
		super(sound, SoundSource.PLAYERS, RandomSource.create());
		this.looping = false;
		this.delay = 0;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
		this.volume = 1.0f;
		this.pitch = 1.0f;
	}
}
