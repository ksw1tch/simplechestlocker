package me.toobad.simplechestlocker;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class LockEvents {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;

            BlockPos clickPos = hit.getBlockPos();
            String playerName = player.getName().getString();

            for (BlockPos pos : LockableContainers.resolveLockableBlocks(clickPos, serverWorld)) {
                if (!LockManager.isLocked(serverWorld, pos)) continue;

                UUID owner = LockManager.getOwner(serverWorld, pos);
                if (owner == null) continue;

                if (!owner.equals(player.getUuid()) &&
                        !LockManager.isTrusted(serverWorld, owner, pos, playerName)) {
                    player.sendMessage(Text.literal("§cThis container is locked!"), true);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, breakPos, state, blockEntity) -> {
            if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return true;

            boolean allowed = true;
            UUID playerId = player.getUuid();

            for (BlockPos pos : LockableContainers.resolveLockableBlocks(breakPos, serverWorld)) {
                if (!LockManager.isLocked(serverWorld, pos)) continue;

                UUID owner = LockManager.getOwner(serverWorld, pos);
                if (owner == null) continue;

                if (!owner.equals(playerId)) {
                    player.sendMessage(Text.literal("§cYou cannot break a locked container!"), true);
                    allowed = false;
                } else {
                    LockManager.removeLock(serverWorld, pos);
                }
            }
            return allowed;
        });
    }

    public static boolean isInventoryLocked(ServerWorld world, BlockPos pos) {
        if (!LockManager.isLocked(world, pos)) return false;
        return world.getBlockEntity(pos) instanceof Inventory;
    }
}