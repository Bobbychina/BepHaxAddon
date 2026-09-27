package bep.hax.mixin;
import bep.hax.modules.InvFix;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BundleMouseActions.class)
public class BundleIssue2b2tHotfixMixin {
    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("BepHax.BundleIssue2b2tHotfixMixin");
    @Shadow @Final private Minecraft client;
    @Unique private Integer packetSelectedItemIndex = null;
    @Inject(method = "sendPacket", at = @At("HEAD"))
    public void sendPacketHead(ItemStack item, int slotId, int selectedItemIndex, CallbackInfo info) {
        packetSelectedItemIndex = null;
        InvFix module = Modules.get().get(InvFix.class);
        if(module == null || !module.shouldFixBundles()) return;
        ClientPacketListener networkHandler = client.getConnection();
        if(networkHandler == null || networkHandler.getServerData() == null) return;
        String address = networkHandler.getServerData().ip;
        if(address == null) return;
        if(!address.equalsIgnoreCase("2b2t.org") && !address.toLowerCase().endsWith(".2b2t.org")) return;
        if(!item.has(DataComponents.BUNDLE_CONTENTS)) return;
        if(selectedItemIndex == -1) return;
        BundleContents bundleContents = item.get(DataComponents.BUNDLE_CONTENTS);
        if(bundleContents.isEmpty()) return;
        packetSelectedItemIndex = (bundleContents.size()-1) - selectedItemIndex;
    }
    @ModifyArg(method = "sendPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendPacket(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 0))
    public Packet<?> sendPacketAtSetSelectedItem(Packet<?> packet) {
        if(packet instanceof ServerboundSelectBundleItemPacket itemSelPacket && packetSelectedItemIndex != null) {
            LOGGER.info("Changed selected bundle index " + itemSelPacket.selectedItemIndex() + " to " + packetSelectedItemIndex);
            return new ServerboundSelectBundleItemPacket(itemSelPacket.slotId(), packetSelectedItemIndex);
        } else {
            return packet;
        }
    }
}