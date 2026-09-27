package bep.hax.modules;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import java.util.List;
import bep.hax.Bep;
import net.minecraft.world.item.Item;
import bep.hax.util.MsgUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.inventory.AnvilMenu;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.core.component.DataComponents;
import bep.hax.mixin.accessor.AnvilScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import bep.hax.mixin.accessor.AnvilScreenHandlerAccessor;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;
public class StashBrander extends Module {
    public StashBrander() { super(Bep.STARDUST, "StashBrander","Allows you to automatically rename items in bulk when using anvils."); }
    private final Setting<List<Item>> itemList = settings.getDefaultGroup().add(
        new ItemListSetting.Builder()
            .name("items")
            .description("Items to automatically rename (or exclude from being renamed, if blacklist mode is enabled.)")
            .build()
    );
    private final Setting<String> itemName = settings.getDefaultGroup().add(
        new StringSetting.Builder()
            .name("custom-name")
            .description("The name you want to give to qualifying items.")
            .defaultValue("")
            .onChanged(name -> {
                if (name.length() > AnvilMenu.MAX_NAME_LENGTH) {
                    MsgUtil.sendModuleMsg("§4Custom name exceeds max accepted length§8..!", this.name);
                }
            })
            .build()
    );
    private final Setting<Boolean> blacklistMode = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("blacklist-mode")
            .description("Rename all items except the ones selected in the Items list.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> renameNamed = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("rename-prenamed")
            .description("Rename items which already have a different custom name.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> muteAnvils = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("mute-anvils")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> pingOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("sound-ping")
            .description("Play a sound cue when no more items can be renamed.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> pingVolume = settings.getDefaultGroup().add(
        new DoubleSetting.Builder()
            .name("ping-volume")
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(1.0)
            .build()
    );
    private final Setting<Boolean> closeOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("close-anvil")
            .description("Automatically close the anvil screen when no more items can be renamed.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> disableOnDone = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("disable-on-done")
            .description("Automatically disable the module when no more items can be renamed.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> enableExpThrower = settings.getDefaultGroup().add(
        new BoolSetting.Builder()
            .name("enable-exp-thrower")
            .description("Automatically enable the Exp Thrower module when no more items can be renamed.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> tickRate = settings.getDefaultGroup().add(
        new IntSetting.Builder()
            .name("tick-rate")
            .min(0).max(1000)
            .sliderRange(0, 100)
            .defaultValue(0)
            .build()
    );
    private int timer = 0;
    private boolean notified = false;
    private static final int ANVIL_OFFSET = 3;
    public boolean shouldMute() { return muteAnvils.get(); }
    private boolean hasValidItems(AnvilMenu handler) {
        if (mc.player == null) return false;
        for (int n = 0; n < ((PlayerInventoryAccessor) mc.player.getInventory()).getMain().size() + ANVIL_OFFSET; n++) {
            if (n == 2) continue;
            ItemStack stack = handler.getSlot(n).getItem();
            if ((blacklistMode.get() && !itemList.get().contains(stack.getItem()))
                || (!blacklistMode.get() && itemList.get().contains(stack.getItem())))
            {
                if (itemName.get().isBlank() && stack.has(DataComponents.CUSTOM_NAME)) return true;
                else if (!stack.getHoverName().getString().equals(itemName.get())) return true;
            }
        }
        return false;
    }
    private void noXP() {
        if (mc.player == null) return;
        if (!notified) {
            MsgUtil.sendModuleMsg("Not enough experience§c..!", this.name);
            if (pingOnDone.get()) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
        }
        notified = true;
        if (closeOnDone.get()) mc.player.closeContainer();
        if (disableOnDone.get()) this.toggle();
        if (enableExpThrower.get() && !Modules.get().isActive(EXPThrower.class)) Modules.get().get(EXPThrower.class).toggle();
    }
    private void finished() {
        if (mc.player == null) return;
        if (!notified) {
            MsgUtil.sendModuleMsg("No more items to rename§a..!", this.name);
            if (pingOnDone.get()) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, pingVolume.get().floatValue(), 1.0f);
        }
        notified = true;
        if (closeOnDone.get()) mc.player.closeContainer();
        if (disableOnDone.get()) this.toggle();
    }
    @Override
    public void onDeactivate() {
        timer = 0;
        notified = false;
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (mc.screen == null) {
            notified = false;
            return;
        }
        if (!(mc.screen instanceof AnvilScreen anvilScreen)) return;
        if (!(mc.player.containerMenu instanceof AnvilMenu anvil)) return;
        if (timer < tickRate.get()) {
            timer++;
            return;
        } else {
            timer = 0;
        }
        ItemStack input1 = anvil.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack input2 = anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        ItemStack output = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        if (!hasValidItems(anvil)) finished();
        else if (input1.isEmpty() && input2.isEmpty()) {
            for (int n = ANVIL_OFFSET; n < ((PlayerInventoryAccessor) mc.player.getInventory()).getMain().size() + ANVIL_OFFSET; n++) {
                ItemStack stack = anvil.getSlot(n).getItem();
                if (stack.has(DataComponents.CUSTOM_NAME) && !renameNamed.get()) continue;
                else if (stack.getHoverName().getString().equals(itemName.get())) continue;
                else if (itemName.get().isBlank() && !stack.has(DataComponents.CUSTOM_NAME)) continue;
                if ((blacklistMode.get() && !itemList.get().contains(stack.getItem()))
                    || (!blacklistMode.get() && itemList.get().contains(stack.getItem())))
                {
                    InvUtils.shiftClick().slotId(n);
                    ((AnvilScreenAccessor) anvilScreen).getNameField().setValue(itemName.get());
                    ItemStack check = anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem();
                    if (itemList.get().contains(check.getItem())) {
                        if (check.getHoverName().getString().equals(itemName.get()) || (itemName.get().isBlank() && stack.has(DataComponents.CUSTOM_NAME))) {
                            int cost = ((AnvilScreenHandlerAccessor) anvil).getLevelCost().get();
                            if (mc.player.experienceLevel >= cost) {
                                InvUtils.shiftClick().slotId(AnvilMenu.RESULT_SLOT);
                            } else noXP();
                            return;
                        }
                    }
                }
            }
            finished();
        } else if (!output.isEmpty() && itemList.get().contains(output.getItem())) {
            if (output.getHoverName().getString().equals(itemName.get()) || (itemName.get().isBlank() && input1.has(DataComponents.CUSTOM_NAME))) {
                int cost = ((AnvilScreenHandlerAccessor) anvil).getLevelCost().get();
                if (mc.player.experienceLevel >= cost) {
                    InvUtils.shiftClick().slotId(AnvilMenu.RESULT_SLOT);
                } else noXP();
            }
        } else if (!input2.isEmpty()) {
            InvUtils.shiftClick().slotId(AnvilMenu.ADDITIONAL_SLOT);
        } else if (output.isEmpty()) {
            InvUtils.shiftClick().slotId(AnvilMenu.INPUT_SLOT);
        }
    }
}