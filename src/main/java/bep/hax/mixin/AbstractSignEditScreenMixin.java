package bep.hax.mixin;
import java.util.List;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import bep.hax.modules.SignHistorian;
import bep.hax.modules.SignatureSign;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.client.gui.font.TextFieldHelper;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import bep.hax.mixin.accessor.AbstractSignEditScreenAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin extends Screen {
    @Shadow
    private int line;                 // 26.1: currentRow -> line
    @Shadow
    @Final
    protected SignBlockEntity sign;   // 26.1: blockEntity -> sign
    @Shadow
    private TextFieldHelper signField; // 26.1: selectionManager -> signField
    // 26.1: setCurrentRowMessage -> setMessage(String)（目标为 private，shadow 需自带方法体）
    @Shadow
    private void setMessage(String message) { throw new AssertionError(); }
    protected AbstractSignEditScreenMixin(Component title) { super(title); }
    @Inject(method = "init", at = @At("TAIL"))
    public void stardustMixinInit(CallbackInfo ci) {
        if (this.minecraft == null) return;
        Modules modules = Modules.get();
        if (modules == null) return;
        SignHistorian signHistorian = modules.get(SignHistorian.class);
        SignatureSign signatureSign = modules.get(SignatureSign.class);
        if (!signatureSign.isActive() && !signHistorian.isActive()) return;
        if (signatureSign.getAutoConfirm()) return;
        SignText restoration = signHistorian.getRestoration(this.sign);
        if ((!signHistorian.isActive() || restoration == null) && signatureSign.isActive()) {
            SignText signature = signatureSign.getSignature(this.sign);
            List<String> msgs = Arrays.stream(signature.getMessages(false)).map(Component::getString).toList();
            String[] messages = new String[msgs.size()];
            messages = msgs.toArray(messages);
            ((AbstractSignEditScreenAccessor) this).setText(signature);
            ((AbstractSignEditScreenAccessor) this).setMessages(messages);
            if ((signatureSign.isActive() && signatureSign.signFreedom.get())) {
                AbstractSignEditScreenAccessor accessor = ((AbstractSignEditScreenAccessor) this);
                this.signField = new TextFieldHelper(
                    () -> accessor.getMessages()[this.line], this::setMessage,
                    TextFieldHelper.createClipboardGetter(this.minecraft), TextFieldHelper.createClipboardSetter(this.minecraft),
                    string -> true
                );
            }
            if (signatureSign.needsDisabling()) {
                signatureSign.disable();
            }
        }
    }
}