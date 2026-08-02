package dev.vryxon.simplechestlocker;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
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

    public static String getContainerType(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        String namespace = blockId != null ? blockId.getNamespace() : "";

        if ("minecraft".equals(namespace)) {
            if (block instanceof ChestBlock) {
                return isDoubleChest(pos, world) ? "Double Chest" : "Chest";
            }
            if (block instanceof BarrelBlock) return "Barrel";
            if (block instanceof ShulkerBoxBlock) return "Shulker Box";
            if (block instanceof FurnaceBlock) return "Furnace";
            if (block instanceof HopperBlock) return "Hopper";
            if (block instanceof DispenserBlock) return "Dispenser";
            if (block instanceof DropperBlock) return "Dropper";
        }

        if (blockId != null) {
            String path = blockId.getPath();
            return capitalizeWords(path.replace('_', ' '));
        }

        return "Container";
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
                capitalizeNext = false;
            }
        }

        return result.toString();
    }
}