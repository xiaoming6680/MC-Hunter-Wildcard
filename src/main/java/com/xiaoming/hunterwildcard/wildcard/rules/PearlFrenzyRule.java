package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class PearlFrenzyRule implements WildcardRule {
    private static final int REFILL_INTERVAL_TICKS = 900;
    private static final int MAX_PEARLS = 4;
    private static final int SIDE_EFFECT_TICKS = 600;
    private int ticks;

    @Override
    public String getName() {
        return "珍珠狂潮";
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        for (ServerPlayerEntity player : context.getParticipants()) {
            giveUpTo(player, Items.ENDER_PEARL, 2, MAX_PEARLS);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (remainingTicks <= 0 || ticks % REFILL_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : context.getParticipants()) {
            giveUpTo(player, Items.ENDER_PEARL, 1, MAX_PEARLS);
        }
    }

    @Override
    public void onItemUse(GameContext context, ServerPlayerEntity player, Hand hand, ItemStack stack) {
        if (!stack.isOf(Items.ENDER_PEARL)) {
            return;
        }

        int roll = context.getRandom().nextInt(100);
        if (roll < 40) {
            return;
        }

        if (roll < 65) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (roll < 85) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (roll < 95) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, SIDE_EFFECT_TICKS, 0, false, false, true));
        } else if (player.getEntityWorld() instanceof ServerWorld world) {
            player.damage(world, player.getDamageSources().magic(), 2.0F);
        }
        player.sendMessage(Text.literal("珍珠副作用触发。"), false);
    }

    private void giveUpTo(ServerPlayerEntity player, Item item, int amount, int maxHeld) {
        int current = countItem(player, item);
        int toGive = Math.min(amount, Math.max(0, maxHeld - current));
        if (toGive > 0) {
            player.getInventory().offerOrDrop(new ItemStack(item, toGive));
        }
    }

    private int countItem(ServerPlayerEntity player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
