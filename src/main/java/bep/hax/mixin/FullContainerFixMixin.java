package bep.hax.mixin;
import bep.hax.modules.InvFix;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MultiPlayerGameMode.class)
public class FullContainerFixMixin {
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void onClickSlot(int syncId, int slotId, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        InvFix module = Modules.get().get(InvFix.class);
        if (module == null || !module.shouldPreventFullContainerClicks()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AbstractContainerMenu handler = mc.player.containerMenu;
        if (handler == null || handler.containerId != syncId) return;
        if (actionType != ContainerInput.QUICK_MOVE) return;
        if (slotId < 0 || slotId >= handler.slots.size()) return;
        Slot sourceSlot = handler.getSlot(slotId);
        ItemStack sourceStack = sourceSlot.getItem();
        if (sourceStack.isEmpty()) return;
        boolean isFromPlayerInventory = slotId >= handler.slots.size() - 36;
        if (isFromPlayerInventory) {
            if (isContainerFull(handler, sourceStack)) {
                ci.cancel();
                return;
            }
        } else {
            if (isPlayerInventoryFull(handler, sourceStack)) {
                ci.cancel();
                return;
            }
        }
    }
    private boolean isContainerFull(AbstractContainerMenu handler, ItemStack itemToMove) {
        int containerSlots = handler.slots.size() - 36;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = handler.getSlot(i);
            if (!slot.mayPlace(itemToMove)) continue;
            ItemStack slotStack = slot.getItem();
            if (slotStack.isEmpty()) return false;
            if (ItemStack.isSameItemSameComponents(slotStack, itemToMove) &&
                slotStack.getCount() < slotStack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
    private boolean isPlayerInventoryFull(AbstractContainerMenu handler, ItemStack itemToMove) {
        int totalSlots = handler.slots.size();
        int playerInvStart = totalSlots - 36;
        for (int i = playerInvStart; i < totalSlots; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack slotStack = slot.getItem();
            if (slotStack.isEmpty()) return false;
            if (ItemStack.isSameItemSameComponents(slotStack, itemToMove) &&
                slotStack.getCount() < slotStack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }
}