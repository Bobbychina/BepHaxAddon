package bep.hax.mixin;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.DoorBlock;
import bep.hax.modules.AutoDoors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block {
    public DoorBlockMixin(BlockBehaviour.Properties settings) {
        super(settings);
    }
    // 26.1: playOpenCloseSound -> private void playSound(Entity,Level,BlockPos,boolean)
    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void mixinPlayOpenCloseSound(Entity entity, Level level, BlockPos pos, boolean open, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AutoDoors autoDoors = modules.get(AutoDoors.class);
        if (autoDoors == null) return;
        if (autoDoors.shouldMute()) ci.cancel();
    }
}