package me.toobad.simplechestlocker.mixin;

import me.toobad.simplechestlocker.LockManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.explosion.ExplosionImpl;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Iterator;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin {

    @Shadow public abstract ServerWorld getWorld();
    @Inject(method = "getBlocksToDestroy", at = @At("RETURN"), cancellable = true)
    private void onGetBlocksToDestroy(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> affectedBlocks = cir.getReturnValue();
        if (affectedBlocks == null || affectedBlocks.isEmpty()) return;

        // Assuming you have access to the world instance; adjust as necessary
        World world = getWorld(); // Obtain the world instance appropriately
        if (!(world instanceof ServerWorld serverWorld)) return;

        Iterator<BlockPos> iterator = affectedBlocks.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (LockManager.isLocked(serverWorld, pos)) {
                iterator.remove();
            }
        }

        // Set the modified list back
        cir.setReturnValue(affectedBlocks);
    }

}
