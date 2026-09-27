package bep.hax.mixin.accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.Options;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Options.class)
public interface GameOptionsAccessor {
    @Mutable
    @Accessor("renderDistance") // 26.1: viewDistance -> renderDistance
    void setViewDistance(OptionInstance<Integer> viewDistance);
}