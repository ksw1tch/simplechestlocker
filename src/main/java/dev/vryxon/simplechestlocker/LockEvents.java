package dev.vryxon.simplechestlocker;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import java.util.UUID;

public class LockEvents {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverWorld)) return InteractionResult.PASS;

            BlockPos clickPos = hit.getBlockPos();
            String playerName = player.getName().getString();

            for (BlockPos pos : LockableContainers.resolveLockableBlocks(clickPos, serverWorld)) {
                if (!LockManager.isLocked(serverWorld, pos)) continue;

                UUID owner = LockManager.getOwner(serverWorld, pos);
                if (owner == null) continue;

                if (!owner.equals(player.getUUID()) &&
                        !LockManager.isTrusted(serverWorld, owner, pos, playerName)) {
                    player.displayClientMessage(Component.literal("§cThis container is locked!"), true);
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, breakPos, state, blockEntity) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverWorld)) return true;

            boolean allowed = true;
            UUID playerId = player.getUUID();

            for (BlockPos pos : LockableContainers.resolveLockableBlocks(breakPos, serverWorld)) {
                if (!LockManager.isLocked(serverWorld, pos)) continue;

                UUID owner = LockManager.getOwner(serverWorld, pos);
                if (owner == null) continue;

                if (!owner.equals(playerId)) {
                    player.displayClientMessage(Component.literal("§cYou cannot break a locked container!"), true);
                    allowed = false;
                } else {
                    LockManager.removeLock(serverWorld, pos);
                }
            }
            return allowed;
        });
    }

    public static boolean isInventoryLocked(ServerLevel world, BlockPos pos) {
        if (!LockManager.isLocked(world, pos)) return false;
        return world.getBlockEntity(pos) instanceof Container;
    }
}