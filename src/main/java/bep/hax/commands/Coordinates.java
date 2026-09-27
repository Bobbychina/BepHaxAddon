package bep.hax.commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.item.Items;
public class Coordinates extends Command {
    public Coordinates() {
        super("coordinates", "Copies your coordinates to the clipboard.", "coords");
    }
    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            mc.keyboardHandler.setClipboard("%d, %d, %d".formatted(mc.player.blockPosition().getX(), mc.player.blockPosition().getY(), mc.player.blockPosition().getZ()));
            mc.getToastManager().addToast(new MeteorToast.Builder("Coordinates")
                .text("Copied to clipboard.")
                .icon(Items.NETHERITE_PICKAXE)
                .duration(5000)
                .build());
            return SINGLE_SUCCESS;
        });
    }
}