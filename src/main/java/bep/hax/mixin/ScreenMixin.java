package bep.hax.mixin;
import java.util.Arrays;
import net.minecraft.network.chat.*;
import bep.hax.util.LogUtil;
import bep.hax.modules.AntiToS;
import bep.hax.modules.ChatSigns;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.Renderable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.injection.At;
import bep.hax.mixin.accessor.StyleAccessor;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler implements Renderable {
    @Shadow
    @Final
    @Mutable
    protected Component title;
    // 26.1: Screen.render -> extractRenderState(GuiGraphicsExtractor,int,int,float)
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void censorScreenTitles(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Modules mods = Modules.get();
        if (mods == null) return;
        AntiToS tos = mods.get(AntiToS.class);
        if (!tos.isActive() || !tos.containsBlacklistedText(this.title.getString())) return;
        MutableComponent txt = Component.literal(tos.censorText(this.title.getString()));
        this.title = txt.setStyle(this.title.getStyle());
    }
}