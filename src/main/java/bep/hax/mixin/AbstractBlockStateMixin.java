package bep.hax.mixin;
import bep.hax.modules.BepMine;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true) // 26.1: calcBlockBreakingDelta -> getDestroyProgress
    private void onCalcBlockBreakingDelta(Player player, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> info) {
        BepMine bepMine = Modules.get().get(BepMine.class);
        if (bepMine != null && bepMine.isActive()) {
            if (bepMine.getModeConfig().get() == BepMine.SpeedmineMode.DAMAGE) {
                float originalDelta = info.getReturnValueF();
                float speedMultiplier = 1.0f / bepMine.getSpeedConfig().get().floatValue();
                info.setReturnValue(originalDelta * speedMultiplier);
            }
        }
    }
}