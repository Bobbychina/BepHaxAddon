package bep.hax.mixin;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.Identifier;
import bep.hax.modules.MusicTweaks;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.client.sounds.WeighedSoundEvents;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WeighedSoundEvents.class)
public abstract class WeightedSoundSetMixin implements Weighted<Sound> {
    private static final Random RANDOM = new Random();
    @Shadow
    @Final
    private List<Weighted<Sound>> list; // 26.1: sounds -> list
    @Inject(method = "getSound(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/client/resources/sounds/Sound;", at = @At("HEAD"), cancellable = true)
    private void mixinGetSound(net.minecraft.util.RandomSource random, CallbackInfoReturnable<Sound> cir) {
        Modules modules = Modules.get();
        if (modules == null) return;
        MusicTweaks tweaks = modules.get(MusicTweaks.class);
        if (tweaks == null || !tweaks.isActive()) return;
        boolean overwrite = false;
        for (Weighted<Sound> sound : this.list) {
            String id = sound.getSound(random).toString();
            if (id.contains("minecraft:music/")) {
                overwrite = true;
                break;
            }
        }
        if (!overwrite) return;
        List<String> soundIDs = tweaks.getSoundSet();
        if (soundIDs.isEmpty()) return;
        float adjustedPitch;
        if (tweaks.randomPitch()) {
            adjustedPitch = 1.0f + tweaks.getRandomPitch();
        } else {
            adjustedPitch = 1.0f + tweaks.getPitchAdjustment();
        }
        float adjustedVolume = tweaks.getClient().options.getFinalSoundSourceVolume(SoundSource.MUSIC) + tweaks.getVolumeAdjustment();
        cir.setReturnValue(
            new Sound(
                Identifier.parse(soundIDs.get(RANDOM.nextInt(soundIDs.size()))),
                ConstantFloat.of(adjustedVolume),
                ConstantFloat.of(adjustedPitch),
                this.getWeight(), Sound.Type.SOUND_EVENT,
                true, true, 16
            )
        );
    }
}