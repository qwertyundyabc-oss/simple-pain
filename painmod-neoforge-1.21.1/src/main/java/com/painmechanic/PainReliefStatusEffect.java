package com.painmechanic;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 止痛效果：持续时间内疼痛的削减在服务端 tick 中统一处理，
 * 这里只需要保证效果会持续结算即可。
 */
public class PainReliefStatusEffect extends MobEffect {
	protected PainReliefStatusEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return false;
	}
}
