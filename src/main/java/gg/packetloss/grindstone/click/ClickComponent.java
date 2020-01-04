package gg.packetloss.grindstone.click;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.DoubleClickEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ComponentInformation(friendlyName = "Click Component", desc = "Track player clicks.")
public class ClickComponent extends BukkitComponent implements Listener {
    private Map<UUID, ClickRecord> clickRecordMap = new HashMap<>();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    private ClickRecord getClickRecordFor(Player player) {
        UUID playerID = player.getUniqueId();
        clickRecordMap.putIfAbsent(playerID, new ClickRecord());
        return clickRecordMap.get(playerID);
    }

    @EventHandler
    public void onEntityDamageEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        Entity attacker = event.getDamager();
        if (attacker instanceof Player) {
            ClickRecord clickRecord = getClickRecordFor((Player) attacker);
            clickRecord.recordAttack();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        ClickRecord clickRecord = getClickRecordFor(player);
        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
                if (clickRecord.isDoubleLeftClick()) {
                    CommandBook.callEvent(new DoubleClickEvent(
                            player, ClickType.LEFT, null, null
                    ));
                }
                break;
            case LEFT_CLICK_BLOCK:
                if (clickRecord.isDoubleLeftClick()) {
                    CommandBook.callEvent(new DoubleClickEvent(
                            player, ClickType.LEFT, event.getClickedBlock(), event.getBlockFace()
                    ));
                }
                break;
            case RIGHT_CLICK_AIR:
                if (clickRecord.isDoubleRightClick()) {
                    CommandBook.callEvent(new DoubleClickEvent(
                            player, ClickType.RIGHT, null, null
                    ));
                }
                break;
            case RIGHT_CLICK_BLOCK:
                if (clickRecord.isDoubleRightClick()) {
                    CommandBook.callEvent(new DoubleClickEvent(
                            player, ClickType.RIGHT, event.getClickedBlock(), event.getBlockFace()
                    ));
                }
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clickRecordMap.remove(event.getPlayer().getUniqueId());
    }
}
