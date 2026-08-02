package dev.vryxon.simplechestlocker;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class LockableContainers {
    private LockableContainers() {}

    public static boolean isLockable(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) return true;
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof Container;
    }

    public static BlockPos[] resolveLockableBlocks(BlockPos pos, Level world) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type == ChestType.SINGLE) {
                return new BlockPos[]{pos};
            }
            List<BlockPos> parts = new ArrayList<>();
            parts.add(pos);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                BlockState ns = world.getBlockState(neighbor);
                if (ns.getBlock() instanceof ChestBlock && ns.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                    parts.add(neighbor);
                }
            }
            return parts.toArray(new BlockPos[0]);
        }
        return new BlockPos[]{pos};
    }

    public static boolean isDoubleChest(BlockPos pos, Level world) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return false;
        return state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
    }
}