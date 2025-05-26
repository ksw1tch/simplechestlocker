package me.toobad.simplechestlocker;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.ChestType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.*;

public class LockEvents {

    public static void register() {

        // Prevent player interaction with locked containers
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return ActionResult.PASS;

            BlockPos clickPos = hit.getBlockPos();
            String playerName = player.getName().getString();

            for (BlockPos pos : resolveLockableBlocks(clickPos, serverWorld)) {
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

        // Prevent breaking locked containers
        PlayerBlockBreakEvents.BEFORE.register((world, player, breakPos, state, blockEntity) -> {
            if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return true;

            boolean allowed = true;
            UUID playerId = player.getUuid();

            for (BlockPos pos : resolveLockableBlocks(breakPos, serverWorld)) {
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

    /**
     * Resolve positions for a container: barrels (single) and chests (single or both halves of double chest).
     */
    public static BlockPos[] resolveLockableBlocks(BlockPos pos, ServerWorld world) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BarrelBlock) {
            return new BlockPos[]{pos};
        }
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type == ChestType.SINGLE) {
                return new BlockPos[]{pos};
            }
            // double chest: scan horizontal neighbors
            List<BlockPos> parts = new ArrayList<>();
            parts.add(pos);
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = pos.offset(dir);
                BlockState ns = world.getBlockState(neighbor);
                if (ns.getBlock() instanceof ChestBlock && ns.get(ns.getBlock().getStateManager().getProperty("type")) != ChestType.SINGLE) {
                    parts.add(neighbor);
                }
            }
            return parts.toArray(new BlockPos[0]);
        }
        return new BlockPos[]{pos};
    }
}
