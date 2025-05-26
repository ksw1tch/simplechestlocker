package me.toobad.simplechestlocker;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.BarrelBlock;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LockCommands {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("scl")

                            .then(CommandManager.literal("lock")
                                    .executes(LockCommands::handleLock))

                            .then(CommandManager.literal("unlock")
                                    .executes(LockCommands::handleUnlock))

                            .then(CommandManager.literal("trust")
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .suggests(LockCommands::suggestOnlinePlayers)
                                            .executes(LockCommands::handleTrust)))

                            .then(CommandManager.literal("untrust")
                                    .then(CommandManager.argument("player", StringArgumentType.word())
                                            .suggests(LockCommands::suggestOnlinePlayers)
                                            .executes(LockCommands::handleUntrust)))

                            .then(CommandManager.literal("list")
                                    .executes(LockCommands::handleList))
            );
        });
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(
                ctx.getSource().getServer().getPlayerManager().getPlayerNames(),
                builder
        );
    }

    private static BlockPos getTargetedContainerPos(ServerPlayerEntity player) {
        HitResult hit = player.raycast(6.0, player.getYaw(), false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                return pos;
            }
        }
        return null;
    }

    private static int handleLock(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        BlockPos pos = getTargetedContainerPos(player);
        ServerWorld world = player.getServerWorld();
        if (pos == null) {
            player.sendMessage(Text.literal("§cNo container in sight!"), false);
            return 0;
        }

        if (LockManager.lockChest(world, player.getUuid(), pos)) {
            player.sendMessage(Text.literal("§aContainer locked!"), false);
            return 1;
        } else {
            player.sendMessage(Text.literal("§cAlready locked!"), false);
            return 0;
        }
    }

    private static int handleUnlock(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        ServerWorld world = player.getServerWorld();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            player.sendMessage(Text.literal("§cNo container in sight!"), false);
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            player.sendMessage(Text.literal("§cThis container isn't locked!"), false);
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou don't own it!"), false);
            return 0;
        }

        LockManager.removeLock(world, pos);
        player.sendMessage(Text.literal("§aContainer unlocked."), false);
        return 1;
    }

    private static int handleTrust(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");
        ServerWorld world = player.getServerWorld();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            player.sendMessage(Text.literal("§cNo container in sight!"), false);
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            player.sendMessage(Text.literal("§cNot locked!"), false);
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou don't own it!"), false);
            return 0;
        }

        LockManager.addTrusted(world, owner, pos, target);
        player.sendMessage(Text.literal("§aTrusted §e" + target), false);
        return 1;
    }

    private static int handleUntrust(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");
        ServerWorld world = player.getServerWorld();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            player.sendMessage(Text.literal("§cNo container in sight!"), false);
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            player.sendMessage(Text.literal("§cNot locked!"), false);
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou don't own it!"), false);
            return 0;
        }

        LockManager.removeTrusted(world, owner, pos, target);
        player.sendMessage(Text.literal("§aUntrusted §e" + target), false);
        return 1;
    }

    private static int handleList(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        ServerWorld world = player.getServerWorld();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            player.sendMessage(Text.literal("§cNo container in sight!"), false);
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            player.sendMessage(Text.literal("§cNot locked!"), false);
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou don't own it!"), false);
            return 0;
        }

        Set<String> trusted = LockManager.getTrusted(world, owner, pos);
        if (trusted.isEmpty()) {
            player.sendMessage(Text.literal("§eNo trusted players."), false);
        } else {
            player.sendMessage(Text.literal("§aTrusted: §e" + String.join(", ", trusted)), false);
        }
        return 1;
    }
}
