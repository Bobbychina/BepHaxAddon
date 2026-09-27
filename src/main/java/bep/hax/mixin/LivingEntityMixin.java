package bep.hax.mixin;
import bep.hax.modules.ElytraFlyPlusPlus;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    @Shadow
    private int noJumpDelay; // 26.1: jumpingCooldown -> noJumpDelay
    @Shadow
    public abstract Brain<?> getBrain();
    private Module bephax$noJumpDelayModule; // 重命名避免与 @Shadow 的 noJumpDelay 冲突
    private ElytraFlyPlusPlus efly;
    private Module getNoJumpDelay() {
        if (bephax$noJumpDelayModule == null) {
            Modules modules = Modules.get();          // 启动早期 Meteor 未初始化 -> 返回 null，调用方已判空
            if (modules == null) return null;
            bephax$noJumpDelayModule = modules.get(bep.hax.modules.NoJumpDelay.class);
        }
        return bephax$noJumpDelayModule;
    }
    private ElytraFlyPlusPlus getEfly() {
        if (efly == null) {
            Modules modules = Modules.get();
            if (modules == null) return null;
            efly = modules.get(ElytraFlyPlusPlus.class);
        }
        return efly;
    }
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/world/entity/LivingEntity;aiStep()V" /* 26.1: tickMovement -> aiStep */)
    private void tickMovement(CallbackInfo ci)
    {
        ElytraFlyPlusPlus eflyModule = getEfly();
        Module noJumpDelayModule = getNoJumpDelay();
        if (mc.player != null && mc.player.getBrain().equals(this.getBrain()) && eflyModule != null && eflyModule.enabled() || noJumpDelayModule != null && noJumpDelayModule.isActive())
        {
            this.noJumpDelay = 0;
        }
    }
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z" /* 26.1: isGliding -> isFallFlying */, cancellable = true)
    private void isGliding(CallbackInfoReturnable<Boolean> cir)
    {
        ElytraFlyPlusPlus eflyModule = getEfly();
        if (mc.player != null && mc.player.getBrain().equals(this.getBrain()) && eflyModule != null && eflyModule.enabled() && !eflyModule.isFakeFlyEnabled())
        {
            cir.setReturnValue(true);
        }
    }
}