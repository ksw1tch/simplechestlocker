package me.toobad.simplechestlocker.mixin;

import me.toobad.simplechestlocker.LockManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin {

    @Inject(
            method = "move",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            cancellable = true
    )
    private void preventMovingLockedBlock(World world, BlockPos pos, Direction dir, boolean extend, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) return;

        BlockPos targetPos = pos.offset(dir);
        if (LockManager.isLocked((ServerWorld) world, targetPos)) {
            cir.setReturnValue(false); // Cancel piston movement
        }
    }

    @Inject(
            method = "move",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;hasBlockEntity()Z"),
            cancellable = true
    )
    private void preventDestroyingLockedBlock(World world, BlockPos pos, Direction dir, boolean extend, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) return;

        BlockPos targetPos = pos.offset(dir);
        if (LockManager.isLocked((ServerWorld) world, targetPos)) {
            cir.setReturnValue(false); // Cancel piston destruction
        }
    }
}
