package bep.hax.mixin.accessor;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ClientLevel.class)
public interface AccessorClientWorld {
    @Invoker("playSound")
    void hookPlaySound(double x, double y, double z, SoundEvent event,
                       SoundSource category, float volume, float pitch,
                       boolean useDistance, long seed);
    @Invoker("getBlockStatePredictionHandler") // 26.1: getPendingUpdateManager -> getBlockStatePredictionHandler
    BlockStatePredictionHandler hookGetPendingUpdateManager();
}