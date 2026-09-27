package bep.hax.modules;
import java.util.List;
import java.util.ArrayDeque;
import net.minecraft.world.item.*;
import bep.hax.Bep;
import bep.hax.util.MsgUtil;
import bep.hax.util.StardustUtil;
import net.minecraft.sounds.SoundEvents;
import meteordevelopment.orbit.EventHandler;
import java.util.concurrent.ThreadLocalRandom;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.inventory.ContainerInput;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.world.inventory.SmithingMenu;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.HashedStack;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
public class AutoSmith extends Module {
    public AutoSmith() {
        super(Bep.STARDUST, "AutoSmith", "Automatically upgrade gear in smithing tables with configurable templates, materials, and equipment.");
    }
    public enum ModuleMode {
        Packet, Interact
    }
    private final SettingGroup sgMode = settings.createGroup("Mode Settings");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<ModuleMode> moduleMode = sgMode.add(
        new EnumSetting.Builder<ModuleMode>()
            .name("module-mode")
            .description("Packet is significantly faster, but may get you kicked in some scenarios.")
            .defaultValue(ModuleMode.Packet)
            .build()
    );
    private final Setting<Integer> tickRate = sgMode.add(
        new IntSetting.Builder()
            .name("tick-delay")
            .description("Increase this if the server is kicking you.")
            .visible(() -> moduleMode.get().equals(ModuleMode.Interact))
            .range(2, 100)
            .sliderRange(2, 20)
            .defaultValue(4)
            .build()
    );
    private final Setting<Integer> packetLimit = sgMode.add(
        new IntSetting.Builder()
            .name("packet-limit")
            .description("Decrease this if the server is kicking you.")
            .visible(() -> moduleMode.get().equals(ModuleMode.Packet))
            .min(20).sliderMax(100)
            .defaultValue(42)
            .build()
    );
    private final Setting<List<Item>> templates = sgItems.add(
        new ItemListSetting.Builder()
            .name("templates")
            .description("Smithing templates to use for upgrading.")
            .defaultValue(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
            .build()
    );
    private final Setting<List<Item>> materials = sgItems.add(
        new ItemListSetting.Builder()
            .name("materials")
            .description("Materials to use for upgrading (ingots, etc).")
            .defaultValue(Items.NETHERITE_INGOT)
            .build()
    );
    private final Setting<List<Item>> equipment = sgItems.add(
        new ItemListSetting.Builder()
            .name("equipment")
            .description("Equipment to upgrade.")
            .defaultValue(
                Items.DIAMOND_HELMET,
                Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS,
                Items.DIAMOND_BOOTS,
                Items.DIAMOND_SWORD,
                Items.DIAMOND_PICKAXE,
                Items.DIAMOND_AXE,
                Items.DIAMOND_SHOVEL,
                Items.DIAMOND_HOE
            )
            .build()
    );
    public final Setting<Boolean> muteSmithy = sgGeneral.add(
        new BoolSetting.Builder()
            .name("mute-smithing-table")
            .description("Mute the smithing table sounds.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> closeOnDone = sgGeneral.add(
        new BoolSetting.Builder()
            .name("close-screen")
            .description("Automatically close the crafting screen when no more gear can be upgraded.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> disableOnDone = sgGeneral.add(
        new BoolSetting.Builder()
            .name("disable-on-done")
            .description("Automatically disable the module when no more gear can be upgraded.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> pingOnDone = sgGeneral.add(
        new BoolSetting.Builder()
            .name("sound-ping")
            .description("Play a sound cue when no more gear can be upgraded.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> pingVolume = sgGeneral.add(
        new DoubleSetting.Builder()
            .name("ping-volume")
            .visible(pingOnDone::get)
            .sliderMin(0.0)
            .sliderMax(5.0)
            .defaultValue(0.5)
            .build()
    );
    private int timer = 0;
    private boolean notified = false;
    private boolean foundEquip = false;
    private boolean foundMaterial = false;
    private boolean foundTemplate = false;
    private ItemStack templateStack = null;
    private ItemStack materialStack = null;
    private ItemStack equipmentStack = null;
    private final IntArrayList projectedEmpty = new IntArrayList();
    private final IntArrayList processedSlots = new IntArrayList();
    private int getInvSize() {
        return ((PlayerInventoryAccessor) mc.player.getInventory()).getMain().size();
    }
    private boolean isValidEquipment(ItemStack stack) {
        return equipment.get().contains(stack.getItem());
    }
    private boolean isValidTemplate(ItemStack stack) {
        return templates.get().contains(stack.getItem());
    }
    private boolean isValidMaterial(ItemStack stack) {
        return materials.get().contains(stack.getItem());
    }
    @Override
    public void onDeactivate() {
        timer = 0;
        templateStack = null;
        notified = false;
        foundEquip = false;
        foundMaterial = false;
        materialStack = null;
        equipmentStack = null;
        foundTemplate = false;
        processedSlots.clear();
        projectedEmpty.clear();
    }
    @EventHandler
    private void onScreenOpened(OpenScreenEvent event) {
        if (event.screen instanceof SmithingScreen) {
            notified = false;
        }
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (mc.getConnection() == null) return;
        if (mc.screen == null) {
            notified = false;
            return;
        }
        if (!(mc.screen instanceof SmithingScreen)) return;
        if (!(mc.player.containerMenu instanceof SmithingMenu ss)) return;
        switch (moduleMode.get()) {
            case Packet -> {
                if (notified) return;
                ArrayDeque<ServerboundContainerClickPacket> packets = new ArrayDeque<>();
                boolean exhausted = false;
                while (!exhausted) {
                    ServerboundContainerClickPacket packet = generateSmithingPacket(ss);
                    if (packet == null) {
                        exhausted = true;
                    } else if (packets.size() >= packetLimit.get()) {
                        exhausted = true;
                        packets.addLast(packet);
                        MsgUtil.sendModuleMsg("Packet limit was hit§c..! §7You may need to run the module again§c...", this.name);
                    } else {
                        packets.addLast(packet);
                    }
                }
                while (!packets.isEmpty()) {
                    mc.getConnection()
                        .getConnection()
                        .send(packets.removeFirst());
                }
                finished();
            }
            case Interact -> {
                if (timer >= tickRate.get()) {
                    timer = 0;
                } else {
                    ++timer;
                    return;
                }
                ItemStack output = ss.getSlot(SmithingMenu.RESULT_SLOT).getItem();
                if (!output.isEmpty()) {
                    InvUtils.shiftClick().slotId(SmithingMenu.RESULT_SLOT);
                    foundEquip = false;
                    int materialsRemaining = ss.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().getCount();
                    int templatesRemaining = ss.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().getCount();
                    if (materialsRemaining == 0) foundMaterial = false;
                    if (templatesRemaining == 0) foundTemplate = false;
                } else if (!foundEquip) {
                    for (int n = 4; n < getInvSize() + 4; n++) {
                        ItemStack stack = ss.getSlot(n).getItem();
                        if (isValidEquipment(stack)) {
                            foundEquip = true;
                            InvUtils.shiftClick().slotId(n);
                            break;
                        }
                    }
                    if (!foundEquip && !notified) {
                        MsgUtil.sendModuleMsg("No gear left to upgrade§c..!", this.name);
                        finished();
                    }
                } else if (!foundMaterial) {
                    for (int n = 4; n < getInvSize() + 4; n++) {
                        ItemStack stack = ss.getSlot(n).getItem();
                        if (isValidMaterial(stack)) {
                            foundMaterial = true;
                            InvUtils.shiftClick().slotId(n);
                            break;
                        }
                    }
                    if (!foundMaterial && !notified) {
                        MsgUtil.sendModuleMsg("No materials left to use§c..!", this.name);
                        finished();
                    }
                } else if (!foundTemplate) {
                    for (int n = 4; n < getInvSize() + 4; n++) {
                        ItemStack stack = ss.getSlot(n).getItem();
                        if (isValidTemplate(stack)) {
                            foundTemplate = true;
                            InvUtils.shiftClick().slotId(n);
                            break;
                        }
                    }
                    if (!foundTemplate && !notified) {
                        MsgUtil.sendModuleMsg("No templates left to use§c..!", this.name);
                        finished();
                    }
                } else {
                    timer = tickRate.get() - 1;
                }
            }
        }
    }
    private void finished() {
        if (mc.player == null) {
            notified = true;
            return;
        }
        if (!notified) {
            MsgUtil.sendModuleMsg("Finished processing items" + StardustUtil.rCC() + "..!", this.name);
            if (pingOnDone.get()) {
                mc.player.playSound(
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    pingVolume.get().floatValue(),
                    ThreadLocalRandom.current().nextFloat(0.69f, 1.337f)
                );
            }
        }
        notified = true;
        processedSlots.clear();
        projectedEmpty.clear();
        if (closeOnDone.get()) mc.player.closeContainer();
        if (disableOnDone.get()) toggle();
    }
    private ServerboundContainerClickPacket generateSmithingPacket(SmithingMenu handler) {
        if (mc.player == null) return null;
        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>();
        if (templateStack != null && materialStack != null && equipmentStack != null) {
            int templateCount = templateStack.getCount();
            int materialCount = materialStack.getCount();
            changedSlots.put(SmithingMenu.RESULT_SLOT, ItemStack.EMPTY);
            changedSlots.put(SmithingMenu.BASE_SLOT, ItemStack.EMPTY);
            if (templateCount - 1 > 0) {
                ItemStack newTemplateStack = templateStack.copyWithCount(templateCount - 1);
                changedSlots.put(SmithingMenu.TEMPLATE_SLOT, newTemplateStack);
                templateStack = newTemplateStack;
            } else {
                changedSlots.put(SmithingMenu.TEMPLATE_SLOT, ItemStack.EMPTY);
                templateStack = null;
            }
            if (materialCount - 1 > 0) {
                ItemStack newMaterialStack = materialStack.copyWithCount(materialCount - 1);
                changedSlots.put(SmithingMenu.ADDITIONAL_SLOT, newMaterialStack);
                materialStack = newMaterialStack;
            } else {
                changedSlots.put(SmithingMenu.ADDITIONAL_SLOT, ItemStack.EMPTY);
                materialStack = null;
            }
            int shiftClickTargetSlot = predictEmptySlot(handler);
            if (shiftClickTargetSlot == -1) {
                MsgUtil.sendModuleMsg("Failed to predict empty target slot§c..!", this.name);
                return null;
            }
            ItemStack output = getUpgradedItem(equipmentStack);
            changedSlots.put(shiftClickTargetSlot, output);
            equipmentStack = null;
            Int2ObjectMap<HashedStack> hashMap = new Int2ObjectOpenHashMap<>();
            changedSlots.forEach((slot, stack) -> hashMap.put(slot, HashedStack.create(stack, component -> 0)));
            return new ServerboundContainerClickPacket(
                handler.containerId, handler.getStateId(), (short) SmithingMenu.RESULT_SLOT, (byte) 0,
                ContainerInput.QUICK_MOVE, hashMap, HashedStack.create(ItemStack.EMPTY, component -> 0)
            );
        }
        if (equipmentStack == null) {
            for (int n = 4; n < getInvSize() + 4; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getItem();
                if (isValidEquipment(stack)) {
                    equipmentStack = stack;
                    processedSlots.add(n);
                    projectedEmpty.add(n);
                    processedSlots.add(SmithingMenu.BASE_SLOT);
                    changedSlots.put(SmithingMenu.BASE_SLOT, stack);
                    changedSlots.put(n, ItemStack.EMPTY);
                    if (templateStack != null && materialStack != null) {
                        ItemStack output = getUpgradedItem(equipmentStack);
                        changedSlots.put(SmithingMenu.RESULT_SLOT, output);
                    }
                    Int2ObjectMap<HashedStack> hashMap = new Int2ObjectOpenHashMap<>();
                    changedSlots.forEach((slot, stack2) -> hashMap.put(slot, HashedStack.create(stack2, component -> 0)));
                    return new ServerboundContainerClickPacket(
                        handler.containerId, handler.getStateId(), (short) n, (byte) 0,
                        ContainerInput.QUICK_MOVE, hashMap, HashedStack.create(ItemStack.EMPTY, component -> 0)
                    );
                }
            }
            return null;
        }
        if (materialStack == null) {
            for (int n = 4; n < getInvSize() + 4; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getItem();
                if (isValidMaterial(stack)) {
                    materialStack = stack;
                    processedSlots.add(n);
                    projectedEmpty.add(n);
                    processedSlots.add(SmithingMenu.ADDITIONAL_SLOT);
                    changedSlots.put(SmithingMenu.ADDITIONAL_SLOT, stack);
                    changedSlots.put(n, ItemStack.EMPTY);
                    if (templateStack != null) {
                        ItemStack output = getUpgradedItem(equipmentStack);
                        changedSlots.put(SmithingMenu.RESULT_SLOT, output);
                    }
                    Int2ObjectMap<HashedStack> hashMap = new Int2ObjectOpenHashMap<>();
                    changedSlots.forEach((slot, stack2) -> hashMap.put(slot, HashedStack.create(stack2, component -> 0)));
                    return new ServerboundContainerClickPacket(
                        handler.containerId, handler.getStateId(), (short) n, (byte) 0,
                        ContainerInput.QUICK_MOVE, hashMap, HashedStack.create(ItemStack.EMPTY, component -> 0)
                    );
                }
            }
            return null;
        }
        if (templateStack == null) {
            for (int n = 4; n < getInvSize() + 4; n++) {
                if (processedSlots.contains(n)) continue;
                ItemStack stack = handler.getSlot(n).getItem();
                if (isValidTemplate(stack)) {
                    templateStack = stack;
                    processedSlots.add(n);
                    projectedEmpty.add(n);
                    processedSlots.add(SmithingMenu.TEMPLATE_SLOT);
                    changedSlots.put(SmithingMenu.TEMPLATE_SLOT, stack);
                    changedSlots.put(n, ItemStack.EMPTY);
                    if (equipmentStack != null && materialStack != null) {
                        ItemStack output = getUpgradedItem(equipmentStack);
                        changedSlots.put(SmithingMenu.RESULT_SLOT, output);
                    }
                    Int2ObjectMap<HashedStack> hashMap = new Int2ObjectOpenHashMap<>();
                    changedSlots.forEach((slot, stack2) -> hashMap.put(slot, HashedStack.create(stack2, component -> 0)));
                    return new ServerboundContainerClickPacket(
                        handler.containerId, handler.getStateId(), (short) n, (byte) 0,
                        ContainerInput.QUICK_MOVE, hashMap, HashedStack.create(ItemStack.EMPTY, component -> 0)
                    );
                }
            }
            return null;
        }
        return null;
    }
    @SuppressWarnings("deprecation")
    private ItemStack getUpgradedItem(ItemStack original) {
        if (original.is(Items.DIAMOND_HELMET)) {
            return new ItemStack(Items.NETHERITE_HELMET.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_CHESTPLATE)) {
            return new ItemStack(Items.NETHERITE_CHESTPLATE.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_LEGGINGS)) {
            return new ItemStack(Items.NETHERITE_LEGGINGS.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_BOOTS)) {
            return new ItemStack(Items.NETHERITE_BOOTS.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_SWORD)) {
            return new ItemStack(Items.NETHERITE_SWORD.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_PICKAXE)) {
            return new ItemStack(Items.NETHERITE_PICKAXE.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_AXE)) {
            return new ItemStack(Items.NETHERITE_AXE.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_SHOVEL)) {
            return new ItemStack(Items.NETHERITE_SHOVEL.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else if (original.is(Items.DIAMOND_HOE)) {
            return new ItemStack(Items.NETHERITE_HOE.builtInRegistryHolder(), original.getCount(), original.getComponentsPatch());
        } else {
            return original;
        }
    }
    private int predictEmptySlot(SmithingMenu handler) {
        if (mc.player == null) return -1;
        for (int n = getInvSize() + 3; n >= 4; n--) {
            if (processedSlots.contains(n) && !projectedEmpty.contains(n)) continue;
            if (projectedEmpty.contains(n)) {
                projectedEmpty.rem(n);
                return n;
            } else if (handler.getSlot(n).getItem().isEmpty()) {
                processedSlots.add(n);
                return n;
            }
        }
        return -1;
    }
}