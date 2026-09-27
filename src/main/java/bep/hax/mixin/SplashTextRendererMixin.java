package bep.hax.mixin;
import org.spongepowered.asm.mixin.Mixin;
import bep.hax.config.StardustConfig;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SplashRenderer.class)
public class SplashTextRendererMixin {
    @Unique private int trackAlpha = 0;

    // Note: render method signature may have changed in 1.21.11
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void mixinRender(GuiGraphicsExtractor context, int width, Font textRenderer, float alpha, CallbackInfo ci) {
        this.trackAlpha = (int)(alpha * 255.0f);
    }

    // Note: drawCenteredTextWithShadow signature changed in 1.21.11, using require = 0 to make optional
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;drawCenteredTextWithShadow(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 4, require = 0)
    private int modifyRenderArg(int color) {
        return StardustConfig.greenSplashTextSetting.get() ? 0x54FB54 | this.trackAlpha : color;
    }
}