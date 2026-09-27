package bep.hax.mixin;
import bep.hax.modules.RocketMan;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ElytraOnPlayerSoundInstance.class)
public abstract class ElytraSoundInstanceMixin extends AbstractTickableSoundInstance {
    protected ElytraSoundInstanceMixin(SoundEvent soundEvent, SoundSource soundCategory, RandomSource random) {
        super(soundEvent, soundCategory, random);
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void mixinTick(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        RocketMan rocketMan = modules.get(RocketMan.class);
        if (rocketMan == null) return;   // 26.1: 启动期模块可能未注册
        if (rocketMan.isActive() && rocketMan.shouldMuteElytra()) this.stop();
    }
}