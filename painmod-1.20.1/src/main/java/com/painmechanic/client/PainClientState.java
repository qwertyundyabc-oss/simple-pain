package com.painmechanic.client;

public final class PainClientState {
	public static float pain;
	public static float maxHealth = 20f;
	public static boolean adrenalineActive;
	public static boolean dying;
	public static int shockRemainingTicks;

	public static void reset() {
		pain = 0f;
		maxHealth = 20f;
		adrenalineActive = false;
		dying = false;
		shockRemainingTicks = 0;
		PainHud.resetVignetteAnimation();
	}

	private PainClientState() {
	}
}
