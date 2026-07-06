package com.xiaoming.hunterwildcard.ui;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MessageManager {
    public void broadcast(MinecraftServer server, Text message) {
        server.getPlayerManager().broadcast(HunterWildcardText.prefixed(message).formatted(Formatting.GOLD), false);
    }

    public void broadcastSpec(MinecraftServer server, String messageSpec) {
        broadcast(server, HunterWildcardText.fromSpec(messageSpec));
    }

    public void toParticipants(GameContext context, Text message) {
        Text text = HunterWildcardText.prefixed(message).formatted(Formatting.GOLD);
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.sendMessage(text);
        }
    }

    public void toParticipantsSpec(GameContext context, String messageSpec) {
        toParticipants(context, HunterWildcardText.fromSpec(messageSpec));
    }

    public void actionBar(ServerPlayerEntity player, Text message) {
        player.sendMessage(message.copy().formatted(Formatting.YELLOW), true);
    }

    public void actionBar(GameContext context, Text message) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            actionBar(player, message);
        }
    }

    public void direct(ServerPlayerEntity player, Text message) {
        player.sendMessage(HunterWildcardText.prefixed(message).formatted(Formatting.GOLD));
    }

    public void directSpec(ServerPlayerEntity player, String messageSpec) {
        direct(player, HunterWildcardText.fromSpec(messageSpec));
    }
}
