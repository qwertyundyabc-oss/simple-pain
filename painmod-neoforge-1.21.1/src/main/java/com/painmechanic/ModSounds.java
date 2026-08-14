package com.painmechanic;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS =
		DeferredRegister.create(Registries.SOUND_EVENT, PainMechanic.MOD_ID);
	public static final DeferredHolder<SoundEvent, SoundEvent> PAIN_DRONE =
		SOUNDS.register("pain_drone", () -> SoundEvent.createVariableRangeEvent(PainMechanic.id("pain_drone")));
	public static final DeferredHolder<SoundEvent, SoundEvent> PAIN_CRITICAL =
		SOUNDS.register("pain_critical", () -> SoundEvent.createVariableRangeEvent(PainMechanic.id("pain_critical")));
	public static final DeferredHolder<SoundEvent, SoundEvent> PAIN_UNCONSCIOUS =
		SOUNDS.register("pain_unconscious", () -> SoundEvent.createVariableRangeEvent(PainMechanic.id("pain_unconscious")));
	public static final DeferredHolder<SoundEvent, SoundEvent> PAIN_END =
		SOUNDS.register("pain_end", () -> SoundEvent.createVariableRangeEvent(PainMechanic.id("pain_end")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DYING =
		SOUNDS.register("dying", () -> SoundEvent.createVariableRangeEvent(PainMechanic.id("dying")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DEATH =
		SOUNDS.register("death", () -> SoundEvent.createVariableRangeEvent(PainMechanic.id("death")));
	public static final DeferredHolder<SoundEvent, SoundEvent>[] PAIN_BREATHS = painBreaths();

	@SuppressWarnings("unchecked")
	private static DeferredHolder<SoundEvent, SoundEvent>[] painBreaths() {
		DeferredHolder<SoundEvent, SoundEvent>[] breaths = new DeferredHolder[5];
		for (int i = 0; i < 5; i++) {
			int index = 39 + i;
			breaths[i] = SOUNDS.register("pain_breath_" + index,
				() -> SoundEvent.createVariableRangeEvent(PainMechanic.id("pain_breath_" + index)));
		}
		return breaths;
	}

	private ModSounds() {
	}

	public static void register(IEventBus modEventBus) {
		SOUNDS.register(modEventBus);
	}
}
