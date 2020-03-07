package gg.packetloss.grindstone.util.listener;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.entity.EntitySpawnBlockedEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.function.Predicate;

public class BetterMobSpawningListener implements Listener {
    private static final int SAMPLE_SIZE = 5;
    private static final int SAMPLE_DIST = 5;
    private static final int LIGHT_LEVEL_SPAWN_THRESHOLD = 8;

    private final Predicate<World> appliesTo;

    public BetterMobSpawningListener(Predicate<World> appliesTo) {
        this.appliesTo = appliesTo;
    }

    private int sampleLightLevel(Location loc) {
        int relevantSamples = 0;
        int totalLight = 0;

        for (int i = 0; i < SAMPLE_SIZE; ++i) {
            Block block = loc.clone().add(
                    ChanceUtil.getPolarizedRandom(SAMPLE_DIST),
                    0,
                    ChanceUtil.getPolarizedRandom(SAMPLE_DIST)
            ).getBlock();

            if (block.getType().isSolid()) {
                continue;
            }

            ++relevantSamples;
            totalLight += block.getLightLevel();
        }

        return totalLight / relevantSamples;
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

        if (sampleLightLevel(entity.getLocation()) < LIGHT_LEVEL_SPAWN_THRESHOLD) {
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
