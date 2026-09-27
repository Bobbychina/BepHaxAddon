package bep.hax.mixin.accessor;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AnvilMenu.class)
public interface AnvilScreenHandlerAccessor {
    @Accessor("cost") // 26.1: 隐式 getter 推导的 levelCost 不存在，字段名为 cost
    DataSlot getLevelCost();
}