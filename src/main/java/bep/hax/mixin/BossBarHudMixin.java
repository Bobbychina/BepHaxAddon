package bep.hax.mixin;
import net.minecraft.network.chat.Component;
import bep.hax.modules.AntiToS;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BossHealthOverlay.class)
public class BossBarHudMixin {
    @Inject(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD")) // 26.1: renderBossBar -> extractBar
    private void censorBossBar(GuiGraphicsExtractor context, int x, int y, BossEvent bossBar, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (antiToS == null) return;   // 26.1: 启动期模块可能未注册
        if (!antiToS.isActive()) return;
        if (antiToS.containsBlacklistedText(bossBar.getName().getString())) {
            bossBar.setName(Component.literal(antiToS.censorText(bossBar.getName().getString()).formatted(bossBar.getName().getStyle())));
        }
    }
}