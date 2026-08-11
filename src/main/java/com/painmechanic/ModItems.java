package com.painmechanic;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
	public static Item PAIN_RELIEF_POWDER;

	private ModItems() {
	}

	public static void register() {
		Identifier id = PainMechanic.id("pain_relief_powder");
		FoodComponent food = new FoodComponent(0, 0f, true);
		ConsumableComponent consumable = ConsumableComponent.builder()
			.consumeEffect(new ApplyEffectsConsumeEffect(
				new StatusEffectInstance(ModStatusEffects.PAIN_RELIEF,
					PainConfig.get().painReliefDurationSeconds * 20)))
			.build();
		PAIN_RELIEF_POWDER = new Item(new Item.Settings()
			.registryKey(RegistryKey.of(RegistryKeys.ITEM, id))
			.maxCount(16)
			.food(food, consumable));
		Registry.register(Registries.ITEM, id, PAIN_RELIEF_POWDER);
		Registry.register(Registries.ITEM_GROUP, PainMechanic.id("pain_mechanic_tab"),
			FabricItemGroup.builder()
				.displayName(Text.literal("Simple Pain"))
				.icon(() -> new ItemStack(PAIN_RELIEF_POWDER))
				.entries((context, entries) -> entries.add(PAIN_RELIEF_POWDER))
				.build());
	}
}
