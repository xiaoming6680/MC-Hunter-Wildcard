package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.client.key.HunterWildcardKeyBindings;
import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HunterWildcardClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HunterWildcardPackets.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_SYNC_CONFIG, (payload, context) ->
                context.client().execute(() -> HunterWildcardConfigScreen.receiveSync(payload)));
        HunterWildcardKeyBindings.register();
    }
}
