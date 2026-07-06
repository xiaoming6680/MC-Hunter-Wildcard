package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.client.hud.WildcardDrawOverlay;
import com.xiaoming.hunterwildcard.client.key.HunterWildcardKeyBindings;
import com.xiaoming.hunterwildcard.client.screen.HunterWildcardConfigScreen;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HunterWildcardClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HunterWildcardPackets.registerPayloadTypes();
        WildcardDrawOverlay.register();
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_SYNC_CONFIG, (payload, context) ->
                context.client().execute(() -> HunterWildcardConfigScreen.receiveSync(payload)));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_OPERATION_RESULT, (payload, context) ->
                context.client().execute(() -> HunterWildcardConfigScreen.receiveOperationResult(payload)));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_CLOSE_CONFIG_SCREEN, (payload, context) ->
                context.client().execute(HunterWildcardConfigScreen::closeFromServer));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_CLEAR_CHAT, (payload, context) ->
                context.client().execute(() -> context.client().inGameHud.getChatHud().clear(false)));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_WILDCARD_DRAW, (payload, context) ->
                context.client().execute(() -> WildcardDrawOverlay.start(payload.wildcardName())));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_WILDCARD_INTRO, (payload, context) ->
                context.client().execute(() -> WildcardDrawOverlay.setIntro(
                        payload.visible(),
                        payload.wildcardName(),
                        payload.description()
                )));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_HUNTER_KILL_FEEDBACK, (payload, context) ->
                context.client().execute(() -> WildcardDrawOverlay.showKillFeedback(
                        payload.hunterName(),
                        payload.runnerName(),
                        payload.remainingKills(),
                        payload.currentKills(),
                        payload.targetKills()
                )));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_HUD_FEEDBACK, (payload, context) ->
                context.client().execute(() -> WildcardDrawOverlay.showFeedback(
                        payload.title(),
                        payload.line1(),
                        payload.line2(),
                        payload.style()
                )));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_OBJECTIVE_STATUS, (payload, context) ->
                context.client().execute(() -> WildcardDrawOverlay.setObjectiveStatus(
                        payload.visible(),
                        payload.text(),
                        payload.style()
                )));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_OBJECTIVE_NOTICE, (payload, context) ->
                context.client().execute(() -> WildcardDrawOverlay.showObjectiveNotice(
                        payload.message(),
                        payload.style()
                )));
        ClientPlayNetworking.registerGlobalReceiver(HunterWildcardPackets.S2C_WEAPON_OVERHEAT_STATUS, (payload, context) ->
                context.client().execute(() -> WildcardDrawOverlay.setWeaponOverheat(
                        payload.heat(),
                        payload.maxHeat(),
                        payload.visible()
                )));
        HunterWildcardKeyBindings.register();
    }
}
