package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class WindChargeBrawlRule implements WildcardRule {
    private static final int MAX_CHARGES = 16;
    private int ticks;

    @Override
    public String getName() {
        return "风弹乱斗";
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        for (ServerPlayerEntity player : context.getParticipants()) {
            giveUpTo(player, Items.WIND_CHARGE, 1, MAX_CHARGES);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (remainingTicks <= 0 || ticks % context.getConfig().getWindChargeBrawlIntervalTicks() != 0) {
            return;
        }

        for (ServerPlayerEntity player : context.getParticipants()) {
            giveUpTo(player, Items.WIND_CHARGE, 1, MAX_CHARGES);
        }
    }

    public float getExplosionPowerMultiplier(ModConfig config) {
        return config.getWindChargeExplosionMultiplier();
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
