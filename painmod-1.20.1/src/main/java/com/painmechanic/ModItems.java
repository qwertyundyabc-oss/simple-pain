package com.painmechanic;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
	public static Item PAIN_RELIEF_POWDER;

	private ModItems() {
	}

	public static void register() {
		Identifier id = PainMechanic.id("pain_relief_powder");
		FoodComponent food = new FoodComponent.Builder()
			.hunger(0)
			.saturationModifier(0f)
			.statusEffect(new StatusEffectInstance(ModStatusEffects.PAIN_RELIEF,
				PainConfig.get().painReliefDurationSeconds * 20), 1.0f)
			.build();
		PAIN_RELIEF_POWDER = new Item(new Item.Settings()
			.maxCount(16)
			.food(food));
		Registry.register(Registries.ITEM, id, PAIN_RELIEF_POWDER);
		Registry.register(Registries.ITEM_GROUP, PainMechanic.id("pain_mechanic_tab"),
			FabricItemGroup.builder()
				.displayName(Text.literal("Simple Pain"))
				.icon(() -> new ItemStack(PAIN_RELIEF_POWDER))
				.entries((context, entries) -> entries.add(PAIN_RELIEF_POWDER))
				.build());
	}
}