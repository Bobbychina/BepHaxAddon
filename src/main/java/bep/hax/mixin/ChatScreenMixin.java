package bep.hax.mixin;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.Modules;
import bep.hax.modules.WebChat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected EditBox chatField;
    protected ChatScreenMixin(Component title) {
        super(title);
    }
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules != null) {
            WebChat webChat = modules.get(WebChat.class);
            if (webChat != null && webChat.isActive() && webChat.shouldHideInGameChat()) {
                if (this.chatField != null) {
                    context.fill(2, this.height - 14 - 2, this.width - 2, this.height - 2, 0x80000000);
                    this.chatField.render(context, mouseX, mouseY, delta);
                }
                ci.cancel();
            }
        }
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules != null) {
            WebChat webChat = modules.get(WebChat.class);
            if (webChat != null && webChat.isActive() && webChat.shouldHideInGameChat()) {
                if (this.chatField != null) {
                    this.chatField.setY(this.height - 12);
                }
            }
        }
    }
}