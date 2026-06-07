package com.xiaoming.hunterwildcard.client.hud;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class WildcardIcons {
    private WildcardIcons() {
    }

    public static ItemStack iconFor(String wildcardName) {
        if (wildcardName == null) {
            return new ItemStack(Items.NETHER_STAR);
        }

        return switch (wildcardName) {
            case "疾速追猎", "SpeedRush" -> new ItemStack(Items.SUGAR);
            case "轻盈之身", "Featherweight" -> new ItemStack(Items.FEATHER);
            case "全员发光", "Glowing" -> new ItemStack(Items.GLOWSTONE_DUST);
            case "暗夜追猎", "NightHunt" -> new ItemStack(Items.CLOCK);
            case "死亡爆炸", "ExplosiveDeath" -> new ItemStack(Items.TNT);
            case "补给空投", "SupplyDrop" -> new ItemStack(Items.CHEST);
            case "猎人雷达", "HunterRadar" -> new ItemStack(Items.SPYGLASS);
            case "指南针干扰", "CompassChaos" -> new ItemStack(Items.COMPASS);
            default -> new ItemStack(Items.NETHER_STAR);
        };
    }
}
