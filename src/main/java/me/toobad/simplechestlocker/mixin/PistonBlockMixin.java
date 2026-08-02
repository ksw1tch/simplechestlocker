package me.toobad.simplechestlocker.mixin;

import me.toobad.simplechestlocker.LockConfig;
import me.toobad.simplechestlocker.LockManager;
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

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void preventPistonInteractionWithLockedBlock(World world, BlockPos pos, Direction dir, boolean extend, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) return;
        if (!LockConfig.get().protectFromPistons) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        BlockPos targetPos = pos.offset(dir);
        if (LockManager.isLocked(serverWorld, targetPos)) {
            cir.setReturnValue(false);
        }
    }
}