package gg.packetloss.grindstone.util.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.sk89q.commandbook.CommandBook;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BossBuggedRespawnListener implements Listener {
    private final String bossName;

    private final Predicate<LivingEntity> shouldRespawn;
    private final Consumer<LivingEntity> respawnHook;

    public BossBuggedRespawnListener(String bossName, Predicate<LivingEntity> shouldRespawn,
                                     Consumer<LivingEntity> respawnHook) {
        this.bossName = bossName;
        this.shouldRespawn = shouldRespawn;
        this.respawnHook = respawnHook;
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        LivingEntity living = (LivingEntity) entity;
        if (shouldRespawn.test(living)) {
            CommandBook.logger().info("Respawning " + bossName + " due to bugged despawn");
            respawnHook.accept(living);
        }
    }
}
