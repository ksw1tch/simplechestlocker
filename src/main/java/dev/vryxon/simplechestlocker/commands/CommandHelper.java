package dev.vryxon.simplechestlocker.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.vryxon.simplechestlocker.LockManager;
import dev.vryxon.simplechestlocker.LockableContainers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsg;

public final class CommandHelper {
    private CommandHelper() {}

    public static BlockPos getTargetedContainerPos(ServerPlayer player) {
        HitResult hit = player.pick(6.0, player.getYRot(), false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            if (LockableContainers.isLockable(player.level(), pos)) {
                return pos;
            }
        }
        return null;
    }

    public static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                ctx.getSource().getServer().getPlayerList().getPlayerNamesArray(),
                builder
        );
    }

    public static CompletableFuture<Suggestions> suggestLabels(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }

        return SharedSuggestionProvider.suggest(
                LockManager.getAllLabels(player.level().getLevel(), player.getUUID()),
                builder
        );
    }

    public static boolean requireOwnership(ServerPlayer player, ServerLevel world, BlockPos pos) {
        if (!LockManager.isLocked(world, pos)) {
            sendMsg(player, "§cThat container isn't locked!");
            return false;
        }
        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return false;
        }
        return true;
    }

    public static boolean requireTargetedContainer(ServerPlayer player, BlockPos pos) {
        if (pos == null) {
            sendMsg(player, "§cNo container in sight!");
            return false;
        }
        return true;
    }
}