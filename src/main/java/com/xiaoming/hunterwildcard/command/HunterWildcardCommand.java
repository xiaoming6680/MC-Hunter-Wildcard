package com.xiaoming.hunterwildcard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class HunterWildcardCommand {
    private static final Permission OP_LEVEL_TWO = new Permission.Level(PermissionLevel.GAMEMASTERS);

    private HunterWildcardCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("hw")
                .then(CommandManager.literal("start")
                        .requires(HunterWildcardCommand::canManageGame)
                        .executes(context -> {
                            GameManager.getInstance().start(context.getSource());
                            return 1;
                        }))
                .then(CommandManager.literal("stop")
                        .requires(HunterWildcardCommand::canManageGame)
                        .executes(context -> {
                            GameManager.getInstance().stop(context.getSource());
                            return 1;
                        }))
                .then(CommandManager.literal("join")
                        .then(CommandManager.literal("hunter")
                                .executes(context -> join(context.getSource(), PlayerRole.HUNTER)))
                        .then(CommandManager.literal("runner")
                                .executes(context -> join(context.getSource(), PlayerRole.RUNNER))))
                .then(CommandManager.literal("leave")
                        .executes(context -> {
                            GameManager.getInstance().leave(context.getSource().getPlayerOrThrow());
                            return 1;
                        }))
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            context.getSource().sendFeedback(() -> Text.literal(GameManager.getInstance().getStatusText()), false);
                            return 1;
                        })));
    }

    private static int join(ServerCommandSource source, PlayerRole role) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        GameManager.getInstance().join(player, role);
        return 1;
    }

    private static boolean canManageGame(ServerCommandSource source) {
        return source.getPermissions().hasPermission(OP_LEVEL_TWO);
    }
}
