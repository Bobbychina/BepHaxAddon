package bep.hax.mixin.accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
@Mixin(AnvilScreen.class)
public interface AnvilScreenAccessor {
    @Accessor("name") // 26.1: 隐式 getter 推导的 nameField 不存在，字段名为 name
    EditBox getNameField();
}