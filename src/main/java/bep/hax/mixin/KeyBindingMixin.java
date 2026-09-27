package bep.hax.mixin;
import bep.hax.accessor.InputAccessor;
import bep.hax.modules.ElytraFlyPlusPlus;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static meteordevelopment.meteorclient.MeteorClient.mc;
@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin {
    @Final
    @Shadow
    private String name; // 26.1: id -> name
    @Unique
    ElytraFlyPlusPlus efly = null;
    @Inject(at = @At("RETURN"), method = "isDown", cancellable = true) // 26.1: isPressed -> isDown
    public void isPressed(CallbackInfoReturnable<Boolean> cir)
    {
        // 26.1: KeyMapping.isDown 在 Minecraft 构造期（KeyMapping.releaseAll）就会被调用，
        // 那时 Meteor 的 Modules 尚未初始化 -> Modules.get() 为 null，必须先判空
        if (efly == null) {
            Modules modules = Modules.get();
            if (modules == null) return;
            efly = modules.get(ElytraFlyPlusPlus.class);
            if (efly == null) return;
        }
        if (efly.isActive() && efly.enabled() && name.equals("key.forward"))
        {
            cir.setReturnValue(true);
        }
    }
}