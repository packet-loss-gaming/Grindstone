package gg.packetloss.grindstone.util.listener;

import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.function.Predicate;

public class InventorySlotBlockingListener implements Listener {
    private final Predicate<Player> appliesToPlayer;
    private final Predicate<InventoryType.SlotType> appliesToSlot;

    public InventorySlotBlockingListener(Predicate<Player> appliesToPlayer, Predicate<InventoryType.SlotType> appliesToSlot) {
        this.appliesToPlayer = appliesToPlayer;
        this.appliesToSlot = appliesToSlot;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (!appliesToPlayer.test(player)) return;

        InventoryType.SlotType st = event.getSlotType();
        if (appliesToSlot.test(st)) {
            event.setResult(Event.Result.DENY);
            ChatUtil.sendWarning(player, "You can't do that right now.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (!appliesToPlayer.test(player)) return;

        for (int slot : event.getInventorySlots()) {
            InventoryType.SlotType st = event.getView().getSlotType(slot);
            if (appliesToSlot.test(st)) {
                event.setResult(Event.Result.DENY);
                ChatUtil.sendWarning(player, "You can't do that right now.");
                break;
            }
        }
    }
}
