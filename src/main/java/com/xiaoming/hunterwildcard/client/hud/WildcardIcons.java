package com.xiaoming.hunterwildcard.client.hud;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class WildcardIcons {
    private WildcardIcons() {
    }

    public static ItemStack iconFor(String wildcardId) {
        if (wildcardId == null) {
            return new ItemStack(Items.NETHER_STAR);
        }

        return switch (wildcardId) {
            case "speed_rush", "SpeedRush" -> new ItemStack(Items.SUGAR);
            case "featherweight", "Featherweight" -> new ItemStack(Items.FEATHER);
            case "glowing", "Glowing" -> new ItemStack(Items.GLOWSTONE_DUST);
            case "night_hunt", "NightHunt" -> new ItemStack(Items.CLOCK);
            case "explosive_death", "ExplosiveDeath" -> new ItemStack(Items.TNT);
            case "supply_drop", "SupplyDrop" -> new ItemStack(Items.CHEST);
            case "hunter_radar", "HunterRadar" -> new ItemStack(Items.SPYGLASS);
            case "compass_chaos", "CompassChaos" -> new ItemStack(Items.COMPASS);
            case "hunger_chase", "HungerChase" -> new ItemStack(Items.COOKED_BEEF);
            case "weapon_overheat", "WeaponOverheat" -> new ItemStack(Items.IRON_SWORD);
            case "light_load", "LightLoad" -> new ItemStack(Items.LEATHER_BOOTS);
            case "block_decay", "BlockDecay" -> new ItemStack(Items.CRACKED_STONE_BRICKS);
            case "pearl_frenzy", "PearlFrenzy" -> new ItemStack(Items.ENDER_PEARL);
            case "wind_charge_brawl", "WindChargeBrawl" -> new ItemStack(Items.WIND_CHARGE);
            case "blood_rage", "BloodRage" -> new ItemStack(Items.REDSTONE);
            case "disabled_wildcard", "DisabledWildcard", "NoEffect" -> new ItemStack(Items.BARRIER);
            default -> new ItemStack(Items.NETHER_STAR);
        };
    }
}
