package com.xiaoming.hunterwildcard.ui;

import com.xiaoming.hunterwildcard.game.GameContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MessageManager {
    public void broadcast(MinecraftServer server, String message) {
        server.getPlayerManager().broadcast(Text.literal("[猎人外卡] " + message).formatted(Formatting.GOLD), false);
    }

    public void toParticipants(GameContext context, String message) {
        Text text = Text.literal("[猎人外卡] " + message).formatted(Formatting.GOLD);
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.sendMessage(text);
        }
    }

    public void actionBar(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(message).formatted(Formatting.YELLOW), true);
    }

    public void actionBar(GameContext context, String message) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            actionBar(player, message);
        }
    }

    public void direct(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal("[猎人外卡] " + message).formatted(Formatting.GOLD));
    }
}
