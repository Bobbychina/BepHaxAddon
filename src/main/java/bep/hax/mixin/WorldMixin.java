package bep.hax.mixin;
import net.minecraft.world.level.Level;
import bep.hax.modules.AutoSmith;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sounds.SoundSource;
import bep.hax.modules.StashBrander;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Level.class)
public abstract class WorldMixin implements LevelAccessor, AutoCloseable {
}