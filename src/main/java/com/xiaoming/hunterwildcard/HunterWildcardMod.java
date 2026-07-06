package com.xiaoming.hunterwildcard;

import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.wildcard.rules.SupplyDropRule;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HunterWildcardMod implements ModInitializer {
    public static final String MOD_ID = "hunterwildcard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        HunterWildcardPackets.registerPayloadTypes();
        HunterWildcardPackets.registerServerReceivers();
        ServerTickEvents.END_SERVER_TICK.register(SupplyDropRule::tickTrackedDrops);
        GameManager.getInstance().registerEvents();
        HunterWildcardCommand.register();
        LOGGER.info("Manhunt Wildcard loaded. Server commands registered.");
    }
}
