package gg.packetloss.grindstone.items.specialattack.attacks.ranged.guild.ninja;

import com.sk89q.worldedit.math.BlockVector3;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.SimpleRayTrace;
import gg.packetloss.grindstone.util.VectorUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Ignition extends EntityAttack implements RangedSpecial {
    private static final int RADIUS = 4;

    private final World world = owner.getWorld();

    private boolean finished = false;
    private Set<BlockVector3> ignitionPoints = new HashSet<>();

    public Ignition(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private void spawnParticles() {
        // Reuse a location to keep the GC from potentially picking up a lot of garbage
        Location loc = new Location(world, 0, 0, 0);
        for (BlockVector3 point : ignitionPoints) {
            loc.set(point.getX(), point.getY(), point.getZ());
            SingleBlockParticleEffect.burstOfFlames(loc);
        }
    }

    private void spawnDamager() {
        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(10);
        taskBuilder.setNumberOfRuns(12);

        taskBuilder.setAction((times) -> {
            if (ignitionPoints.isEmpty()) {
                return finished;
            }

            if (owner instanceof Player) {
                server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
            }

            spawnParticles();

            Class<? extends Entity> filterType = target.getClass();
            if (Monster.class.isAssignableFrom(filterType)) {
                filterType = Monster.class;
            }

            for (Entity e : world.getEntitiesByClasses(filterType)) {
                if (!e.isValid() || e.equals(owner)) continue;

                if (ignitionPoints.contains(WorldEditBridge.toBlockVec3(e.getLocation()))) {
                    SpecialAttackFactory.processDamage(owner, (LivingEntity) e, this, 5);
                }
            }

            return finished;
        });

        taskBuilder.build();
    }

    private void runTracker(SimpleRayTrace it, int distance) {
        if (!it.hasNext()) {
            finished = true;
            return;
        }

        server.getScheduler().runTaskLater(inst, () -> {
            Location loc = it.next();

            for (int i = 0; i < 5 && it.hasNext(); ++i) {
                loc = it.next();
                SingleBlockParticleEffect.burstOfFlames(loc);
            }

            Class<? extends LivingEntity> filterType = target.getClass();
            if (Monster.class.isAssignableFrom(filterType)) {
                filterType = Monster.class;
            }

            Collection<? extends LivingEntity> entityList = loc.getNearbyEntitiesByType(filterType, RADIUS);

            for (Entity e : entityList) {
                if (e.isValid() && !e.equals(owner)) {
                    ignitionPoints.add(WorldEditBridge.toBlockVec3(e.getLocation()));
                }
            }

            runTracker(it, distance + 1);
        }, 1);
    }

    @Override
    public void activate() {
        int maxBlocks = (int) owner.getLocation().distance(target.getLocation()) + 15;

        Vector vel = VectorUtil.createDirectionalVector(owner.getLocation(), target.getLocation());

        SimpleRayTrace it = new SimpleRayTrace(
                owner.getLocation(),
                vel,
                maxBlocks
        );

        runTracker(it, 0);
        spawnDamager();

        inform("Your bow ignites the air.");
    }
}
