package bep.hax.mixin;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import static meteordevelopment.meteorclient.MeteorClient.mc;
@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public abstract boolean hasPose(Pose pose); // 26.1: isInPose -> hasPose
    @Shadow
    public abstract Component getName();
    @Shadow
    public abstract Level level(); // 26.1: getEntityWorld -> level()
    // 26.1: Entity.interact 已改为 interact(Player,InteractionHand,Vec3)，此处未使用，摘除
    @Shadow
    protected abstract void checkFallDamage(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition); // 26.1: fall -> checkFallDamage
    // 26.1: Entity.stepOnBlock 已移除，此处未使用，摘除
    @Shadow
    public abstract float maxUpStep(); // 26.1: getStepHeight -> maxUpStep
    @Shadow
    public abstract boolean onGround(); // 26.1: isOnGround -> onGround
    @Shadow
    public abstract AABB getBoundingBox();
}