package bep.hax.mixin;
import bep.hax.modules.ShulkerOverviewModule;
import bep.hax.modules.ItemSearchBar;
import bep.hax.modules.AutoCraft;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {
    protected HandledScreenMixin(Component title) {
        super(title);
    }
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow public abstract AbstractContainerMenu getScreenHandler();
    @Unique private EditBox itemSearchField;
    @Unique private ItemSearchBar itemSearchModule;
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        itemSearchModule = Modules.get().get(ItemSearchBar.class);
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        itemSearchField = new EditBox(
            Minecraft.getInstance().font,
            this.x + itemSearchModule.getOffsetX(),
            this.y + itemSearchModule.getOffsetY(),
            itemSearchModule.getFieldWidth(),
            itemSearchModule.getFieldHeight(),
            Component.literal("Search items...")
        );
        itemSearchField.setHint(Component.literal("Search items..."));
        itemSearchField.setMaxLength(100);
        String currentQuery = itemSearchModule.searchQuery.get();
        if (currentQuery != null && !currentQuery.isEmpty()) {
            itemSearchField.setValue(currentQuery);
        }
        itemSearchField.setResponder(text -> {
            if (itemSearchModule != null) {
                itemSearchModule.updateSearchQuery(text);
            }
        });
        itemSearchField.setFocused(false);
        itemSearchField.setEditable(true);
        itemSearchField.setVisible(true);
        this.addDrawableChild(itemSearchField);
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        if (itemSearchField == null) return;
        itemSearchField.setX(this.x + itemSearchModule.getOffsetX());
        itemSearchField.setY(this.y + itemSearchModule.getOffsetY());
        itemSearchField.setVisible(true);
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        if (itemSearchField == null) return;
        int keyCode = input.key();
        if (keyCode == 258) {
            this.setFocused(itemSearchField);
            itemSearchField.setFocused(true);
            cir.setReturnValue(true);
            return;
        }
        if (keyCode == 256 && itemSearchField.isFocused()) {
            this.setFocused(null);
            itemSearchField.setFocused(false);
            cir.setReturnValue(true);
            return;
        }
        if (itemSearchField.isFocused()) {
            itemSearchField.keyPressed(input);
            if (keyCode != 256) {
                cir.setReturnValue(true);
            }
        }
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean pressed, CallbackInfoReturnable<Boolean> cir) {
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        if (itemSearchField == null) return;
        double mouseX = click.x();
        double mouseY = click.y();
        boolean clickedOnField = mouseX >= itemSearchField.getX() &&
                                mouseX < itemSearchField.getX() + itemSearchField.getWidth() &&
                                mouseY >= itemSearchField.getY() &&
                                mouseY < itemSearchField.getY() + itemSearchField.getHeight();
        if (clickedOnField) {
            this.setFocused(itemSearchField);
            itemSearchField.setFocused(true);
            if (itemSearchField.mouseClicked(click, pressed)) {
                cir.setReturnValue(true);
                return;
            }
        } else {
            if (this.getFocused() == itemSearchField) {
                this.setFocused(null);
            }
            itemSearchField.setFocused(false);
        }
    }
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        if (itemSearchModule != null && itemSearchModule.isActive() && itemSearchModule.shouldShowSearchField()) {
            if (itemSearchField != null && itemSearchField.isFocused()) {
                if (itemSearchField.charTyped(input)) {
                    return true;
                }
            }
        }
        return super.charTyped(input);
    }
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlotHead(GuiGraphicsExtractor context, Slot slot, int x, int y, CallbackInfo ci) {
        if (itemSearchModule != null && itemSearchModule.isActive() && slot.hasItem()) {
            if (itemSearchModule.shouldHighlightSlot(slot.getItem())) {
                context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16,
                    itemSearchModule.highlightColor.get().getPacked());
            }
        }
    }
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlotTail(GuiGraphicsExtractor context, Slot slot, int x, int y, CallbackInfo ci) {
        ShulkerOverviewModule shulkerModule = Modules.get().get(ShulkerOverviewModule.class);
        if (shulkerModule != null && shulkerModule.isActive()) {
            shulkerModule.renderShulkerOverlay(context, slot.x, slot.y, slot.getItem());
        }
    }
}