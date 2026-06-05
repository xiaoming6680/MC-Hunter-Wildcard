package com.xiaoming.hunterwildcard.compass;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.List;
import java.util.Optional;

public class CompassTracker {
    private static final String COMPASS_NAME = "追猎指南针";
    private static final String LEGACY_COMPASS_NAME = "猎人指南针";
    private static final String COMPASS_DATA_KEY = "hunterwildcard_compass";

    private int updateTicks;

    public void reset() {
        updateTicks = 0;
    }

    public void tick(GameContext context, WildcardRule activeRule) {
        updateTicks--;
        if (updateTicks > 0) {
            return;
        }

        updateTicks = context.getConfig().getCompassUpdateTicks();
        updateHunterCompasses(context, activeRule);
    }

    public void onConfigChanged(ModConfig config) {
        if (updateTicks > config.getCompassUpdateTicks()) {
            updateTicks = config.getCompassUpdateTicks();
        }
    }

    public void giveCompasses(GameContext context) {
        for (ServerPlayerEntity hunter : context.getHunters()) {
            giveCompass(hunter);
        }
    }

    public void giveCompass(ServerPlayerEntity hunter) {
        if (ensureSingleHunterCompass(hunter)) {
            return;
        }

        ItemStack stack = createCompass();
        if (!hunter.getInventory().insertStack(stack)) {
            hunter.dropItem(stack, false);
        }
    }

    public void clear(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            removeCompass(player);
        }
    }

    public void removeCompass(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (isHunterCompass(stack)) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
    }

    private void updateHunterCompasses(GameContext context, WildcardRule activeRule) {
        List<ServerPlayerEntity> runners = context.getRunners();
        if (runners.isEmpty()) {
            return;
        }

        for (ServerPlayerEntity hunter : context.getHunters()) {
            giveCompass(hunter);
            ServerPlayerEntity runner = PlayerUtil.findNearestRunner(hunter, runners);
            if (runner == null) {
                continue;
            }

            BlockPos target = runner.getBlockPos();
            if (activeRule != null) {
                target = activeRule.modifyCompassTarget(context, hunter, runner, target);
            }

            GlobalPos globalPos = GlobalPos.create(runner.getEntityWorld().getRegistryKey(), target);
            LodestoneTrackerComponent tracker = new LodestoneTrackerComponent(Optional.of(globalPos), false);
            updateCompassStacks(hunter, tracker);
        }
    }

    private void updateCompassStacks(ServerPlayerEntity hunter, LodestoneTrackerComponent tracker) {
        boolean updated = false;
        for (int slot = 0; slot < hunter.getInventory().size(); slot++) {
            ItemStack stack = hunter.getInventory().getStack(slot);
            if (isHunterCompass(stack)) {
                if (updated) {
                    hunter.getInventory().setStack(slot, ItemStack.EMPTY);
                    continue;
                }

                markHunterCompass(stack);
                stack.set(DataComponentTypes.LODESTONE_TRACKER, tracker);
                updated = true;
            }
        }
    }

    private boolean ensureSingleHunterCompass(ServerPlayerEntity hunter) {
        boolean found = false;
        for (int slot = 0; slot < hunter.getInventory().size(); slot++) {
            ItemStack stack = hunter.getInventory().getStack(slot);
            if (isHunterCompass(stack)) {
                if (found) {
                    hunter.getInventory().setStack(slot, ItemStack.EMPTY);
                    continue;
                }

                markHunterCompass(stack);
                found = true;
            }
        }

        return found;
    }

    private boolean isHunterCompass(ItemStack stack) {
        if (!stack.isOf(Items.COMPASS)) {
            return false;
        }

        return isTaggedHunterCompass(stack) || isLegacyHunterCompass(stack);
    }

    private boolean isTaggedHunterCompass(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return false;
        }

        return data.copyNbt().getBoolean(COMPASS_DATA_KEY, false);
    }

    private boolean isLegacyHunterCompass(ItemStack stack) {
        String name = stack.getName().getString();
        return (name.equals(COMPASS_NAME) || name.equals(LEGACY_COMPASS_NAME))
                && Boolean.TRUE.equals(stack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE));
    }

    private ItemStack createCompass() {
        ItemStack stack = new ItemStack(Items.COMPASS);
        markHunterCompass(stack);
        return stack;
    }

    private void markHunterCompass(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(COMPASS_DATA_KEY, true));
        stack.set(DataComponentTypes.ITEM_NAME, Text.literal(COMPASS_NAME).formatted(Formatting.AQUA));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
    }
}
