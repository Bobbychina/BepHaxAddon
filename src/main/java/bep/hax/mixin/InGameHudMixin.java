package bep.hax.mixin;
import net.minecraft.network.chat.Component;
import bep.hax.modules.AntiToS;
import bep.hax.modules.NoHurtCam;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import bep.hax.modules.ShulkerOverviewModule;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.HumanoidArm;
@Mixin(Gui.class)
public class InGameHudMixin {
    @Shadow
    private ItemStack currentStack;
    @Inject(
        method = "renderHeldItemTooltip",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;contains(Lnet/minecraft/core/component/DataComponentType;)Z")
    )
    private void censorItemTooltip(GuiGraphicsExtractor context, CallbackInfo ci, @Local LocalRef<MutableComponent> itemName) {
        if (this.currentStack.isEmpty()) return;
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;
        if (antiToS.containsBlacklistedText(itemName.get().getString())) {
            itemName.set(Component.empty().append(antiToS.censorText(itemName.get().getString())).withStyle(this.currentStack.getRarity().color()));
        }
    }
    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void onRenderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        ShulkerOverviewModule module = Modules.get().get(ShulkerOverviewModule.class);
        if (module == null || !module.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();
        int center = scaledWidth / 2;
        int hotbarY = scaledHeight - 19;
        for (int i = 0; i < 9; i++) {
            int posX = center - 90 + i * 20 + 2;
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) continue;
            module.renderShulkerOverlay(context, posX, hotbarY, stack);
        }
        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.isEmpty() && offhandStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            int offY = scaledHeight - 23;
            int offX;
            if (player.getMainArm() == HumanoidArm.LEFT) {
                offX = center + 91 + 9;
            } else {
                offX = center - 91 - 29;
            }
            module.renderShulkerOverlay(context, offX + 3, offY + 3, offhandStack);
        }
    }
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderOverlay(GuiGraphicsExtractor context, net.minecraft.resources.Identifier texture, float opacity, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        NoHurtCam noHurtCam = modules.get(NoHurtCam.class);
        if (noHurtCam != null && noHurtCam.shouldDisableRedOverlay()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.hurtTime > 0) {
                ci.cancel();
            }
        }
    }
}