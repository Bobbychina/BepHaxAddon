package bep.hax.util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ShulkerDataParser {
    public static Map<Item, Integer> parseShulkerContents(ItemStack shulkerStack) {
        Map<Item, Integer> itemCounts = new HashMap<>();
        ItemContainerContents container = shulkerStack.get(DataComponents.CONTAINER);
        if (container != null) {
            List<ItemStack> items = container.stream().toList();
            for (ItemStack itemStack : items) {
                if (!itemStack.isEmpty()) {
                    itemCounts.merge(itemStack.getItem(), itemStack.getCount(), Integer::sum);
                }
            }
            if (!itemCounts.isEmpty()) {
                return itemCounts;
            }
        }
        CustomData customData = shulkerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData != null && !customData.isEmpty()) {
            CompoundTag nbt = customData.copyTag();
            if (nbt != null && nbt.contains("BlockEntityTag")) {
                var optional = nbt.getCompound("BlockEntityTag");
                if (optional.isPresent()) {
                    CompoundTag blockEntityTag = optional.get();
                    if (blockEntityTag.contains("Items")) {
                        var itemsListOpt = blockEntityTag.getList("Items");
                        if (!itemsListOpt.isPresent()) return itemCounts;
                        ListTag items = itemsListOpt.get();
                        for (int i = 0; i < items.size(); i++) {
                            var itemOpt = items.getCompound(i);
                            if (itemOpt.isPresent()) {
                                ItemStack parsed = parseItemFromNbt(itemOpt.get());
                                if (!parsed.isEmpty()) {
                                    itemCounts.merge(parsed.getItem(), parsed.getCount(), Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }
        return itemCounts;
    }
    private static ItemStack parseItemFromNbt(CompoundTag itemTag) {
        String id = itemTag.getString("id", "");
        if (id.isEmpty()) return ItemStack.EMPTY;
        int count = 1;
        if (itemTag.contains("count")) {
            count = itemTag.getInt("count", 1);
        } else if (itemTag.contains("Count")) {
            count = itemTag.getByte("Count", (byte) 1);
        }
        Identifier itemId = Identifier.tryParse(id);
        if (itemId == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == null || item == BuiltInRegistries.ITEM.getValue(BuiltInRegistries.ITEM.getDefaultKey())) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }
}