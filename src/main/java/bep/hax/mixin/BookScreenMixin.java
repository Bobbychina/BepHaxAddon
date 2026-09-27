package bep.hax.mixin;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import bep.hax.modules.AntiToS;
import bep.hax.util.StardustUtil;
import bep.hax.modules.BookTools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mutable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import bep.hax.mixin.accessor.BookScreenContentsAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BookViewScreen.class)
public abstract class BookScreenMixin extends Screen {
    @Shadow private int currentPage;      // 26.1: pageIndex -> currentPage
    @Shadow private int cachedPage;       // 26.1: cachedPageIndex -> cachedPage
    @Shadow
    private BookViewScreen.BookAccess bookAccess; // 26.1: contents -> bookAccess
    protected BookScreenMixin(Component title) { super(title); }
    @Unique
    private boolean deobfuscated = false;
    @Unique
    private Button deobfuscateButton;
    @Unique
    private List<Component> obfuscatedPages = new ArrayList<>();
    @Unique
    private void deobfuscateBook(Button btn) {
        if (this.deobfuscated) {
            reobfuscateBook(btn);
            return;
        }
        if (this.bookAccess instanceof BookViewScreen.BookAccess) {
            List<Component> pages = ((BookScreenContentsAccessor)(Object) bookAccess).getPages();
            List<Component> deobfuscatedPages = new java.util.ArrayList<>(List.of());
            for (Component page : pages) {
                deobfuscatedPages.add(Component.literal(page.getString().replace("§k", "")));
            }
            ((BookScreenContentsAccessor)(Object) bookAccess).setPages(deobfuscatedPages);
            btn.setAlpha(0.5f);
            btn.setTooltip(Tooltip.create(Component.literal("§8Restore this tome's secrets..")));
            this.cachedPage = -1;
            btn.setMessage(
                Component.literal("§0<"+StardustUtil.rCC()+"§o✨§r§0> "+StardustUtil.rCC()+"§o§kReobfuscate "+"§0<"
                    +StardustUtil.rCC()+"§o✨§r§0> ")
            );
            this.deobfuscated = true;
        }
    }
    @Unique
    private void reobfuscateBook(Button btn) {
        if (this.bookAccess instanceof BookViewScreen.BookAccess) {
            btn.setAlpha(1f);
            btn.setTooltip(Tooltip.create(Component.literal("§8Reveal this tome's secrets..")));
            btn.setMessage(Component.literal("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "));
            ((BookScreenContentsAccessor)(Object) bookAccess).setPages(this.obfuscatedPages);
            if (!this.obfuscatedPages.get(this.cachedPage).getString().contains("§k")) {
                btn.visible = false;
            }
            this.cachedPage = -1;
            this.deobfuscated = false;
        }
    }
    @Inject(method = "init", at = @At("HEAD"))
    private void mixinInit(CallbackInfo ci) {
        if (!(this.bookAccess instanceof BookViewScreen.BookAccess)) return;
        Modules modules = Modules.get();
        if (modules == null) return;
        List<Component> pages = ((BookScreenContentsAccessor)(Object) this.bookAccess).getPages();
        AntiToS antiToS = modules.get(AntiToS.class);
        if (antiToS == null) return;   // 26.1: 启动期模块可能未注册
        BookTools bookTools = modules.get(BookTools.class);
        if (bookTools == null) return;   // 26.1: 启动期模块可能未注册
        if (antiToS.isActive()) {
            List<Component> filtered = new ArrayList<>();
            for (Component page : pages) {
                if (antiToS.containsBlacklistedText(page.getString())) {
                    filtered.add(Component.literal(antiToS.censorText(page.getString())));
                } else filtered.add(page);
            }
            ((BookScreenContentsAccessor)(Object) this.bookAccess).setPages(filtered);
            this.cachedPage = -1;
        } else if (bookTools.skipDeobfuscation()) return;
        this.deobfuscateButton = this.addRenderableWidget(
            Button.builder(
                    Component.literal("§0<§b§o✨§r§0> "+StardustUtil.rCC()+"§oDeobfuscate "+"§0<§a§o✨§r§0> "),
                    this::deobfuscateBook)
                .bounds(this.width / 2 - 59, 217, 120, 20)
                .tooltip(Tooltip.create(Component.literal("§8Reveal this tome's secrets..")))
                .build());
        if (!pages.isEmpty()) {
            this.deobfuscateButton.visible = pages.get(this.currentPage).getString().contains("§k");
        } else {
            this.deobfuscateButton.visible = false;
        }
        if (pages.stream().anyMatch(page -> page.getString().contains("§k"))) {
            this.obfuscatedPages = ((BookScreenContentsAccessor)(Object) this.bookAccess).getPages();
        }
    }
    @Inject(method = "updateButtonVisibility", at = @At("TAIL")) // 26.1: updatePageButtons -> updateButtonVisibility
    private void mixinUpdatePageButtons(CallbackInfo ci) {
        if (this.deobfuscated) return;
        if (!(this.bookAccess instanceof BookViewScreen.BookAccess)) return;
        Modules mods = Modules.get();
        if (mods == null) return;
        BookTools bookTools = mods.get(BookTools.class);
        if (bookTools == null) return;   // 26.1: 启动期模块可能未注册
        if (bookTools.skipDeobfuscation()) return;
        List<Component> pages = ((BookScreenContentsAccessor)(Object) bookAccess).getPages();
        if (!pages.isEmpty()) {
            this.deobfuscateButton.visible = pages.get(this.currentPage).getString().contains("§k");
        } else {
            this.deobfuscateButton.visible = false;
        }
    }
}