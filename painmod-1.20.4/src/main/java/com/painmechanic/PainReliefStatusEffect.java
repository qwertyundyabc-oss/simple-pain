package com.painmechanic;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 止痛效果：持续时间内的疼痛衰减在服务端 tick 中统一处理，
 * 这里只需要保证效果会持续结算即可。
 */
public class PainReliefStatusEffect extends StatusEffect {
	protected PainReliefStatusEffect(StatusEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return false;
	}
}
