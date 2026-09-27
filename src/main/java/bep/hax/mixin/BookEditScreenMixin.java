package bep.hax.mixin;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import bep.hax.util.StardustUtil;
import bep.hax.modules.BookTools;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.Button;
import bep.hax.mixin.accessor.BookEditScreenAccessor;
import bep.hax.mixin.accessor.EditBoxWidgetAccessor;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    private static final Random RANDOM = new Random();
    protected BookEditScreenMixin(Component title) { super(title); }
    @Unique
    private boolean rainbowMode = false;
    @Unique
    private boolean didFormatPage = false;
    @Unique
    private String activeFormatting = "";
    @Unique
    private StardustUtil.RainbowColor lastCC = null;
    @Unique
    private final ArrayList<Button> buttons = new ArrayList<>();
    @Unique
    private void insertText(String text) {
        MultilineTextField editBox = ((EditBoxWidgetAccessor) ((BookEditScreenAccessor) this).getEditBox()).getEditBox();
        if (editBox != null) {
            editBox.insertText(text);
        }
    }
    @Unique
    private void onClickColorButton(Button btn) {
        String color = btn.getMessage().getString().substring(0, 2);
        this.didFormatPage = true;
        insertText(color);
    }
    @Unique
    private void onClickFormatButton(Button btn) {
        String format = btn.getMessage().getString().substring(0, 2);
        if (rainbowMode) {
            activeFormatting = format;
        } else {
            this.didFormatPage = true;
            insertText(format);
        }
    }
    @Unique
    private void onClickRainbowButton(Button btn) {
        rainbowMode = !rainbowMode;
        if (rainbowMode) {
            btn.setMessage(Component.literal(uCC()+"🌈"));
            btn.setTooltip(Tooltip.create(Component.literal(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §2On")));
        } else {
            btn.setMessage(Component.literal("🌈"));
            btn.setTooltip(Tooltip.create(Component.literal(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §4Off")));
        }
    }
    @Unique
    private String uCC() {
        if (lastCC == null) {
            lastCC = StardustUtil.RainbowColor.getFirst();
        } else {
            lastCC = StardustUtil.RainbowColor.getNext(lastCC);
        }
        return lastCC.labels[RANDOM.nextInt(lastCC.labels.length)];
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void mixinInit(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        BookTools bookTools = modules.get(BookTools.class);
        if (bookTools.skipFormatting()) return;
        int offset = 0;
        boolean odd = false;
        for (StardustUtil.TextColor color : StardustUtil.TextColor.values()) {
            if (color.label.isEmpty()) continue;
            this.buttons.add(
                this.addRenderableWidget(
                    Button.builder(
                            Component.literal(color.label+"§l◼"),
                            this::onClickColorButton
                        )
                        .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                        .tooltip(Tooltip.create(Component.literal("§7"+color.name().replace("_", " "))))
                        .build())
            );
            if (odd) offset += 12;
            odd = !odd;
        }
        for (StardustUtil.TextFormat format : StardustUtil.TextFormat.values()) {
            if (format.label.isEmpty()) continue;
            this.buttons.add(
                this.addRenderableWidget(
                    Button.builder(
                            Component.literal(format.label+"A"),
                            this::onClickFormatButton
                        )
                        .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                        .tooltip(Tooltip.create(Component.literal("§7"+format.name())))
                        .build())
            );
            if (odd) offset += 12;
            odd = !odd;
        }
        this.buttons.add(
            this.addRenderableWidget(
                Button.builder(
                        Component.literal("§rA"),
                        this::onClickFormatButton
                    )
                    .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 10, 10)
                    .tooltip(Tooltip.create(Component.literal("§7Reset Formatting")))
                    .build()
            )
        );
        if (odd) offset += 12;
        odd = !odd;
        this.buttons.add(
            this.addRenderableWidget(
                Button.builder(
                        Component.literal("🌈"),
                        this::onClickRainbowButton
                    )
                    .bounds(odd ? this.width / 2 - 100 : this.width / 2 - 112, 47+offset, 22, 10)
                    .tooltip(Tooltip.create(Component.literal(uCC()+"R"+uCC()+"a"+uCC()+"i"+uCC()+"n"+uCC()+"b"+uCC()+"o"+uCC()+"w "+uCC()+"M"+uCC()+"o"+uCC()+"d"+uCC()+"e"+" §4Off")))
                    .build()
            )
        );
    }
    @Inject(method = "finalizeBook", at = @At("HEAD"))
    private void mixinFinalizeBook(CallbackInfo ci) {
        if (this.didFormatPage) {
            insertText("§r");
        }
    }
    @Inject(method = "openPreviousPage", at = @At("HEAD"))
    private void mixinOpenPreviousPage(CallbackInfo ci) {
        this.didFormatPage = false;
    }
    @Inject(method = "openNextPage", at = @At("HEAD"))
    private void mixinOpenNextPage(CallbackInfo ci) {
        this.didFormatPage = false;
    }
    @Inject(method = "updatePage", at = @At("TAIL"))
    private void mixinUpdatePage(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        BookTools bookTools = modules.get(BookTools.class);
        if (bookTools.skipFormatting()) return;
        for (Button btn : this.buttons) {
            btn.visible = true;
        }
    }
}