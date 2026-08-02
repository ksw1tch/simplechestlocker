package dev.vryxon.simplechestlocker.mixin;

import dev.vryxon.simplechestlocker.LockConfig;
import dev.vryxon.simplechestlocker.LockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z", at = @At("HEAD"), cancellable = true)
    private static void preventExtractionFromLockedContainer(Level world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (LockConfig.get().protectFromHoppers && world instanceof ServerLevel serverWorld) {
            BlockPos sourcePos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ()).relative(Direction.UP);
            if (LockEvents.isInventoryLocked(serverWorld, sourcePos)) {
                cir.setReturnValue(false);
            }
        }
    }
}