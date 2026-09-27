package bep.hax.mixin;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Notifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(ChatComponent.class)
public class ChatMentionMixin {
    @Shadow
    @Final
    private Minecraft client;
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Component modifyMessage(Component message) {
        if (client.player == null) return message;
        Notifier notifier = Modules.get().get(Notifier.class);
        if (notifier == null || !notifier.isActive()) return message;
        var highlightSetting = notifier.settings.get("highlight-mentions");
        if (highlightSetting == null || !(boolean) highlightSetting.get()) return message;
        String chatMessage = message.getString();
        String playerName = client.player.getName().getString();
        if (chatMessage.toLowerCase().contains(playerName.toLowerCase())) {
            MutableComponent highlightedMessage = message.copy();
            highlightedMessage.setStyle(message.getStyle().withBold(true));
            var soundSetting = notifier.settings.get("mention-sound");
            if (soundSetting != null && (boolean) soundSetting.get()) {
                var volumeSetting = notifier.settings.get("mention-volume");
                float volume = volumeSetting != null ? ((Double) volumeSetting.get()).floatValue() : 1.0f;
                client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), volume, 1.0f);
            }
            return highlightedMessage;
        }
        return message;
    }
}