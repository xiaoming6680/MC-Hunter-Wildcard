package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class HungerChaseRule implements WildcardRule {
    private static final int EXHAUSTION_INTERVAL_TICKS = 20;
    private static final float NORMAL_EXHAUSTION = 0.08F;
    private static final float LOW_FOOD_EXHAUSTION = 0.03F;
    private static final int LOW_FOOD_LEVEL = 6;
    private static final int HUNGER_EFFECT_TICKS = 45;
    private static final int HUNGER_EFFECT_AMPLIFIER = 1;
    private static final int LOW_FOOD_SLOWNESS_TICKS = 45;
    private static final int FOOD_SPEED_TICKS = 100;
    private static final int HIGH_VALUE_FOOD_SPEED_TICKS = 200;

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks <= 0 || remainingTicks % EXHAUSTION_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : context.getParticipants()) {
            int foodLevel = player.getHungerManager().getFoodLevel();
            player.addExhaustion(foodLevel <= LOW_FOOD_LEVEL ? LOW_FOOD_EXHAUSTION : NORMAL_EXHAUSTION);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, HUNGER_EFFECT_TICKS, HUNGER_EFFECT_AMPLIFIER, false, false, true));
            if (foodLevel <= LOW_FOOD_LEVEL) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, LOW_FOOD_SLOWNESS_TICKS, 0, false, false, true));
            }
        }
    }

    @Override
    public void onPlayerAteFood(GameContext context, ServerPlayerEntity player, ItemStack eatenStack) {
        if (!isFood(eatenStack)) {
            return;
        }

        boolean highValueFood = isHighValueFood(eatenStack);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, highValueFood ? HIGH_VALUE_FOOD_SPEED_TICKS : FOOD_SPEED_TICKS, highValueFood ? 1 : 0, false, false, true));
    }

    private boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.contains(DataComponentTypes.FOOD);
    }

    private boolean isHighValueFood(ItemStack stack) {
        return stack.isOf(Items.GOLDEN_APPLE)
                || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.isOf(Items.GOLDEN_CARROT);
    }
}
