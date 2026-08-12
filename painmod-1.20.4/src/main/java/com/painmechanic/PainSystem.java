package com.painmechanic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

/**
 * 疼痛机制核心：受伤加痛、debuff 属性、休克、保护机制、衰减。
 */
public final class PainSystem {
	private static final Identifier SPEED_MODIFIER = PainMechanic.id("pain_speed");
	private static final Identifier DAMAGE_MODIFIER = PainMechanic.id("pain_damage");
	private static final Identifier ATTACK_SPEED_MODIFIER = PainMechanic.id("pain_attack_speed");
	private static final Identifier ADRENALINE_SPEED_MODIFIER = PainMechanic.id("adrenaline_speed");
	private static final Identifier ADRENALINE_DAMAGE_MODIFIER = PainMechanic.id("adrenaline_damage");
	private static final Identifier ADRENALINE_ATTACK_SPEED_MODIFIER = PainMechanic.id("adrenaline_attack_speed");
	private static final float ADRENALINE_ATTRIBUTE_BONUS = 0.25f;

	private static final Set<UUID> PROTECTED_HITS = new HashSet<>();
	private static final Map<UUID, Float> HEALTH_BEFORE = new HashMap<>();
	private static final Map<UUID, Float> FINAL_DAMAGE = new HashMap<>();
	private static final Map<UUID, RescueAttempt> RESCUES = new HashMap<>();
	private static final Map<UUID, EntityPose> SHOCK_POSES = new HashMap<>();
	private static final Map<UUID, Boolean> SHOCK_SWIMMING = new HashMap<>();
	private static final Map<UUID, Integer> NEXT_BREATH_TICKS = new HashMap<>();
	private static final Map<UUID, Integer> BREATHING_ACTIVE_AFTER_TICKS = new HashMap<>();
	private static final Map<UUID, Integer> SHOCK_STARTED_TICKS = new HashMap<>();
	private static final Map<UUID, Integer> DYING_STARTED_TICKS = new HashMap<>();
	private static final Set<UUID> FORCED_DEATHS = new HashSet<>();
	private static final int BREATHING_START_DELAY_TICKS = 80;
	private static final int DYING_FADE_TICKS = 10;
	private static final int DYING_AUDIO_TICKS = 400;
	private static final int DYING_TOTAL_TICKS = DYING_FADE_TICKS + DYING_AUDIO_TICKS;
	private static int tickCounter;

	private static final class RescueAttempt {
		private final UUID rescuer;
		private final UUID target;
		private int ticks;

		private RescueAttempt(UUID rescuer, UUID target) {
			this.rescuer = rescuer;
			this.target = target;
		}
	}

	private PainSystem() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(PainData::onServerStart);
		ServerLifecycleEvents.SERVER_STOPPING.register(PainData::onServerStop);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			RESCUES.clear();
			SHOCK_POSES.clear();
			SHOCK_SWIMMING.clear();
			NEXT_BREATH_TICKS.clear();
			BREATHING_ACTIVE_AFTER_TICKS.clear();
			SHOCK_STARTED_TICKS.clear();
			DYING_STARTED_TICKS.clear();
			FORCED_DEATHS.clear();
			HEALTH_BEFORE.clear();
			FINAL_DAMAGE.clear();
			PROTECTED_HITS.clear();
			PainNetworking.clearAll();
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearPlayer(handler.getPlayer().getUuid()));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(PainSystem::onAllowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.register(PainSystem::onAllowDeath);
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity player) {
				clearPlayer(player.getUuid());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(PainSystem::onServerTick);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			clearPlayer(oldPlayer.getUuid());
			PainData.set(newPlayer.getUuid(), 0f);
			PainNetworking.sendImmediate(newPlayer);
		});

		// 休克状态：禁止任何操作
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!isShockLocked(player) || (isInShock(player) && canUseItemInShock(player.getStackInHand(hand)))) {
				return TypedActionResult.pass(player.getStackInHand(hand));
			}
			return TypedActionResult.fail(player.getStackInHand(hand));
		});
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> isShockLocked(player) ? ActionResult.FAIL : ActionResult.PASS);
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> isShockLocked(player) ? ActionResult.FAIL : ActionResult.PASS);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> isShockLocked(player) ? ActionResult.FAIL : ActionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> !isShockLocked(player));
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClient() || !(player instanceof ServerPlayerEntity rescuer)
				|| !(entity instanceof ServerPlayerEntity target)) {
				return ActionResult.PASS;
			}
			PainConfig.PainConfigData config = PainConfig.get();
			boolean targetInShock = isInShock(target) || isDying(target);
			if (!config.rescueEnabled || !rescuer.isSneaking() || isShockLocked(rescuer)
				|| !target.isAlive() || !targetInShock
				|| !rescuer.getStackInHand(hand).isEmpty()
				|| rescuer.squaredDistanceTo(target) > config.rescueMaxDistance * config.rescueMaxDistance) {
				return ActionResult.PASS;
			}
			for (RescueAttempt attempt : RESCUES.values()) {
				if (attempt.target.equals(target.getUuid()) && !attempt.rescuer.equals(rescuer.getUuid())) {
					rescuer.sendMessage(Text.translatable("pain_mechanic.rescue.busy"), true);
					return ActionResult.SUCCESS;
				}
			}
			RescueAttempt current = RESCUES.get(rescuer.getUuid());
			if (current == null) {
				RESCUES.put(rescuer.getUuid(), new RescueAttempt(rescuer.getUuid(), target.getUuid()));
			} else if (!current.target.equals(target.getUuid())) {
				rescuer.sendMessage(Text.translatable("pain_mechanic.rescue.already_started"), true);
				return ActionResult.SUCCESS;
			}
			rescuer.sendMessage(Text.translatable("pain_mechanic.rescue.started"), true);
			return ActionResult.SUCCESS;
		});
	}

	public static boolean isInShock(PlayerEntity player) {
		return PainConfig.get().shockEnabled
			&& !player.hasStatusEffect(ModStatusEffects.ADRENALINE)
			&& PainData.get(player.getUuid()) > player.getMaxHealth();
	}

	public static boolean isDying(PlayerEntity player) {
		return DYING_STARTED_TICKS.containsKey(player.getUuid());
	}

	/** True only while the automatic death caused by an expired shock timer is being processed. */
	public static boolean isShockDeathInProgress(PlayerEntity player) {
		return FORCED_DEATHS.contains(player.getUuid());
	}

	/** Remaining rescue-window ticks, including the 20-second death-audio phase. */
	public static int getShockRemainingTicks(PlayerEntity player) {
		UUID id = player.getUuid();
		if (!isInShock(player) && !isDying(player)) {
			return 0;
		}
		int started = SHOCK_STARTED_TICKS.getOrDefault(id, tickCounter);
		return Math.max(0, shockDurationTicks() + DYING_AUDIO_TICKS - (tickCounter - started));
	}

	private static int shockDurationTicks() {
		return Math.max(1, PainConfig.get().shockDurationSeconds * 20);
	}

	private static boolean isShockLocked(PlayerEntity player) {
		return isInShock(player) || isDying(player);
	}

	public static boolean hasAdrenaline(PlayerEntity player) {
		return player.hasStatusEffect(ModStatusEffects.ADRENALINE);
	}

	private static void tryTriggerAdrenaline(ServerPlayerEntity player) {
		if (!player.isAlive() || player.hasStatusEffect(ModStatusEffects.ADRENALINE_COOLDOWN)) {
			return;
		}
		float maxHealth = Math.max(1f, player.getMaxHealth());
		float threshold = maxHealth * PainConfig.get().adrenalineThresholdPercent / 100f;
		if (PainData.get(player.getUuid()) < threshold) {
			if (PainConfig.get().debugLogging) {
				PainMechanic.LOGGER.info("[Simple Pain][dbg] adrenaline skip uuid={} pain={} threshold={}", player.getUuid(), PainData.get(player.getUuid()), threshold);
			}
			return;
		}
		int duration = Math.max(1, PainConfig.get().adrenalineDurationSeconds * 20);
		int cooldown = Math.max(1, PainConfig.get().adrenalineCooldownSeconds * 20);
		if (PainConfig.get().debugLogging) {
			PainMechanic.LOGGER.info("[Simple Pain][dbg] adrenaline TRIGGER uuid={}", player.getUuid());
		}
		player.addStatusEffect(new StatusEffectInstance(ModStatusEffects.ADRENALINE,
			duration, 0, false, false, true));
		player.addStatusEffect(new StatusEffectInstance(ModStatusEffects.ADRENALINE_COOLDOWN,
			cooldown, 0, false, false, true));
	}

	/**
	 * 记录该玩家本次伤害的最终伤害（护甲/魔抗减免后、未封顶、吸收前）。
	 */
	public static void setFinalDamage(UUID id, float finalDamage) {
		FINAL_DAMAGE.put(id, finalDamage);
	}

	private static float takeFinalDamage(UUID id, float fallback) {
		Float dmg = FINAL_DAMAGE.remove(id);
		return dmg != null ? dmg : fallback;
	}

	private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (entity instanceof ServerPlayerEntity player) {
			HEALTH_BEFORE.put(player.getUuid(), player.getHealth());
			if (PainConfig.get().debugLogging) {
				PainMechanic.LOGGER.info("[Simple Pain][dbg] ALLOW_DAMAGE uuid={} healthBefore={} amount={}", player.getUuid(), player.getHealth(), amount);
			}
		}
		return true;
	}

	private static boolean onAllowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return true;
		}
		UUID id = player.getUuid();
		if (FORCED_DEATHS.contains(id)) {
			return true;
		}
		float maxHealth = player.getMaxHealth();
		float pain = PainData.get(id);
		PainConfig.PainConfigData config = PainConfig.get();
		if (isDying(player)) {
			// 死亡音效阶段冻结疼痛值，后续伤害不能再次增加疼痛。
			HEALTH_BEFORE.remove(id);
			FINAL_DAMAGE.remove(id);
			PROTECTED_HITS.remove(id);
			return false;
		}
		if (config.protectionBypassUnavoidable
			&& (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL))) {
			return true;
		}
		if (config.protectionEnabled && pain < maxHealth) {
			// 保护机制：存活并锁 1 血，致命伤害（最终伤害打空血条的部分）造成的疼痛翻倍
			float healthBefore = HEALTH_BEFORE.getOrDefault(id, maxHealth);
			// 优先用捕获到的完整最终伤害；捕获失败时用整段原始伤害兜底（避免只算当前血量）
			float finalDamage = takeFinalDamage(id, Math.max(amount, healthBefore));
			float lethal = Math.min(Math.max(0f, finalDamage), healthBefore);
			// 整段最终伤害按伤害倍率记痛，致命部分再按致命倍率额外记痛
			float painGain = finalDamage * PainConfig.get().damageToPainMultiplier
				+ lethal * (PainConfig.get().lethalDamagePainMultiplier - 1f) * PainConfig.get().damageToPainMultiplier;
			if (!config.shockEnabled && pain + painGain > maxHealth) {
				// 关闭休克时，致命伤害预测会把疼痛推过 100%，因此直接死亡。
				return true;
			}
			// 1.20.4 无 AFTER_DAMAGE 事件，保护命中后 applyDamage 不会执行，疼痛已在上面计算。
			PROTECTED_HITS.add(id);
			PainData.add(id, painGain);
			delayBreathing(id);
			clampToMax(player);
			tryTriggerAdrenaline(player);
			player.setHealth(1f);
			PainNetworking.sendImmediate(player);
			return false;
		}
		return true;
	}

	private static boolean canUseItemInShock(ItemStack stack) {
		return stack.isFood() || stack.getItem() instanceof PotionItem;
	}

	public static void onAfterDamageApplied(ServerPlayerEntity player, float fallbackDamage) {
		UUID id = player.getUuid();
		Float healthBefore = HEALTH_BEFORE.remove(id);
		if (PainConfig.get().debugLogging) {
			PainMechanic.LOGGER.info("[Simple Pain][dbg] AFTER_DAMAGE uuid={} healthBefore={} health={} fallback={} dying={}", id, healthBefore, player.getHealth(), fallbackDamage, isDying(player));
		}
		if (isDying(player)) {
			FINAL_DAMAGE.remove(id);
			PROTECTED_HITS.remove(id);
			return;
		}
		float finalDamage = healthBefore != null
			? Math.max(0f, healthBefore - player.getHealth())
			: Math.max(0f, fallbackDamage);
		if (finalDamage <= 0f) {
			if (PainConfig.get().debugLogging) {
				PainMechanic.LOGGER.info("[Simple Pain][dbg] AFTER_DAMAGE final<=0 uuid={} final={}", id, finalDamage);
			}
			FINAL_DAMAGE.remove(id);
			return;
		}
		if (PROTECTED_HITS.remove(id)) {
			// 保护命中已在 onAllowDeath 中按翻倍计算过疼痛
			return;
		}
		PainData.add(id, finalDamage * PainConfig.get().damageToPainMultiplier);
		if (PainConfig.get().debugLogging) {
			PainMechanic.LOGGER.info("[Simple Pain][dbg] AFTER_DAMAGE pain+= {} uuid={} total={}", finalDamage * PainConfig.get().damageToPainMultiplier, id, PainData.get(id));
		}
		delayBreathing(id);
		clampToMax(player);
		tryTriggerAdrenaline(player);
		PainNetworking.sendImmediate(player);
	}

	private static void onServerTick(MinecraftServer server) {
		tickCounter++;
		PainData.tick();
		tickRescues(server);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			UUID id = player.getUuid();
			boolean dying = isDying(player);
			// 疼痛值保持绝对数值；最大疼痛值（上限）、休克阈值、减益、HUD 均实时跟随当前最大生命值
			if (!dying) {
				clampToMax(player);
			}

			boolean relief = player.hasStatusEffect(ModStatusEffects.PAIN_RELIEF);
			boolean adrenaline = hasAdrenaline(player);
			float maxHealth = Math.max(1f, player.getMaxHealth());
			if (dying) {
				// 濒死阶段播放最后 20 秒死亡音效，疼痛值固定为进入该阶段时的数值。
			} else if (relief || adrenaline) {
				float decay = maxHealth * PainConfig.get().painReliefDecayPerSecond / 100f;
				PainData.add(id, -decay / 20f);
			} else if (player.getHealth() > 1f) {
				float decay = maxHealth * PainConfig.get().painDecayPerSecond / 100f;
				if (player.getHealth() >= player.getMaxHealth()) {
					decay *= PainConfig.get().fullHealthPainDecayMultiplier;
				}
				PainData.add(id, -decay / 20f);
			}
			if (!dying) {
				tickLowHealthPain(player, id, relief, adrenaline);
			}

			boolean shock = isInShock(player);
			updateBreathing(player, id);
			if (dying) {
				// 濒死阶段只推进自己的倒计时，不能再次进入休克计时，否则起始 tick 会被每 tick 重置。
				if (tickDying(player, id)) {
					continue;
				}
			} else if (shock) {
				tickShock(player, id);
			} else {
				SHOCK_STARTED_TICKS.remove(id);
				DYING_STARTED_TICKS.remove(id);
			}
			if (shock && PainConfig.get().shockDarkness && tickCounter % 30 == 0) {
				// 休克：持续获得 3 秒无粒子黑暗效果（每 1.5 秒刷新一次）
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false, true));
			}
			boolean shockLocked = shock || dying;
			if (shockLocked) {
				// 休克时持续清除跳跃输入。
				player.setJumping(false);
			}
			updateAttributes(player, shockLocked);
			if (shockLocked) {
				SHOCK_POSES.putIfAbsent(id, player.getPose());
				SHOCK_SWIMMING.putIfAbsent(id, player.isSwimming());
				player.setSwimming(true);
				if (!player.isInPose(EntityPose.SWIMMING)) {
					player.setPose(EntityPose.SWIMMING);
				}
			} else {
				EntityPose previousPose = SHOCK_POSES.remove(id);
				Boolean previousSwimming = SHOCK_SWIMMING.remove(id);
				if (previousSwimming != null) {
					player.setSwimming(previousSwimming);
				}
				if (previousPose != null) {
					player.setPose(previousPose);
				}
			}
			// 网络层会过滤小变化，避免每名玩家每秒发送 20 个数据包
			PainNetworking.sendTo(player);
		}
	}

	/** Low-health pain only transitions the player into shock; it never deals damage. */
	private static void tickLowHealthPain(ServerPlayerEntity player, UUID id, boolean relief, boolean adrenaline) {
		if (player.getHealth() > 1f || relief || adrenaline) {
			return;
		}
		PainData.add(id, PainConfig.get().lowHealthPainPerTick);
		clampToMax(player);
	}

	private static void updateBreathing(ServerPlayerEntity player, UUID id) {
		if (!PainConfig.get().breathingEnabled) {
			NEXT_BREATH_TICKS.remove(id);
			BREATHING_ACTIVE_AFTER_TICKS.remove(id);
			return;
		}
		float maxHealth = Math.max(1f, player.getMaxHealth());
		float painRatio = PainData.get(id) / maxHealth;
		if (!player.isAlive() || painRatio <= 0.5f || hasAdrenaline(player)) {
			NEXT_BREATH_TICKS.remove(id);
			BREATHING_ACTIVE_AFTER_TICKS.remove(id);
			return;
		}
		if (isInShock(player)) {
			NEXT_BREATH_TICKS.remove(id);
			return;
		}

		int activeAfter = BREATHING_ACTIVE_AFTER_TICKS.getOrDefault(id, 0);
		if (tickCounter < activeAfter) {
			return;
		}
		BREATHING_ACTIVE_AFTER_TICKS.remove(id);

		int nextTick = NEXT_BREATH_TICKS.getOrDefault(id, 0);
		if (tickCounter < nextTick) {
			return;
		}

		int soundIndex = player.getRandom().nextInt(ModSounds.PAIN_BREATHS.length);
		player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
			ModSounds.PAIN_BREATHS[soundIndex], SoundCategory.PLAYERS, 1.0f,
			0.96f + player.getRandom().nextFloat() * 0.08f);

		float intensity = Math.max(0f, Math.min(1f, (painRatio - 0.5f) / 0.5f));
		int interval = Math.round(80f - intensity * 40f);
		NEXT_BREATH_TICKS.put(id, tickCounter + Math.max(30, interval));
	}

	private static void delayBreathing(UUID id) {
		NEXT_BREATH_TICKS.remove(id);
		BREATHING_ACTIVE_AFTER_TICKS.put(id, tickCounter + BREATHING_START_DELAY_TICKS);
	}

	private static void tickShock(ServerPlayerEntity player, UUID id) {
		int started = SHOCK_STARTED_TICKS.computeIfAbsent(id, ignored -> tickCounter);
		if (tickCounter - started >= shockDurationTicks()) {
			DYING_STARTED_TICKS.put(id, tickCounter);
			PainNetworking.sendImmediate(player);
		}
	}

	private static boolean tickDying(ServerPlayerEntity player, UUID id) {
		int started = DYING_STARTED_TICKS.getOrDefault(id, tickCounter);
		if (tickCounter - started < DYING_TOTAL_TICKS) {
			return false;
		}

		forceDeath(player);
		if (!player.isAlive()) {
			return true;
		}
		return false;
	}

	private static void forceDeath(ServerPlayerEntity player) {
		if (player.isAlive()) {
			UUID id = player.getUuid();
			FORCED_DEATHS.add(id);
			try {
				// Complete the LivingEntity death path directly so invulnerability or creative mode cannot reject it.
				DamageSource source = player.getDamageSources().outOfWorld();
				player.setHealth(0f);
				player.onDeath(source);
			} finally {
				FORCED_DEATHS.remove(id);
			}
		}
	}

	private static void tickRescues(MinecraftServer server) {
		PainConfig.PainConfigData config = PainConfig.get();
		int duration = Math.max(1, config.rescueDurationSeconds * 20);
		var iterator = RESCUES.entrySet().iterator();
		while (iterator.hasNext()) {
			RescueAttempt attempt = iterator.next().getValue();
			ServerPlayerEntity rescuer = server.getPlayerManager().getPlayer(attempt.rescuer);
			ServerPlayerEntity target = server.getPlayerManager().getPlayer(attempt.target);
			if (rescuer == null || target == null || !rescuer.isAlive() || !target.isAlive()
				|| !rescuer.isSneaking() || isShockLocked(rescuer)
				|| (!isInShock(target) && !isDying(target))
				|| rescuer.squaredDistanceTo(target) > config.rescueMaxDistance * config.rescueMaxDistance) {
				iterator.remove();
				continue;
			}
			attempt.ticks++;
			if (attempt.ticks % 20 == 1) {
				int remaining = Math.max(1, (duration - attempt.ticks + 19) / 20);
				rescuer.sendMessage(Text.translatable("pain_mechanic.rescue.progress", remaining), true);
			}
			if (attempt.ticks < duration) {
				continue;
			}
			float maxHealth = Math.max(1f, target.getMaxHealth());
			UUID targetId = target.getUuid();
			PainData.set(target.getUuid(), maxHealth * config.rescuePainRatio);
			target.setHealth(Math.max(1f, Math.min(maxHealth, maxHealth * config.rescueHealthRatio)));
			target.removeStatusEffect(StatusEffects.DARKNESS);
			SHOCK_STARTED_TICKS.remove(targetId);
			DYING_STARTED_TICKS.remove(targetId);
			PainNetworking.sendImmediate(target);
			rescuer.sendMessage(Text.translatable("pain_mechanic.rescue.success"), true);
			target.sendMessage(Text.translatable("pain_mechanic.rescue.recovered"), true);
			iterator.remove();
		}
	}

	private static void clearPlayer(UUID id) {
		HEALTH_BEFORE.remove(id);
		FINAL_DAMAGE.remove(id);
		PROTECTED_HITS.remove(id);
		RESCUES.entrySet().removeIf(entry -> entry.getKey().equals(id) || entry.getValue().target.equals(id));
		SHOCK_POSES.remove(id);
		SHOCK_SWIMMING.remove(id);
		NEXT_BREATH_TICKS.remove(id);
		BREATHING_ACTIVE_AFTER_TICKS.remove(id);
		SHOCK_STARTED_TICKS.remove(id);
		DYING_STARTED_TICKS.remove(id);
		FORCED_DEATHS.remove(id);
		PainNetworking.clear(id);
	}

	private static void updateAttributes(ServerPlayerEntity player, boolean shock) {
		float pain = PainData.get(player.getUuid());
		float maxHealth = player.getMaxHealth();
		float reduction;
		if (hasAdrenaline(player)) {
			reduction = 0f;
		} else if (shock) {
			// 休克：完全禁用移动/攻击
			reduction = 1f;
		} else {
			float threshold = PainConfig.get().debuffThreshold;
			float maxPercent = PainConfig.get().debuffMaxPercent;
			if (pain >= threshold && maxHealth > 0f) {
				// 减益 = (疼痛值 - 阈值) / 最大生命值 * 最大百分比，最多扣除 maxPercent%
				reduction = (pain - threshold) / maxHealth * (maxPercent / 100f);
				reduction = Math.max(0f, Math.min(maxPercent / 100f, reduction));
			} else {
				reduction = 0f;
			}
		}
		applyModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER, reduction);
		applyModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_MODIFIER, reduction);
		applyModifier(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED_MODIFIER, reduction);
		float adrenalineBonus = hasAdrenaline(player) ? ADRENALINE_ATTRIBUTE_BONUS : 0f;
		applyBonusModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, ADRENALINE_SPEED_MODIFIER, adrenalineBonus);
		applyBonusModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ADRENALINE_DAMAGE_MODIFIER, adrenalineBonus);
		applyBonusModifier(player, EntityAttributes.GENERIC_ATTACK_SPEED, ADRENALINE_ATTACK_SPEED_MODIFIER, adrenalineBonus);
	}

	public static void applyModifier(LivingEntity entity, EntityAttribute attribute, Identifier id, float reduction) {
		applyModifierValue(entity, attribute, id, -reduction);
	}

	public static void applyBonusModifier(LivingEntity entity, EntityAttribute attribute, Identifier id, float bonus) {
		applyModifierValue(entity, attribute, id, bonus);
	}

	private static void applyModifierValue(LivingEntity entity, EntityAttribute attribute, Identifier id, float value) {
		EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
		if (instance == null) {
			return;
		}
		UUID modifierId = modifierUuid(id);
		if (value != 0f) {
			EntityAttributeModifier existing = instance.getModifier(modifierId);
			// 疼痛值变化导致减益变化时，实时更新修饰符；小变化加死区避免每 tick 重发
			if (existing == null || Math.abs(existing.getValue() - value) > 0.005) {
				instance.removeModifier(modifierId);
				instance.addTemporaryModifier(new EntityAttributeModifier(modifierId, id.toString(), value,
					EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
			}
		} else {
			instance.removeModifier(modifierId);
		}
	}

	private static UUID modifierUuid(Identifier id) {
		return UUID.nameUUIDFromBytes(id.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	/**
	 * 将疼痛值钳制在最大生命值的 maxPainMultiplier 倍以内；关闭休克时锁定为 100%。
	 */
	private static void clampToMax(ServerPlayerEntity player) {
		if (!PainConfig.get().shockEnabled) {
			float cap = player.getMaxHealth();
			float pain = PainData.get(player.getUuid());
			if (pain > cap) {
				PainData.set(player.getUuid(), cap);
			}
			return;
		}
		float multiplier = PainConfig.get().maxPainMultiplier;
		if (multiplier <= 0f) {
			return;
		}
		float cap = player.getMaxHealth() * multiplier;
		float pain = PainData.get(player.getUuid());
		if (pain > cap) {
			PainData.set(player.getUuid(), cap);
		}
	}
}
