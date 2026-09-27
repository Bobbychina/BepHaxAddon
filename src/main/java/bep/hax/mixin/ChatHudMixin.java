package bep.hax.mixin;
import net.minecraft.network.chat.Component;
import bep.hax.modules.AntiToS;
import bep.hax.modules.livemessage.LiveMessage;
import bep.hax.modules.livemessage.gui.LivemessageGui;
import bep.hax.modules.livemessage.util.LivemessageUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.regex.Matcher;
@Mixin(ChatComponent.class)
public class ChatHudMixin {
    @Inject(method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"), cancellable = true)
    private void onLivemessageAddMessage(Component message, MessageSignature signature, GuiMessageTag indicator, CallbackInfo ci) {
        if (!LiveMessage.INSTANCE.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String messageText = message.getString();
        for (java.util.regex.Pattern pattern : LivemessageUtil.FROM_PATTERNS) {
            Matcher matcher = pattern.matcher(messageText);
            if (matcher.find()) {
                String username = matcher.group(1);
                String msg = matcher.group(2);
                boolean shouldHide = LivemessageGui.newMessage(username, msg, false);
                if (shouldHide) {
                    ci.cancel();
                }
                return;
            }
        }
        for (java.util.regex.Pattern pattern : LivemessageUtil.TO_PATTERNS) {
            Matcher matcher = pattern.matcher(messageText);
            if (matcher.find()) {
                String username = matcher.group(1);
                String msg = matcher.group(2);
                boolean shouldHide = LivemessageGui.newMessage(username, msg, true);
                if (shouldHide) {
                    ci.cancel();
                }
                return;
            }
        }
    }
    @ModifyVariable(
        method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component censorChatMessage(Component message) {
        Modules modules = Modules.get();
        if (modules == null) return message;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return message;
        MutableComponent mText = Component.literal(antiToS.censorText(message.getString()));
        return (antiToS.containsBlacklistedText(message.getString()) ? mText.setStyle(message.getStyle()) : message);
    }
    // 26.1: addMessage(Component) 拆成 addClientSystemMessage / addServerSystemMessage，两条都挂
    @Inject(method = "addClientSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void maybeCancelAddMessage(Component message, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;
        if (antiToS.chatMode.get() == AntiToS.ChatMode.Remove && antiToS.containsBlacklistedText(message.getString())) ci.cancel();
    }
    @Inject(method = "addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void maybeCancelAddServerSystemMessage(Component message, CallbackInfo ci) {
        maybeCancelAddMessage(message, ci);
    }
}