package me.toobad.simplechestlocker.mixin;

import me.toobad.simplechestlocker.LockConfig;
import me.toobad.simplechestlocker.LockEvents;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z", at = @At("HEAD"), cancellable = true)
    private static void preventExtractionFromLockedContainer(World world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (LockConfig.get().protectFromHoppers && world instanceof ServerWorld serverWorld) {
            BlockPos sourcePos = BlockPos.ofFloored(hopper.getHopperX(), hopper.getHopperY(), hopper.getHopperZ()).offset(Direction.UP);
            if (LockEvents.isInventoryLocked(serverWorld, sourcePos)) {
                cir.setReturnValue(false);
            }
        }
    }
}