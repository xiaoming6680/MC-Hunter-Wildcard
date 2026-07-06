package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class LightLoadRule implements WildcardRule {
    private static final int REFRESH_INTERVAL_TICKS = 40;
    private static final int EFFECT_TICKS = 50;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks <= 0 || remainingTicks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : context.getParticipants()) {
            int weight = armorWeight(player);
            if (weight <= 4) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, 1, false, false, true));
            } else if (weight <= 8) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, 0, false, false, true));
            } else if (weight > 12) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_TICKS, 0, false, false, true));
            }
        }
    }

    private int armorWeight(ServerPlayerEntity player) {
        int weight = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            weight += itemWeight(player.getEquippedStack(slot));
        }
        return weight;
    }

    private int itemWeight(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_CHESTPLATE || item == Items.LEATHER_LEGGINGS || item == Items.LEATHER_BOOTS) {
            return 1;
        }
        if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) {
            return 2;
        }
        if (item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS
                || item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS
                || item == Items.COPPER_HELMET || item == Items.COPPER_CHESTPLATE || item == Items.COPPER_LEGGINGS || item == Items.COPPER_BOOTS) {
            return 3;
        }
        if (item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS) {
            return 4;
        }
        if (item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) {
            return 5;
        }
        return 0;
    }
}
