package gg.packetloss.grindstone.util.listener;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.entity.EntitySpawnBlockedEvent;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
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

        if (!(entity instanceof Monster)) {
            return;
        }

        SpawnReason reason = event.getSpawnReason();
        switch (reason) {
            case CUSTOM:
            case SPAWNER:
            case SPAWNER_EGG:
            case SLIME_SPLIT:
            case ENDER_PEARL:
            case SILVERFISH_BLOCK:

            // This one is a little questionable
            case BUILD_WITHER:
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
