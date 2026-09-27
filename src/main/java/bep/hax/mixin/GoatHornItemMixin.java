package bep.hax.mixin;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Instrument;
import bep.hax.modules.Honker;
import net.minecraft.world.item.InstrumentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InstrumentItem.class)
public class GoatHornItemMixin extends Item {
    public GoatHornItemMixin(Item.Properties settings) {
        super(settings);
    }
    // 26.1: InstrumentItem.playSound -> private static play(Level,Player,Instrument)
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private static void mixinPlaySound(Level level, Player player, Instrument instrument, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        Honker honker = modules.get(Honker.class);
        if (honker == null) return;   // 26.1: 启动期模块可能未注册
        if (honker.shouldMuteHorns()) ci.cancel();
    }
}