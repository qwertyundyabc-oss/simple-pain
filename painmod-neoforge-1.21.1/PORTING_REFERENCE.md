# Simple Pain — Fabric 1.21.1 (Yarn) → NeoForge 21.1.248 (Mojang) 移植参考

## 规则
- 玩法逻辑、常量、判断顺序 100% 照搬 Fabric 源码，只替换 API 名称与事件机制。
- 保留全部中文注释；代码用 Tab 缩进；类名/包名不变。
- 源文件：`src/main/java-com-backup/com/painmechanic/...`（Fabric 版）
- 目标目录：`src/main/java/com/painmechanic/...`
- 已写好的公共类不要动：PainMechanic, PainConfig, PainData, PainSyncPayload, PainNetworking, ModItems, ModSounds, ModStatusEffects, AdrenalineCooldownStatusEffect, AdrenalineStatusEffect, PainReliefStatusEffect。

## 已验证的 API 对照（NeoForge 21.1.248 / Mojang 官方映射）
| Yarn (Fabric) | Mojang (NeoForge) |
|---|---|
| MinecraftClient | net.minecraft.client.Minecraft |
| ClientPlayerEntity | net.minecraft.client.player.LocalPlayer |
| ServerPlayerEntity | net.minecraft.server.level.ServerPlayer |
| PlayerEntity | net.minecraft.world.entity.player.Player |
| LivingEntity | net.minecraft.world.entity.LivingEntity (包名不变) |
| Identifier | net.minecraft.resources.ResourceLocation |
| Text.literal/translatable | Component.literal / Component.translatable (net.minecraft.network.chat.Component) |
| Text.styled(s -> s.withBold(true)) | Component.withStyle(s -> s.withBold(true)) |
| DrawContext | net.minecraft.client.gui.GuiGraphics |
| TextRenderer | net.minecraft.client.gui.Font（Minecraft.getInstance().font） |
| textRenderer.getWidth(t) | font.width(t) |
| context.getScaledWindowWidth/Height | guiWidth() / guiHeight() |
| context.getMatrices() | context.pose() (PoseStack push/pop/translate/scale 不变) |
| context.setShaderColor(r,g,b,a) | context.setColor(r,g,b,a) |
| context.drawText(font,t,x,y,color,shadow) | context.drawString(font,t,x,y,color,shadow) |
| context.fill | context.fill (不变) |
| EntityPose | net.minecraft.world.entity.Pose（Pose.SWIMMING） |
| player.isInPose(pose) | player.hasPose(pose) |
| StatusEffectInstance | net.minecraft.world.effect.MobEffectInstance |
| StatusEffect | net.minecraft.world.effect.MobEffect |
| StatusEffectCategory | net.minecraft.world.effect.MobEffectCategory（BENEFICIAL/NEUTRAL/HARMFUL） |
| canApplyUpdateEffect | shouldApplyEffectTickThisTick |
| StatusEffects.DARKNESS | net.minecraft.world.effect.MobEffects.DARKNESS (Holder<MobEffect>) |
| player.hasStatusEffect/removeStatusEffect/addStatusEffect | hasEffect / removeEffect / addEffect（均以 Holder<MobEffect> 为参） |
| entity.getActiveStatusEffects().keySet() | entity.getActiveEffectsMap().keySet() (Map<Holder<MobEffect>, MobEffectInstance>) |
| clearStatusEffects | removeAllEffects |
| EntityAttributes.GENERIC_MOVEMENT_SPEED/ATTACK_DAMAGE/ATTACK_SPEED | Attributes.MOVEMENT_SPEED / ATTACK_DAMAGE / ATTACK_SPEED (net.minecraft.world.entity.ai.attributes.Attributes, Holder<Attribute>) |
| RegistryEntry<EntityAttribute> | net.minecraft.core.Holder<Attribute> |
| EntityAttributeInstance | net.minecraft.world.entity.ai.attributes.AttributeInstance |
| entity.getAttributeInstance(attr) | entity.getAttribute(attr) |
| instance.getModifier(id) | instance.getModifier(ResourceLocation) |
| instance.removeModifier(id) | instance.removeModifier(modifier对象)（先取 existing 再移除） |
| instance.addTemporaryModifier(m) | instance.addTransientModifier(m) |
| new EntityAttributeModifier(id, value, op) | new AttributeModifier(ResourceLocation, double, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) |
| modifier.value() | modifier.amount() |
| EntityAttributeModifier.Operation | AttributeModifier.Operation |
| DamageTypes.OUT_OF_WORLD / GENERIC_KILL | net.minecraft.world.damagesource.DamageTypes（同名字段） |
| source.isOf(dt) | source.is(dt) |
| player.getDamageSources().outOfWorld() | player.damageSources().outOfWorld() |
| player.onDeath(src) | player.die(src) |
| player.setJumping(false) | player.setJumping(false)（LivingEntity 方法，不变） |
| player.sendMessage(text, true) | player.displayClientMessage(component, true) |
| player.getEntityWorld() | player.level() |
| world.playSound(null,x,y,z,se,cat,v,p) | level().playSound(null,x,y,z,Holder<SoundEvent>,SoundSource,v,p)（1.21.1 有 Holder 重载，ModSounds 的 DeferredHolder 可直接传） |
| SoundCategory.PLAYERS/AMBIENT | net.minecraft.sounds.SoundSource.PLAYERS / AMBIENT |
| player.getStackInHand(hand) | player.getItemInHand(hand)（net.minecraft.world.InteractionHand） |
| player.squaredDistanceTo(e) | player.distanceToSqr(e) |
| player.getUuid() | player.getUUID() |
| server.getPlayerManager().getPlayerList() | server.getPlayerList().getPlayers() |
| server.getPlayerManager().getPlayer(uuid) | server.getPlayerList().getPlayer(uuid) |
| player.getRandom() | player.getRandom() (net.minecraft.util.RandomSource) |
| Util.getMeasuringTimeMs() | net.minecraft.Util.getMillis() |
| FoodComponent | net.minecraft.world.food.FoodProperties（Builder.nutrition/saturationModifier/effect 同名；effect 有 Supplier<MobEffectInstance> 重载） |
| DataComponentTypes.FOOD | net.minecraft.core.component.DataComponents.FOOD |
| Item.Settings | Item.Properties（stacksTo/food 同名） |
| PotionItem | net.minecraft.world.item.PotionItem（包名变化） |
| SoundEvent.of(id) | SoundEvent.createVariableRangeEvent(ResourceLocation) |

## ModSounds / ModStatusEffects / ModItems 用法注意
- ModStatusEffects.ADRENALINE 等是 DeferredHolder<MobEffect, ?>（即 Holder<MobEffect>），直接传给 hasEffect/addEffect(new MobEffectInstance(ModStatusEffects.ADRENALINE, dur, 0, false, false, true))。
- ModSounds.PAIN_BREATHS 是 DeferredHolder<SoundEvent, SoundEvent>[]；playSound 传 holder 即可；若要 SoundEvent 用 .get()。
- ModItems.PAIN_RELIEF_POWDER.get() 返回 Item。

## 事件对照（全部注册在 NeoForge.EVENT_BUS = net.neoforged.neoforge.common.NeoForge.EVENT_BUS）
| Fabric | NeoForge 21.1.248 |
|---|---|
| ServerLifecycleEvents.SERVER_STARTED | net.neoforged.neoforge.event.server.ServerStartedEvent（event.getServer()） |
| ServerLifecycleEvents.SERVER_STOPPING | net.neoforged.neoforge.event.server.ServerStoppingEvent |
| ServerTickEvents.END_SERVER_TICK | net.neoforged.neoforge.event.tick.TickEvent.ServerTickEvent.Post（event.getServer()；监听器签名直接写 ServerTickEvent.Post） |
| ServerPlayConnectionEvents.DISCONNECT | net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent（event.getEntity()） |
| ServerLivingEntityEvents.ALLOW_DAMAGE | net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent（event.getEntity(), getSource(), getAmount()；记录 HEALTH_BEFORE） |
| （新增，替代 DamageCaptureMixin） | net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre：event.getNewDamage() = 护甲+魔抗减免后、吸收前的最终伤害 → setFinalDamage(uuid, value) |
| ServerLivingEntityEvents.AFTER_DAMAGE | net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post：event.getNewDamage() = 实际扣除生命值（吸收后）→ 用作 damageDealt |
| ServerLivingEntityEvents.ALLOW_DEATH | net.neoforged.neoforge.event.entity.living.LivingDeathEvent：event.setCanceled(true) 阻止死亡；注意此时玩家 health 已是 0，保护路径中先算疼痛再 setHealth(1f) 再取消 |
| ServerLivingEntityEvents.AFTER_DEATH | 删除；清理逻辑保留在 respawn/disconnect |
| ServerPlayerEvents.AFTER_RESPAWN | net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent：clearPlayer(uuid) + PainData.set(uuid,0) + sendImmediate |
| UseItemCallback | net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem（event.getEntity()=玩家, getHand()；setCanceled(true) 阻止） |
| UseBlockCallback | PlayerInteractEvent.RightClickBlock（setCanceled） |
| AttackBlockCallback | PlayerInteractEvent.LeftClickBlock（setCanceled） |
| AttackEntityCallback | net.neoforged.neoforge.event.entity.player.AttackEntityEvent（setCanceled） |
| PlayerBlockBreakEvents.BEFORE | net.neoforged.neoforge.event.level.BlockEvent.BreakEvent（event.getPlayer()；setCanceled） |
| UseEntityCallback（救援） | PlayerInteractEvent.EntityInteract（getEntity()=玩家, getTarget()=目标实体, getHand()；先判断 event.getLevel().isClientSide() 返回） |

- 交互/破坏事件客户端也会触发，但 PainSystem 的 isInShock/isDying 在客户端读到的是空数据（返回 false），取消判断天然只在服务端生效；救援逻辑必须显式判断 isClientSide。
- 事件注册写法：NeoForge.EVENT_BUS.addListener(PainSystem::onServerStarted); 等。

## 客户端（Agent B 专属）
- PainMechanicClient 新结构（不再是 ModInitializer）：
  ```java
  public final class PainMechanicClient {
      public static void init(IEventBus modEventBus) {
          modEventBus.addListener(PainMechanicClient::registerGuiLayers);
          NeoForge.EVENT_BUS.addListener(PainMechanicClient::onClientTick);
      }
      private static void registerGuiLayers(RegisterGuiLayersEvent event) {
          event.registerAboveAll(PainMechanic.id("pain_bar"), (guiGraphics, deltaTracker) -> PainHud.render(guiGraphics));
          event.registerAboveAll(PainMechanic.id("pain_vignette"), (guiGraphics, deltaTracker) -> PainHud.renderVignette(guiGraphics));
          event.registerAboveAll(PainMechanic.id("pain_dying_overlay"), (guiGraphics, deltaTracker) -> PainHud.renderDyingOverlay(guiGraphics));
      }
      private static void onClientTick(ClientTickEvent.Post event) {
          PainClientTicker.tick(Minecraft.getInstance());
      }
  }
  ```
  包：net.neoforged.neoforge.client.event.RegisterGuiLayersEvent / ClientTickEvent；net.neoforged.bus.api.IEventBus；net.neoforged.neoforge.common.NeoForge。
  RegisterGuiLayersEvent.registerAboveAll(ResourceLocation, LayeredDraw.Layer) — Layer 是函数式接口 render(GuiGraphics, DeltaTracker)（DeltaTracker = net.minecraft.client.DeltaTracker）。
- PainHud：见 API 表；注意 blit 重载：
  - drawTexture(id, x, y, 0f, 0f, w, h, tw, th) → blit(id, x, y, 0f, 0f, w, h, tw, th)
  - drawTexture(id, 0, 0, sw, sh, 128-half, 128-half, region, region, 256, 256) → blit(id, 0, 0, sw, sh, (float)(128-half), (float)(128-half), region, region, 256, 256)
  - RenderSystem.enableBlend() 不变（com.mojang.blaze3d.systems.RenderSystem）
  - client.currentScreen != null → client.screen != null
- PainClientTicker：MinecraftClient→Minecraft；ClientPlayerEntity→LocalPlayer；EntityAttributes→Attributes；SoundCategory→SoundSource；client.getSoundManager().pauseAll()→pause()；stopAll()→stop()；ModSounds.xxx 传 holder 或 .get()；new PainDroneSoundInstance(ModSounds.PAIN_DRONE.get(), SoundSource.AMBIENT)；Minecraft.getInstance().player 不变；player.input.jump = false（input 字段在 LocalPlayer，jumping→jump）。
- 声音实例（3 个文件）：
  ```java
  import net.minecraft.client.resources.sounds.AbstractSoundInstance;
  import net.minecraft.client.resources.sounds.SoundInstance;
  import net.minecraft.client.resources.sounds.TickableSoundInstance;
  import net.minecraft.sounds.SoundEvent;
  import net.minecraft.sounds.SoundSource;
  import net.minecraft.util.RandomSource;
  ```
  构造：super(sound, category, RandomSource.create())；
  字段：this.looping = true/false（Yarn repeat）；this.delay = 0（Yarn repeatDelay）；this.relative = true；this.attenuation = SoundInstance.Attenuation.NONE（Yarn attenuationType）；x/y/z/volume/pitch 同名。
  TickableSoundInstance 只有 tick() 方法（没有 isDone()），PainDroneSoundInstance 保留 setVolume + tick()。
- ModMenuIntegration.java 不移植（NeoForge 版 ModMenu 无该 API），直接删除该文件（Agent B 负责从备份目录里忽略它，并在目标目录不要创建）。

## Mixin（Agent C 专属）
- 目标目录 src/main/java/com/painmechanic/mixin/；配置 pain_mechanic.mixins.json 已更新（共 9 个：4 common + 5 client）。
- 类名映射：InGameHud→net.minecraft.client.gui.Gui；GameMenuScreen→net.minecraft.client.gui.screens.PauseScreen；Screen 不变（renderWithTooltip(GuiGraphics,int,int,float) 不变）；DamageTracker→net.minecraft.world.damagesource.CombatTracker（死亡消息方法 getDeathMessage()；字段是 @Shadow @Final private LivingEntity mob; —— 字段名是 mob！）
- 方法映射：jump→jumpFromGround；tickMovement→aiStep；updateSwimming 不变（Player.updateSwimming()）；updatePose→updatePlayerPose；clearStatusEffects→removeAllEffects；input.jumping→input.jump。
- InGameHudGuiShakeMixin：@Mixin(Gui.class)，method = "render"，签名 (GuiGraphics, DeltaTracker, CallbackInfo)。
- 客户端判断：FabricLoader...EnvType → net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT。
- PlayerJumpMixin：@Mixin(LivingEntity.class) method "jumpFromGround"，逻辑不变。
- PreserveAdrenalineCooldownMixin：@Mixin(LivingEntity.class) method "removeAllEffects"，返回 CallbackInfoReturnable<Boolean>；遍历 entity.getActiveEffectsMap().keySet()；entity.removeEffect(effect)（返回 boolean）；ModStatusEffects.ADRENALINE_COOLDOWN 是 Holder，直接比较/传参。
- LivingEntityShockPoseMixin / LivingEntityShockPoseClientMixin：@Mixin(Player.class)，方法 updateSwimming 与 updatePlayerPose；客户端版判断 Minecraft.getInstance().player 与 PainClientState。
- ClientPlayerShockMixin：@Mixin(LocalPlayer.class)，method "aiStep"，input.jump = false。
- 不要修改 mixins.json（除非编译/运行证明有问题）。

## 通用注意
- 不要引入 Fabric 的任何 import。
- 不要改动数值、阈值、tick 逻辑、消息 key（pain_mechanic.*）。
- PainSystem 里保留 forceDeath、tickRescues、updateBreathing 等全部逻辑。
- debugLogging 字段保留（配置里已有），无需新增日志逻辑。
