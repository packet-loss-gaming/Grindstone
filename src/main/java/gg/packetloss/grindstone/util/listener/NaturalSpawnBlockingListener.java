package gg.packetloss.grindstone.util.listener;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.entity.EntitySpawnBlockedEvent;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.function.Predicate;

public class NaturalSpawnBlockingListener implements Listener {
    private final Predicate<World> appliesTo;

    public NaturalSpawnBlockingListener(Predicate<World> appliesTo) {
        this.appliesTo = appliesTo;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDoorBreak(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        if (!appliesTo.test(entity.getWorld())) {
            return;
        }

        if (!EntityUtil.isHostileMob(entity)) {
            return;
        }

        SpawnReason reason = event.getSpawnReason();
        if (EnvironmentUtil.isNonNaturalSpawnReason(reason)) {
            return;
        }

        EntitySpawnBlockedEvent blockEvent = new EntitySpawnBlockedEvent(entity);

        CommandBook.callEvent(blockEvent);
        if (blockEvent.isCancelled()) {
            return;
        }

        event.setCancelled(true);
    }
}
