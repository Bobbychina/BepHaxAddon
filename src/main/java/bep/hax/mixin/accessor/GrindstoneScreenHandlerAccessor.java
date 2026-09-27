package bep.hax.mixin.accessor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.inventory.GrindstoneMenu;
@Mixin(GrindstoneMenu.class)
public interface GrindstoneScreenHandlerAccessor {
    @Invoker("removeNonCursesFrom") // 26.1: grind -> removeNonCursesFrom
    ItemStack invokeGrind(ItemStack item);
    @Invoker("mergeEnchantsFrom") // 26.1: transferEnchantments -> mergeEnchantsFrom
    void invokeTransferEnchantments(ItemStack target, ItemStack source);
}