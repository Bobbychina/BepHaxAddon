package bep.hax.mixin.meteor;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import bep.hax.modules.ItemSearchBar;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;
import bep.hax.util.MsgUtil;
import bep.hax.util.LogUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.item.equipment.Equippable;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.Minecraft;
@Mixin(value = PeekScreen.class, remap = false)
public abstract class PeekScreenMixin extends ShulkerBoxScreen {
    public PeekScreenMixin(ShulkerBoxMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
    @Unique
    private BetterTooltips btt = null;
    @Unique
    private EditBox bephax$searchField;
    @Unique
    private ItemSearchBar bephax$searchModule;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ItemStack storageBlock, ItemStack[] contents, CallbackInfo ci) {
        bephax$searchModule = Modules.get().get(ItemSearchBar.class);
    }
    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void onInitScreen(CallbackInfo ci) {
        if (bephax$searchModule == null || !bephax$searchModule.isActive() || !bephax$searchModule.shouldShowSearchField()) return;
        bephax$searchField = new EditBox(
            Minecraft.getInstance().font,
            this.x + bephax$searchModule.getOffsetX(),
            this.y + bephax$searchModule.getOffsetY(),
            bephax$searchModule.getFieldWidth(),
            bephax$searchModule.getFieldHeight(),
            Component.literal("Search items...")
        );
        bephax$searchField.setHint(Component.literal("Search items..."));
        bephax$searchField.setMaxLength(100);
        String currentQuery = bephax$searchModule.searchQuery.get();
        if (currentQuery != null && !currentQuery.isEmpty()) {
            bephax$searchField.setValue(currentQuery);
        }
        bephax$searchField.setResponder(text -> {
            if (bephax$searchModule != null) {
                bephax$searchModule.updateSearchQuery(text);
            }
        });
        bephax$searchField.setFocused(false);
        bephax$searchField.setEditable(true);
        bephax$searchField.setVisible(true);
        this.addDrawableChild(bephax$searchField);
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (bephax$searchModule == null || !bephax$searchModule.isActive() || !bephax$searchModule.shouldShowSearchField()) return;
        if (bephax$searchField == null) return;
        double mouseX = click.x();
        double mouseY = click.y();
        boolean clickedOnField = mouseX >= bephax$searchField.getX() &&
                                mouseX < bephax$searchField.getX() + bephax$searchField.getWidth() &&
                                mouseY >= bephax$searchField.getY() &&
                                mouseY < bephax$searchField.getY() + bephax$searchField.getHeight();
        if (clickedOnField) {
            bephax$searchField.setFocused(true);
            if (bephax$searchField.mouseClicked(click, doubled)) {
                cir.setReturnValue(true);
                return;
            }
        } else {
            bephax$searchField.setFocused(false);
        }
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (bephax$searchModule == null || !bephax$searchModule.isActive() || !bephax$searchModule.shouldShowSearchField()) return;
        if (bephax$searchField == null) return;
        int keyCode = input.key();
        if (keyCode == 258) {
            bephax$searchField.setFocused(true);
            cir.setReturnValue(true);
            return;
        }
        if (keyCode == 256 && bephax$searchField.isFocused()) {
            bephax$searchField.setFocused(false);
            cir.setReturnValue(true);
            return;
        }
        if (bephax$searchField.isFocused()) {
            bephax$searchField.keyPressed(input);
            if (keyCode != 256) {
                cir.setReturnValue(true);
            }
        }
    }
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        if (bephax$searchModule != null && bephax$searchModule.isActive() && bephax$searchModule.shouldShowSearchField()) {
            if (bephax$searchField != null && bephax$searchField.isFocused()) {
                if (bephax$searchField.charTyped(input)) {
                    return true;
                }
            }
        }
        return super.charTyped(input);
    }
    @Inject(method = "drawBackground", at = @At("TAIL"), remap = true)
    private void onDrawBackground(GuiGraphicsExtractor context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (bephax$searchModule == null || !bephax$searchModule.isActive() || !bephax$searchModule.shouldShowSearchField()) return;
        if (bephax$searchField == null) return;
        bephax$searchField.setX(this.x + bephax$searchModule.getOffsetX());
        bephax$searchField.setY(this.y + bephax$searchModule.getOffsetY());
    }
    @Unique
    private boolean shouldSetComponent(ItemStack stack) {
        return (!stack.has(DataComponents.EQUIPPABLE)
            || !stack.get(DataComponents.EQUIPPABLE).swappable());
    }
}