package gg.packetloss.grindstone.city.engine.area.areas.RogueChaser;

import gg.packetloss.grindstone.city.engine.area.AreaListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class RogueChaserListener extends AreaListener<RogueChaser> {
    public RogueChaserListener(RogueChaser parent) {
        super(parent);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (parent.isChased(entity)) {
            Entity damager = event.getDamager();
            if (damager instanceof Player) {
                parent.chasedHitBy((Player) damager);
            }

            event.setCancelled(true);
        }
    }
}
