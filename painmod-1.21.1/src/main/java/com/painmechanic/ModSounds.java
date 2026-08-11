package com.painmechanic;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

public final class ModSounds {
	public static final SoundEvent PAIN_DRONE = SoundEvent.of(PainMechanic.id("pain_drone"));
	public static final SoundEvent PAIN_CRITICAL = SoundEvent.of(PainMechanic.id("pain_critical"));
	public static final SoundEvent PAIN_UNCONSCIOUS = SoundEvent.of(PainMechanic.id("pain_unconscious"));
	public static final SoundEvent PAIN_END = SoundEvent.of(PainMechanic.id("pain_end"));
	public static final SoundEvent DYING = SoundEvent.of(PainMechanic.id("dying"));
	public static final SoundEvent DEATH = SoundEvent.of(PainMechanic.id("death"));
	public static final SoundEvent[] PAIN_BREATHS = {
		SoundEvent.of(PainMechanic.id("pain_breath_39")),
		SoundEvent.of(PainMechanic.id("pain_breath_40")),
		SoundEvent.of(PainMechanic.id("pain_breath_41")),
		SoundEvent.of(PainMechanic.id("pain_breath_42")),
		SoundEvent.of(PainMechanic.id("pain_breath_43"))
	};

	private ModSounds() {
	}

	public static void register() {
		Registry.register(Registries.SOUND_EVENT, PainMechanic.id("pain_drone"), PAIN_DRONE);
		Registry.register(Registries.SOUND_EVENT, PainMechanic.id("pain_critical"), PAIN_CRITICAL);
		Registry.register(Registries.SOUND_EVENT, PainMechanic.id("pain_unconscious"), PAIN_UNCONSCIOUS);
		Registry.register(Registries.SOUND_EVENT, PainMechanic.id("pain_end"), PAIN_END);
		Registry.register(Registries.SOUND_EVENT, PainMechanic.id("dying"), DYING);
		Registry.register(Registries.SOUND_EVENT, PainMechanic.id("death"), DEATH);
		for (int i = 39; i <= 43; i++) {
			Registry.register(Registries.SOUND_EVENT, PainMechanic.id("pain_breath_" + i), PAIN_BREATHS[i - 39]);
		}
	}
}
