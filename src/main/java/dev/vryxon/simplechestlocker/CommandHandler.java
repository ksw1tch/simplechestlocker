package dev.vryxon.simplechestlocker;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.vryxon.simplechestlocker.commands.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public class CommandHandler {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("scl")
                            .then(Commands.literal("version")
                                    .executes(InfoCommands::handleVersion))

                            .then(Commands.literal("help")
                                    .executes(InfoCommands::handleHelp))

                            .then(Commands.literal("lock")
                                    .executes(LockCommands::handleLock))

                            .then(Commands.literal("unlock")
                                    .executes(LockCommands::handleUnlock))

                            .then(Commands.literal("label")
                                    .then(Commands.argument("label", StringArgumentType.greedyString())
                                            .executes(LabelCommands::handleLabel)))

                            .then(Commands.literal("rename")
                                    .then(Commands.argument("label", StringArgumentType.greedyString())
                                            .executes(LabelCommands::handleRename)))

                            .then(Commands.literal("unlabel")
                                    .executes(LabelCommands::handleUnlabel))

                            .then(Commands.literal("trust")
                                    .then(Commands.argument("player", StringArgumentType.word())
                                            .suggests(CommandHelper::suggestOnlinePlayers)
                                            .executes(TrustCommands::handleTrust)
                                            .then(Commands.argument("label", StringArgumentType.greedyString())
                                                    .suggests(CommandHelper::suggestLabels)
                                                    .executes(TrustCommands::handleTrustLabel))))

                            .then(Commands.literal("untrust")
                                    .then(Commands.argument("player", StringArgumentType.word())
                                            .suggests(CommandHelper::suggestOnlinePlayers)
                                            .executes(TrustCommands::handleUntrust)
                                            .then(Commands.argument("label", StringArgumentType.greedyString())
                                                    .suggests(CommandHelper::suggestLabels)
                                                    .executes(TrustCommands::handleUntrustLabel))))

                            .then(Commands.literal("players")
                                    .executes(InfoCommands::handlePlayers)
                                    .then(Commands.argument("label", StringArgumentType.greedyString())
                                            .suggests(CommandHelper::suggestLabels)
                                            .executes(InfoCommands::handlePlayersLabel)))

                            .then(Commands.literal("list")
                                    .executes(InfoCommands::handleList)
                                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                            .executes(InfoCommands::handleList)))
            );
        });
    }
}