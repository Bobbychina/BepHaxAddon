package bep.hax.mixin;
import bep.hax.modules.ElytraFlyPlusPlus;
import bep.hax.util.PushEntityEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class EntityMixin {
    @Mixin(Entity.class)
    public static class EntityHooks {
        @Shadow
        protected UUID uuid;
        private ElytraFlyPlusPlus efly;
        private ElytraFlyPlusPlus getEfly() {
            if (efly == null) {
                Modules modules = Modules.get();      // 启动早期可能为 null（Meteor 未初始化）
                if (modules == null) return null;
                efly = modules.get(ElytraFlyPlusPlus.class);
            }
            return efly;
        }
        @Inject(at = @At("HEAD"), method = "Lnet/minecraft/world/entity/Entity;getPose()Lnet/minecraft/world/entity/Pose;", cancellable = true)
        private void getPose(CallbackInfoReturnable<Pose> cir) {
            ElytraFlyPlusPlus eflyModule = getEfly();
            if (eflyModule != null && eflyModule.enabled() && this.uuid == mc.player.getUUID()) {
                cir.setReturnValue(Pose.STANDING);
            }
        }
        @Inject(at = @At("HEAD"), method = "Lnet/minecraft/world/entity/Entity;isSprinting()Z", cancellable = true)
        private void isSprinting(CallbackInfoReturnable<Boolean> cir) {
            ElytraFlyPlusPlus eflyModule = getEfly();
            if (eflyModule != null && eflyModule.enabled() && this.uuid == mc.player.getUUID()) {
                cir.setReturnValue(true);
            }
        }
        @Inject(at = @At("HEAD"), method = "push(Lnet/minecraft/world/entity/Entity;)V", cancellable = true) // 26.1: pushAwayFrom -> push(Entity)
        private void pushAwayFrom(Entity entity, CallbackInfo ci) {
            PushEntityEvent pushEntityEvent = new PushEntityEvent((Entity) (Object) this, entity);
            MeteorClient.EVENT_BUS.post(pushEntityEvent);
            if (pushEntityEvent.isCanceled()) {
                ci.cancel();
            }
        }
    }
    @Mixin(EntityRenderer.class)
    public static abstract class EntityRendererHooks {
        @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
        private void shouldRender(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
            if (!(entity instanceof Player player)) return;
            Modules mods = Modules.get();
            if (mods == null) return;
            NoRender noRender = mods.get(NoRender.class);
            if (!noRender.isActive()) return;
            var codySetting = noRender.settings.get("cody");
            if (codySetting != null && (boolean) codySetting.get() && player.getGameProfile().name().equals("codysmile11")) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
}