package com.xiaoming.hunterwildcard;

import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.game.GameManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HunterWildcardMod implements ModInitializer {
    public static final String MOD_ID = "hunterwildcard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GameManager.getInstance().registerEvents();
        HunterWildcardCommand.register();
        LOGGER.info("Hunter Wildcard loaded. Server commands registered.");
    }
}
