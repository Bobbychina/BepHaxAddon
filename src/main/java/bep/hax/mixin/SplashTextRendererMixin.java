package bep.hax.mixin;
import org.spongepowered.asm.mixin.Mixin;
import bep.hax.config.StardustConfig;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
    @Inject(method = "extractRenderState", at = @At("HEAD"), require = 0) // 26.1: render -> extractRenderState
    private void mixinRender(GuiGraphicsExtractor context, int width, Font textRenderer, float alpha, CallbackInfo ci) {
        this.trackAlpha = (int)(alpha * 255.0f);
    }

    // 26.1: extractRenderState 走 ActiveTextCollector.accept(...)，颜色不再是 int 形参；
    // 改为直接对 splash 文本套色（等价效果，require = 0 兜底）
    @ModifyExpressionValue(
        method = "extractRenderState",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/SplashRenderer;splash:Lnet/minecraft/network/chat/Component;"),
        require = 0
    )
    private Component recolorSplash(Component original) {
        if (!StardustConfig.greenSplashTextSetting.get()) return original;
        return original.copy().withStyle(ChatFormatting.GREEN);
    }
}