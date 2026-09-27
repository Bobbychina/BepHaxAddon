package bep.hax.mixin.accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.MultiLineEditBox;
@Mixin(MultiLineEditBox.class)
public interface EditBoxWidgetAccessor {
    @Accessor("textField") // 26.1: 隐式 getter 推导的 editBox 不存在，字段名为 textField
    MultilineTextField getEditBox();
}