package com.painmechanic;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, PainMechanic.MOD_ID);
	public static final DeferredHolder<Item, Item> PAIN_RELIEF_POWDER = ITEMS.register("pain_relief_powder",
		() -> new Item(new Item.Properties()
			.stacksTo(16)
			.food(new FoodProperties.Builder()
				.nutrition(0)
				.saturationModifier(0f)
				.effect(() -> new MobEffectInstance(ModStatusEffects.PAIN_RELIEF,
					PainConfig.get().painReliefDurationSeconds * 20), 1.0f)
				.build())));

	private static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PainMechanic.MOD_ID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PAIN_TAB = TABS.register("pain_mechanic_tab",
		() -> CreativeModeTab.builder()
			.title(Component.literal("Simple Pain"))
			.icon(() -> new ItemStack(PAIN_RELIEF_POWDER.get()))
			.displayItems((params, output) -> output.accept(new ItemStack(PAIN_RELIEF_POWDER.get())))
			.build());

	private ModItems() {
	}

	public static void register(IEventBus modEventBus) {
		ITEMS.register(modEventBus);
		TABS.register(modEventBus);
	}
}
