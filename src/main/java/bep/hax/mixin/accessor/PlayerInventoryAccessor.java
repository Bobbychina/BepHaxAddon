package bep.hax.mixin.accessor;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Inventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selectedSlot")
    int getSelectedSlot();
    @Accessor("selectedSlot")
    void setSelectedSlot(int slot);
    @Accessor("main")
    NonNullList<ItemStack> getMain();
}