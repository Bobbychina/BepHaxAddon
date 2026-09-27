package bep.hax.mixin;
import bep.hax.modules.NoHurtCam;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.math.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // 26.1: tiltViewWhenHurt(PoseStack,float) -> bobHurt(CameraRenderState,PoseStack)（受击镜头抖动）
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(CameraRenderState state, PoseStack matrices, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        NoHurtCam noHurtCam = modules.get(NoHurtCam.class);
        if (noHurtCam != null && noHurtCam.shouldDisableHurtCam()) {
            ci.cancel();
        }
    }
}