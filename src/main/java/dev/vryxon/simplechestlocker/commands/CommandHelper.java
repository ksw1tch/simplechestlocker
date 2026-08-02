package dev.vryxon.simplechestlocker.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.vryxon.simplechestlocker.LockManager;
import dev.vryxon.simplechestlocker.LockableContainers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsg;

public final class CommandHelper {
    private CommandHelper() {}

    private static final Pattern INDEX_REF = Pattern.compile("^\\d+$");

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

    public static GlobalPos resolveTarget(ServerPlayer player, String target) {
        ServerLevel world = player.level().getLevel();
        String trimmed = target.trim();
        if (INDEX_REF.matcher(trimmed).matches()) {
            return LockManager.getPosFromIndex(world, player.getUUID(), Integer.parseInt(trimmed));
        }
        return LockManager.getPosFromLabel(world, player.getUUID(), trimmed);
    }

    public static String[] splitTargetSuffix(ServerPlayer player, String input, boolean allowLabelTarget) {
        ServerLevel world = player.level().getLevel();
        String trimmed = input.trim();

        Matcher idx = Pattern.compile("^(.*)\\s+(\\d+)$").matcher(trimmed);
        if (idx.matches() && !idx.group(1).trim().isEmpty()) {
            return new String[]{idx.group(1).trim(), idx.group(2)};
        }

        if (!allowLabelTarget) {
            return null;
        }

        String best = null;
        for (String label : LockManager.getAllLabels(world, player.getUUID())) {
            if (label != null && !label.isEmpty() && trimmed.endsWith(" " + label)) {
                if (best == null || label.length() > best.length()) {
                    best = label;
                }
            }
        }
        if (best != null) {
            String name = trimmed.substring(0, trimmed.length() - best.length() - 1).trim();
            if (!name.isEmpty()) {
                return new String[]{name, best};
            }
        }
        return null;
    }

    public static String targetRef(ServerPlayer player, GlobalPos pos) {
        ServerLevel world = player.level().getLevel();
        UUID owner = player.getUUID();
        String label = LockManager.getLabel(world, owner, pos.pos());
        if (label != null && !label.isEmpty()) {
            return label;
        }
        int index = LockManager.getContainerIndex(world, owner, pos);
        return index >= 0 ? String.valueOf(index) : null;
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

    public static CompletableFuture<Suggestions> suggestTargets(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return builder.buildFuture();
        }

        ServerLevel world = player.level().getLevel();
        UUID owner = player.getUUID();
        int count = LockManager.getSortedLockedContainersForOwner(world, owner).size();

        for (int i = 0; i < count; i++) {
            builder.suggest(String.valueOf(i));
        }

        suggestLabels(ctx, builder);
        return builder.buildFuture();
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