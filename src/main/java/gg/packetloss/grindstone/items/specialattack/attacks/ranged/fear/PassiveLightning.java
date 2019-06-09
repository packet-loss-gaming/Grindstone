package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.LocationAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.entity.*;

public class PassiveLightning extends LocationAttack implements RangedSpecial {
    private final Projectile projectile;

    public PassiveLightning(Projectile projectile) {
        super((LivingEntity) projectile.getShooter(), projectile.getLocation());
        this.projectile = projectile;
    }

    @Override
    public void activate() {
        if (owner instanceof Player) {
            server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
        }

        // Simulate a lightning strike
        projectile.getWorld().strikeLightningEffect(projectile.getLocation());
        for (Entity e : projectile.getNearbyEntities(2, 4, 2)) {
            if (!e.isValid() || !(e instanceof LivingEntity)) continue;
            // Pig Zombie
            if (e instanceof Pig) {
                e.getWorld().spawn(e.getLocation(), PigZombie.class);
                e.remove();
                continue;
            }
            // Creeper
            if (e instanceof Creeper) {
                ((Creeper) e).setPowered(true);
            }

            DamageUtil.damageWithSpecialAttack(owner, (LivingEntity) e, this, 5);
        }
    }
}