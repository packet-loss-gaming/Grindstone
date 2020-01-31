package gg.packetloss.grindstone.items;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ComponentInformation(friendlyName = "Items Blacklist Component", desc = "Block certain items")
public class ItemBlacklistComponent extends BukkitComponent implements Listener {
    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    private static final Map<Material, Function<ItemStack, ItemStack>> REMAPPED_ITEM_DROPS = new HashMap<>();

    static {
        REMAPPED_ITEM_DROPS.put(
                Material.TOTEM_OF_UNDYING,
                (from) -> CustomItemCenter.build(CustomItems.GEM_OF_LIFE, from.getAmount())
        );
    }

    private ItemStack remapStack(ItemStack stack) {
        Function<ItemStack, ItemStack> remapper = REMAPPED_ITEM_DROPS.get(stack.getType());
        if (remapper == null) {
            return null;
        }

        return remapper.apply(stack);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        ItemStack itemStack = item.getItemStack();

        if (ItemUtil.isInItemFamily(itemStack, ItemFamily.PWNG)) {
            item.remove();
            ChatUtil.sendNotice(event.getPlayer(), ChatColor.DARK_RED + "This item is too powerful for mere mortals.");
            return;
        }

        ItemStack newStack = remapStack(itemStack);
        if (newStack != null) {
            item.setItemStack(newStack);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent event) {
        List<ItemStack> itemStacks = event.getDrops();
        for (int i = 0; i < itemStacks.size(); ++i) {
            ItemStack oldStack = itemStacks.get(i);

            ItemStack newStack = remapStack(oldStack);
            if (newStack == null) {
                continue;
            }

            itemStacks.set(i, newStack);
        }
    }
}
