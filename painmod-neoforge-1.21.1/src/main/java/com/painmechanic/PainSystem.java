package com.painmechanic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 鐤肩棝鏈哄埗鏍稿績锛氬彈浼ゅ姞鐥涖€乨ebuff 灞炴€с€佷紤鍏嬨€佷繚鎶ゆ満鍒躲€佽“鍑忋€? */
public final class PainSystem {
	private static final ResourceLocation SPEED_MODIFIER = PainMechanic.id("pain_speed");
	private static final ResourceLocation DAMAGE_MODIFIER = PainMechanic.id("pain_damage");
	private static final ResourceLocation ATTACK_SPEED_MODIFIER = PainMechanic.id("pain_attack_speed");
	private static final ResourceLocation ADRENALINE_SPEED_MODIFIER = PainMechanic.id("adrenaline_speed");
	private static final ResourceLocation ADRENALINE_DAMAGE_MODIFIER = PainMechanic.id("adrenaline_damage");
	private static final ResourceLocation ADRENALINE_ATTACK_SPEED_MODIFIER = PainMechanic.id("adrenaline_attack_speed");
	private static final float ADRENALINE_ATTRIBUTE_BONUS = 0.25f;

	private static final Set<UUID> PROTECTED_HITS = new HashSet<>();
	private static final Map<UUID, Float> HEALTH_BEFORE = new HashMap<>();
	private static final Map<UUID, Float> FINAL_DAMAGE = new HashMap<>();
	private static final Map<UUID, RescueAttempt> RESCUES = new HashMap<>();
	private static final Map<UUID, Pose> SHOCK_POSES = new HashMap<>();
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
		IEventBus bus = NeoForge.EVENT_BUS;
		bus.addListener(PainSystem::onServerStarted);
		bus.addListener(PainSystem::onServerStopping);
		bus.addListener(PainSystem::onServerTick);
		bus.addListener(PainSystem::onPlayerLoggedOut);
		bus.addListener(PainSystem::onIncomingDamage);
		bus.addListener(PainSystem::onDamagePre);
		bus.addListener(PainSystem::onDamagePost);
		bus.addListener(PainSystem::onDeath);
		bus.addListener(PainSystem::onRespawn);
		bus.addListener(PainSystem::onRightClickItem);
		bus.addListener(PainSystem::onRightClickBlock);
		bus.addListener(PainSystem::onLeftClickBlock);
		bus.addListener(PainSystem::onAttackEntity);
		bus.addListener(PainSystem::onBreakBlock);
		bus.addListener(PainSystem::onEntityInteract);
	}

	private static void onServerStarted(ServerStartedEvent event) {
		PainData.onServerStart(event.getServer());
	}

	private static void onServerStopping(ServerStoppingEvent event) {
		PainData.onServerStop(event.getServer());
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
	}

	private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			clearPlayer(player.getUUID());
		}
	}

	public static boolean isInShock(Player player) {
		return PainConfig.get().shockEnabled
			&& !player.hasEffect(ModStatusEffects.ADRENALINE)
			&& PainData.get(player.getUUID()) > player.getMaxHealth();
	}

	public static boolean isDying(Player player) {
		return DYING_STARTED_TICKS.containsKey(player.getUUID());
	}

	/** True only while the automatic death caused by an expired shock timer is being processed. */
	public static boolean isShockDeathInProgress(Player player) {
		return FORCED_DEATHS.contains(player.getUUID());
	}

	/** Remaining rescue-window ticks, including the 20-second death-audio phase. */
	public static int getShockRemainingTicks(Player player) {
		UUID id = player.getUUID();
		if (!isInShock(player) && !isDying(player)) {
			return 0;
		}
		int started = SHOCK_STARTED_TICKS.getOrDefault(id, tickCounter);
		return Math.max(0, shockDurationTicks() + DYING_AUDIO_TICKS - (tickCounter - started));
	}

	private static int shockDurationTicks() {
		return Math.max(1, PainConfig.get().shockDurationSeconds * 20);
	}

	private static boolean isShockLocked(Player player) {
		return isInShock(player) || isDying(player);
	}

	public static boolean hasAdrenaline(Player player) {
		return player.hasEffect(ModStatusEffects.ADRENALINE);
	}

	private static void tryTriggerAdrenaline(ServerPlayer player) {
		if (!player.isAlive() || player.hasEffect(ModStatusEffects.ADRENALINE_COOLDOWN)) {
			return;
		}
		float maxHealth = Math.max(1f, player.getMaxHealth());
		float threshold = maxHealth * PainConfig.get().adrenalineThresholdPercent / 100f;
		if (PainData.get(player.getUUID()) < threshold) {
			return;
		}
		int duration = Math.max(1, PainConfig.get().adrenalineDurationSeconds * 20);
		int cooldown = Math.max(1, PainConfig.get().adrenalineCooldownSeconds * 20);
		player.addEffect(new MobEffectInstance(ModStatusEffects.ADRENALINE,
			duration, 0, false, false, true));
		player.addEffect(new MobEffectInstance(ModStatusEffects.ADRENALINE_COOLDOWN,
			cooldown, 0, false, false, true));
	}

	/**
	 * 璁板綍璇ョ帺瀹舵湰娆′激瀹崇殑鏈€缁堜激瀹筹紙鎶ょ敳/榄旀姉鍑忓厤鍚庛€佹湭灏侀《銆佸惛鏀跺墠锛夈€?	 */
	public static void setFinalDamage(UUID id, float finalDamage) {
		FINAL_DAMAGE.put(id, finalDamage);
	}

	private static float takeFinalDamage(UUID id, float fallback) {
		Float dmg = FINAL_DAMAGE.remove(id);
		return dmg != null ? dmg : fallback;
	}

	private static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			HEALTH_BEFORE.put(player.getUUID(), player.getHealth());
		}
	}

	/** 鎹曡幏鎶ょ敳/榄旀姉鍑忓厤鍚庛€佸惛鏀跺墠鐨勬渶缁堜激瀹筹紙NeoForge 浜嬩欢鐗?DamageCaptureMixin锛夈€?*/
	private static void onDamagePre(LivingDamageEvent.Pre event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			PainSystem.setFinalDamage(player.getUUID(), event.getNewDamage());
		}
	}

	private static void onDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		UUID id = player.getUUID();
		if (FORCED_DEATHS.contains(id)) {
			return;
		}
		float maxHealth = player.getMaxHealth();
		float pain = PainData.get(id);
		PainConfig.PainConfigData config = PainConfig.get();
		if (isDying(player)) {
// 死亡音效阶段冻结疼痛值，后续伤害不能再次增加疼痛。
			HEALTH_BEFORE.remove(id);
			FINAL_DAMAGE.remove(id);
			PROTECTED_HITS.remove(id);
			event.setCanceled(true);
			return;
		}
		if (config.protectionBypassUnavoidable
			&& (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || event.getSource().is(DamageTypes.GENERIC_KILL))) {
			return;
		}
		if (config.protectionEnabled && pain < maxHealth) {
// 保护机制：存活并锁 1 血，致命伤害（最终伤害打空血条的部分）造成的疼痛翻倍
			float healthBefore = HEALTH_BEFORE.getOrDefault(id, maxHealth);
			// 浼樺厛鐢ㄦ崟鑾峰埌鐨勫畬鏁存渶缁堜激瀹筹紱鎹曡幏澶辫触鏃剁敤鏁存鍘熷浼ゅ鍏滃簳锛堥伩鍏嶅彧绠楀綋鍓嶈閲忥級
			float finalDamage = takeFinalDamage(id, healthBefore);
			float lethal = Math.min(Math.max(0f, finalDamage), healthBefore);
			// 鏁存鏈€缁堜激瀹虫寜浼ゅ鍊嶇巼璁扮棝锛岃嚧鍛介儴鍒嗗啀鎸夎嚧鍛藉€嶇巼棰濆璁扮棝
			float painGain = finalDamage * PainConfig.get().damageToPainMultiplier
				+ lethal * (PainConfig.get().lethalDamagePainMultiplier - 1f) * PainConfig.get().damageToPainMultiplier;
			if (!config.shockEnabled && pain + painGain > maxHealth) {
// 关闭休克时，致命伤害预测会把疼痛推过 100%，因此直接死亡。
				return;
			}
			PROTECTED_HITS.add(id);
			PainData.add(id, painGain);
			delayBreathing(id);
			clampToMax(player);
			tryTriggerAdrenaline(player);
			player.setHealth(1f);
			PainNetworking.sendImmediate(player);
			event.setCanceled(true);
		}
	}


	private static boolean canUseItemInShock(ItemStack stack) {
		return stack.get(DataComponents.FOOD) != null || stack.getItem() instanceof PotionItem;
	}

	private static void onDamagePost(LivingDamageEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		UUID id = player.getUUID();
		Float healthBefore = HEALTH_BEFORE.remove(id);
		if (isDying(player)) {
			FINAL_DAMAGE.remove(id);
			PROTECTED_HITS.remove(id);
			return;
		}
		float damageDealt = event.getNewDamage();
		if (damageDealt <= 0f) {
			FINAL_DAMAGE.remove(id);
			return;
		}
		if (PROTECTED_HITS.remove(id)) {
			// 淇濇姢鍛戒腑宸插湪 onDeath 涓寜缈诲€嶈绠楄繃鐤肩棝
			return;
		}
// 最终伤害 = 护甲/魔抗/吸收后实际扣除的生命值
		float finalDamage = healthBefore != null
			? Math.max(0f, healthBefore - player.getHealth())
			: damageDealt;
		PainData.add(id, finalDamage * PainConfig.get().damageToPainMultiplier);
		delayBreathing(id);
		clampToMax(player);
		tryTriggerAdrenaline(player);
		PainNetworking.sendImmediate(player);
	}

	private static void onServerTick(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		tickCounter++;
		PainData.tick();
		tickRescues(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();
			boolean dying = isDying(player);
// 疼痛值保持绝对数值；最大疼痛值（上限）、休克阈值、减益、HUD 均实时跟随当前最大生命值
			if (!dying) {
				clampToMax(player);
			}

			boolean relief = player.hasEffect(ModStatusEffects.PAIN_RELIEF);
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
				// 浼戝厠锛氭寔缁幏寰?3 绉掓棤绮掑瓙榛戞殫鏁堟灉锛堟瘡 1.5 绉掑埛鏂颁竴娆★級
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
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
				if (!player.hasPose(Pose.SWIMMING)) {
					player.setPose(Pose.SWIMMING);
				}
			} else {
				Pose previousPose = SHOCK_POSES.remove(id);
				Boolean previousSwimming = SHOCK_SWIMMING.remove(id);
				if (previousSwimming != null) {
					player.setSwimming(previousSwimming);
				}
				if (previousPose != null) {
					player.setPose(previousPose);
				}
			}
			// 缃戠粶灞備細杩囨护灏忓彉鍖栵紝閬垮厤姣忓悕鐜╁姣忕鍙戦€?20 涓暟鎹寘
			PainNetworking.sendTo(player);
		}
	}

	/** Low-health pain only transitions the player into shock; it never deals damage. */
	private static void tickLowHealthPain(ServerPlayer player, UUID id, boolean relief, boolean adrenaline) {
		if (player.getHealth() > 1f || relief || adrenaline) {
			return;
		}
		PainData.add(id, PainConfig.get().lowHealthPainPerTick);
		clampToMax(player);
	}

	private static void updateBreathing(ServerPlayer player, UUID id) {
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
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			ModSounds.PAIN_BREATHS[soundIndex], SoundSource.PLAYERS, 1.0f,
			0.96f + player.getRandom().nextFloat() * 0.08f);

		float intensity = Math.max(0f, Math.min(1f, (painRatio - 0.5f) / 0.5f));
		int interval = Math.round(80f - intensity * 40f);
		NEXT_BREATH_TICKS.put(id, tickCounter + Math.max(30, interval));
	}

	private static void delayBreathing(UUID id) {
		NEXT_BREATH_TICKS.remove(id);
		BREATHING_ACTIVE_AFTER_TICKS.put(id, tickCounter + BREATHING_START_DELAY_TICKS);
	}

	private static void tickShock(ServerPlayer player, UUID id) {
		int started = SHOCK_STARTED_TICKS.computeIfAbsent(id, ignored -> tickCounter);
		if (tickCounter - started >= shockDurationTicks()) {
			DYING_STARTED_TICKS.put(id, tickCounter);
			PainNetworking.sendImmediate(player);
		}
	}

	private static boolean tickDying(ServerPlayer player, UUID id) {
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

	private static void forceDeath(ServerPlayer player) {
		if (player.isAlive()) {
			UUID id = player.getUUID();
			FORCED_DEATHS.add(id);
			try {
				// Complete the LivingEntity death path directly so invulnerability or creative mode cannot reject it.
				DamageSource source = player.damageSources().fellOutOfWorld();
				player.setHealth(0f);
				player.die(source);
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
			ServerPlayer rescuer = server.getPlayerList().getPlayer(attempt.rescuer);
			ServerPlayer target = server.getPlayerList().getPlayer(attempt.target);
			if (rescuer == null || target == null || !rescuer.isAlive() || !target.isAlive()
				|| !rescuer.isShiftKeyDown() || isShockLocked(rescuer)
				|| (!isInShock(target) && !isDying(target))
				|| rescuer.distanceToSqr(target) > config.rescueMaxDistance * config.rescueMaxDistance) {
				iterator.remove();
				continue;
			}
			attempt.ticks++;
			if (attempt.ticks % 20 == 1) {
				int remaining = Math.max(1, (duration - attempt.ticks + 19) / 20);
				rescuer.displayClientMessage(Component.translatable("pain_mechanic.rescue.progress", remaining), true);
			}
			if (attempt.ticks < duration) {
				continue;
			}
			float maxHealth = Math.max(1f, target.getMaxHealth());
			UUID targetId = target.getUUID();
			PainData.set(target.getUUID(), maxHealth * config.rescuePainRatio);
			target.setHealth(Math.max(1f, Math.min(maxHealth, maxHealth * config.rescueHealthRatio)));
			target.removeEffect(MobEffects.DARKNESS);
			SHOCK_STARTED_TICKS.remove(targetId);
			DYING_STARTED_TICKS.remove(targetId);
			PainNetworking.sendImmediate(target);
			rescuer.displayClientMessage(Component.translatable("pain_mechanic.rescue.success"), true);
			target.displayClientMessage(Component.translatable("pain_mechanic.rescue.recovered"), true);
			iterator.remove();
		}
	}

	private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer newPlayer) {
			clearPlayer(newPlayer.getUUID());
			PainData.set(newPlayer.getUUID(), 0f);
			PainNetworking.sendImmediate(newPlayer);
		}
	}

	// 浼戝厠鐘舵€侊細绂佹浠讳綍鎿嶄綔
	private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		if (!isShockLocked(player) || (isInShock(player) && canUseItemInShock(player.getItemInHand(event.getHand())))) {
			return;
		}
		event.setCanceled(true);
	}

	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (isShockLocked(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (isShockLocked(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	private static void onAttackEntity(AttackEntityEvent event) {
		if (isShockLocked(event.getEntity())) {
			event.setCanceled(true);
		}
	}

	private static void onBreakBlock(BlockEvent.BreakEvent event) {
		if (isShockLocked(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (event.getLevel().isClientSide()) {
			return;
		}
		if (!(event.getEntity() instanceof ServerPlayer rescuer)
			|| !(event.getTarget() instanceof ServerPlayer target)) {
			return;
		}
		PainConfig.PainConfigData config = PainConfig.get();
		boolean targetInShock = isInShock(target) || isDying(target);
		if (!config.rescueEnabled || !rescuer.isShiftKeyDown() || isShockLocked(rescuer)
			|| !target.isAlive() || !targetInShock
			|| !rescuer.getItemInHand(event.getHand()).isEmpty()
			|| rescuer.distanceToSqr(target) > config.rescueMaxDistance * config.rescueMaxDistance) {
			return;
		}
		for (RescueAttempt attempt : RESCUES.values()) {
			if (attempt.target.equals(target.getUUID()) && !attempt.rescuer.equals(rescuer.getUUID())) {
				rescuer.displayClientMessage(Component.translatable("pain_mechanic.rescue.busy"), true);
				return;
			}
		}
		RescueAttempt current = RESCUES.get(rescuer.getUUID());
		if (current == null) {
			RESCUES.put(rescuer.getUUID(), new RescueAttempt(rescuer.getUUID(), target.getUUID()));
		} else if (!current.target.equals(target.getUUID())) {
			rescuer.displayClientMessage(Component.translatable("pain_mechanic.rescue.already_started"), true);
			return;
		}
		rescuer.displayClientMessage(Component.translatable("pain_mechanic.rescue.started"), true);
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

	private static void updateAttributes(ServerPlayer player, boolean shock) {
		float pain = PainData.get(player.getUUID());
		float maxHealth = player.getMaxHealth();
		float reduction;
		if (hasAdrenaline(player)) {
			reduction = 0f;
		} else if (shock) {
			// 浼戝厠锛氬畬鍏ㄧ鐢ㄧЩ鍔?鏀诲嚮
			reduction = 1f;
		} else {
			float threshold = PainConfig.get().debuffThreshold;
			float maxPercent = PainConfig.get().debuffMaxPercent;
			if (pain >= threshold && maxHealth > 0f) {
				// 鍑忕泭 = (鐤肩棝鍊?- 闃堝€? / 鏈€澶х敓鍛藉€?* 鏈€澶х櫨鍒嗘瘮锛屾渶澶氭墸闄?maxPercent%
				reduction = (pain - threshold) / maxHealth * (maxPercent / 100f);
				reduction = Math.max(0f, Math.min(maxPercent / 100f, reduction));
			} else {
				reduction = 0f;
			}
		}
		applyModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, reduction);
		applyModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, reduction);
		applyModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, reduction);
		float adrenalineBonus = hasAdrenaline(player) ? ADRENALINE_ATTRIBUTE_BONUS : 0f;
		applyBonusModifier(player, Attributes.MOVEMENT_SPEED, ADRENALINE_SPEED_MODIFIER, adrenalineBonus);
		applyBonusModifier(player, Attributes.ATTACK_DAMAGE, ADRENALINE_DAMAGE_MODIFIER, adrenalineBonus);
		applyBonusModifier(player, Attributes.ATTACK_SPEED, ADRENALINE_ATTACK_SPEED_MODIFIER, adrenalineBonus);
	}

	public static void applyModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, float reduction) {
		applyModifierValue(entity, attribute, id, -reduction);
	}

	public static void applyBonusModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, float bonus) {
		applyModifierValue(entity, attribute, id, bonus);
	}

	private static void applyModifierValue(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, float value) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		if (value != 0f) {
			AttributeModifier existing = instance.getModifier(id);
			// 鐤肩棝鍊煎彉鍖栧鑷村噺鐩婂彉鍖栨椂锛屽疄鏃舵洿鏂颁慨楗扮锛涘皬鍙樺寲鍔犳鍖洪伩鍏嶆瘡 tick 閲嶅彂
			if (existing == null || Math.abs(existing.amount() - value) > 0.005) {
				if (existing != null) {
					instance.removeModifier(existing);
				}
				instance.addTransientModifier(new AttributeModifier(id, value,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		} else {
			AttributeModifier existing = instance.getModifier(id);
			if (existing != null) {
				instance.removeModifier(existing);
			}
		}
	}

	/**
	 * 灏嗙柤鐥涘€奸挸鍒跺湪鏈€澶х敓鍛藉€肩殑 maxPainMultiplier 鍊嶄互鍐咃紱鍏抽棴浼戝厠鏃堕攣瀹氫负 100%銆?	 */
	private static void clampToMax(ServerPlayer player) {
		if (!PainConfig.get().shockEnabled) {
			float cap = player.getMaxHealth();
			float pain = PainData.get(player.getUUID());
			if (pain > cap) {
				PainData.set(player.getUUID(), cap);
			}
			return;
		}
		float multiplier = PainConfig.get().maxPainMultiplier;
		if (multiplier <= 0f) {
			return;
		}
		float cap = player.getMaxHealth() * multiplier;
		float pain = PainData.get(player.getUUID());
		if (pain > cap) {
			PainData.set(player.getUUID(), cap);
		}
	}
}

