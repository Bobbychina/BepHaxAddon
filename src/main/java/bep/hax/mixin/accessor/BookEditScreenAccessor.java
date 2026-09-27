package bep.hax.mixin.accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
@Mixin(BookEditScreen.class)
public interface BookEditScreenAccessor {
    @Accessor
    MultiLineEditBox getEditBox();
}