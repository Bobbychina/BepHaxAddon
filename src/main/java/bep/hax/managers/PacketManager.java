package bep.hax.managers;
import net.minecraft.world.item.ItemStack;
import bep.hax.config.StardustConfig;
import net.minecraft.world.inventory.AbstractContainerMenu;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ContainerInput;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.MeteorClient;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
public class PacketManager {
    public PacketManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!Utils.canUpdate()) return;
        if (!StardustConfig.ignoreOverlayMessages.get()) return;
        if (!(event.packet instanceof ClientboundSetActionBarTextPacket packet)) return;
        if (StardustConfig.overlayMessageFilter.get().isEmpty()
            || StardustConfig.overlayMessageFilter.get().stream().allMatch(String::isBlank)) return;
        for (String filter : StardustConfig.overlayMessageFilter.get()) {
            if (filter.isBlank()) continue;
            if (packet.text().getString().equalsIgnoreCase(filter)) {
                event.cancel();
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!StardustConfig.antiInventoryPacketKick.get()) return;
        if (!(event.packet instanceof ServerboundContainerClickPacket packet)) return;
    }
}