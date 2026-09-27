package bep.hax.mixin;
import bep.hax.modules.BepMine;
import bep.hax.modules.RapidFire;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow private float destroyProgress; // 26.1: currentBreakingProgress -> destroyProgress
    // 26.1: stopUsingItem -> releaseUsingItem(Player)
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void preventCrossbowUseReset(Player player, CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        RapidFire rf = mods.get(RapidFire.class);
        if (rf == null) return;   // 26.1: 启动期模块可能未注册
        if (!rf.isActive() || !rf.charging) return;
        ci.cancel();
    }
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true) // 26.1: updateBlockBreakingProgress -> continueDestroyBlock
    private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BepMine bepMine = Modules.get().get(BepMine.class);
        if (bepMine != null && bepMine.isActive() && bepMine.getModeConfig().get() == BepMine.SpeedmineMode.DAMAGE) {
            if (this.destroyProgress >= bepMine.getSpeedConfig().get().floatValue()) {
                this.destroyProgress = 1.0f;
                cir.setReturnValue(true);
            }
        }
    }
}