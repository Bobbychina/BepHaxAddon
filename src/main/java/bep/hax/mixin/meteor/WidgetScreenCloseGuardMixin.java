package bep.hax.mixin.meteor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.1 兼容（崩溃修复）：
 * Meteor 的 {@code WidgetScreen.renderCustom} 会在界面已不是当前屏幕时调用 {@code closeInternal()}
 * → {@code Screen.onClose()} → {@code setScreen(null)}，此时 Fabric screen-api 的
 * {@code ScreenEvents.remove(null)} 直接抛 {@code NullPointerException: Screen cannot be null}，整局崩。
 * 这里只在「自己仍是当前屏幕」时才真正执行 onClose()，其余情况只做 Meteor 自身的 removed() 清理。
 */
@Mixin(value = WidgetScreen.class, remap = false)
public class WidgetScreenCloseGuardMixin {

    @Redirect(method = "closeInternal",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;onClose()V"))
    private void bephax$guardDoubleClose(Screen self) {
        if (Minecraft.getInstance().screen == self) {
            self.onClose();
        }
    }
}
