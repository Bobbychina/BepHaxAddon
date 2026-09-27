package bep.hax.mixin.accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.MultiLineEditBox;
@Mixin(MultiLineEditBox.class)
public interface EditBoxWidgetAccessor {
    @Accessor
    MultilineTextField getEditBox();
}