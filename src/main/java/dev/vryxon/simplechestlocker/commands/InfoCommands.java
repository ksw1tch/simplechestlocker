package dev.vryxon.simplechestlocker.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.vryxon.simplechestlocker.LockManager;
import dev.vryxon.simplechestlocker.LockableContainers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.vryxon.simplechestlocker.SimpleChestLocker.ver;
import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsg;
import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsgS;

public final class InfoCommands {
    private static final int PAGE_SIZE = 5;

    private InfoCommands() {}

    public static int handleVersion(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        sendMsgS(player, "\u00A77SimpleChestLocker version: \u00A7r" + ver);
        return 1;
    }

    public static int handleHelp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        sendMsg(player, "§6§lSimpleChestLocker Help:");
        sendMsg(player, "§e/scl help §7- Show this help message");
        sendMsg(player, "§e/scl version §7- Display the mod version");
        sendMsg(player, "§e/scl lock §7- Lock the container you're looking at");
        sendMsg(player, "§e/scl unlock §7- Unlock the container you're looking at");
        sendMsg(player, "§e/scl unlock <label|index> §7- Unlock a labeled or indexed container");
        sendMsg(player, "§e/scl label <label> §7- Label the locked container you're looking at");
        sendMsg(player, "§e/scl label <label> <index> §7- Label container <index> (e.g. /scl label storage 0)");
        sendMsg(player, "§e/scl rename <label> §7- Rename the container you're looking at");
        sendMsg(player, "§e/scl rename <label> <label|index> §7- Rename a specific container's label");
        sendMsg(player, "§e/scl unlabel §7- Remove the label from the container you're looking at");
        sendMsg(player, "§e/scl unlabel <label|index> §7- Remove the label from a specific container");
        sendMsg(player, "§e/scl trust <player> §7- Trust a player with the container you're looking at");
        sendMsg(player, "§e/scl trust <player> <label|index> §7- Trust a player with a labeled or indexed container");
        sendMsg(player, "§e/scl untrust <player> §7- Untrust a player from the container you're looking at");
        sendMsg(player, "§e/scl untrust <player> <label|index> §7- Untrust a player from a labeled or indexed container");
        sendMsg(player, "§e/scl players §7- List players trusted with the container you're looking at");
        sendMsg(player, "§e/scl players <label|index> §7- List players trusted with a labeled or indexed container");
        sendMsg(player, "§e/scl list §7- List all your locked containers (shows their index)");
        sendMsg(player, "§e/scl list <page> §7- List locked containers at a specific page");
        sendMsg(player, "§e/scl gui §7- Open the GUI menu");
        sendMsg(player, "§7Each container gets an index shown in §e/scl list§7 (Container 0, Container 1, ...). "
                + "Use that number in commands to manage it remotely.");
        return 1;
    }

    public static int handleList(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();

        int page = 1;
        try {
            page = IntegerArgumentType.getInteger(ctx, "page");
            if (page < 1) page = 1;
        } catch (IllegalArgumentException ignored) {
            // page argument not provided, default to 1
        }

        List<GlobalPos> locked = new ArrayList<>(LockManager.getSortedLockedContainersForOwner(world, player.getUUID()));

        if (locked.isEmpty()) {
            sendMsg(player, "§eYou don't have any locked containers.");
            return 1;
        }

        int totalPages = (int) Math.ceil(locked.size() / (double) PAGE_SIZE);
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, locked.size());

        sendMsg(player, "§aLocked containers (§e" + locked.size() + "§a total) §7- Page §e" + page + "§7/§e" + totalPages);

        for (int i = start; i < end; i++) {
            GlobalPos globalPos = locked.get(i);
            ServerLevel level = player.level().getServer().getLevel(globalPos.dimension());

            if (level == null) {
                continue;
            }

            BlockPos pos = globalPos.pos();
            int index = LockManager.getContainerIndex(world, player.getUUID(), globalPos);
            String dimension = globalPos.dimension().identifier().toString();
            String type = LockableContainers.getContainerType(level, pos);
            String label = LockManager.getLabel(level, player.getUUID(), pos);

            if (label == null) {
                label = "§7(Unlabeled)";
            } else {
                label = "§e" + label;
            }

            sendMsg(player,
                    "§fContainer §e" + index +
                            " §7- " + label +
                            " §7- §f" + type +
                            " §7(" +
                            pos.getX() + ", " +
                            pos.getY() + ", " +
                            pos.getZ() +
                            ") §8[" + dimension + "]");
        }

        if (totalPages > 1) {
            sendMsg(player, "§7Use §e/scl list <page>§7 to navigate pages.");
        }

        return 1;
    }

    public static int handlePlayers(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();

        BlockPos pos = CommandHelper.getTargetedContainerPos(player);

        if (!CommandHelper.requireTargetedContainer(player, pos)) {
            return 0;
        }
        if (!CommandHelper.requireOwnership(player, world, pos)) {
            return 0;
        }

        return listTrusted(player, world, pos);
    }

    public static int handlePlayersLabel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "label");

        GlobalPos globalPos = CommandHelper.resolveTarget(player, target);

        if (globalPos == null) {
            sendMsg(player, "§cNo container found for §e" + target + "§c!");
            return 0;
        }

        ServerLevel world = player.level().getServer().getLevel(globalPos.dimension());
        if (world == null) {
            sendMsg(player, "§cThat container's dimension isn't loaded!");
            return 0;
        }

        BlockPos pos = globalPos.pos();
        UUID owner = LockManager.getOwner(world, pos);

        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }

        return listTrusted(player, world, pos);
    }

    private static int listTrusted(ServerPlayer player, ServerLevel world, BlockPos pos) {
        UUID owner = LockManager.getOwner(world, pos);
        Set<String> trusted = LockManager.getTrusted(world, owner, pos);

        if (trusted.isEmpty()) {
            sendMsg(player, "§eNo trusted players.");
        } else {
            sendMsg(player, "§aTrusted: §e" + String.join(", ", trusted));
        }
        return 1;
    }
}