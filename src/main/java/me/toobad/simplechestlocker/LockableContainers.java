package me.toobad.simplechestlocker;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public final class LockableContainers {
    private LockableContainers() {}

    public static boolean isLockable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) return true;
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof Inventory;
    }

    public static BlockPos[] resolveLockableBlocks(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.get(ChestBlock.CHEST_TYPE);
            if (type == ChestType.SINGLE) {
                return new BlockPos[]{pos};
            }
            List<BlockPos> parts = new ArrayList<>();
            parts.add(pos);
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos neighbor = pos.offset(dir);
                BlockState ns = world.getBlockState(neighbor);
                if (ns.getBlock() instanceof ChestBlock && ns.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                    parts.add(neighbor);
                }
            }
            return parts.toArray(new BlockPos[0]);
        }
        return new BlockPos[]{pos};
    }

    public static boolean isDoubleChest(BlockPos pos, World world) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return false;
        return state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE;
    }
}