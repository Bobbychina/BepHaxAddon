package bep.hax.mixin;
import bep.hax.modules.InvFix;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractContainerScreen.class)
public class NoUnstackableDragMixin<T extends AbstractContainerMenu> {
    @Shadow @Final protected T menu; // 26.1: handler -> menu
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void mouseDragged(MouseButtonEvent click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        InvFix module = Modules.get().get(InvFix.class);
        if(module == null || !module.shouldFixUnstackableDrag()) return;
        if(!menu.getCarried().isEmpty() && !menu.getCarried().isStackable()) {
            cir.setReturnValue(true);
        }
    }
}