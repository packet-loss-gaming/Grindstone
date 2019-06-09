package gg.packetloss.grindstone.items.implementations.support;

import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.function.Predicate;

public class NecrosAbsorb {
    private final Predicate<Player> hasArmor;

    public NecrosAbsorb(Predicate<Player> hasArmor) {
        this.hasArmor = hasArmor;
    }

    public void handleEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        int pointOfActivation = ChanceUtil.getRangedRandom(20, 30);
        if (event.getDamage() <= pointOfActivation) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (hasArmor.test(player)) {
            // Calculate the damage that may be removed.
            double extra = event.getDamage() - pointOfActivation;
            double newExtra = ChanceUtil.getRandomNTimes((int) extra, 2);

            double original = event.getDamage();

            // Change the damage
            event.setDamage(pointOfActivation + newExtra);

            // Inform the player about the armor's effect
            double removed = extra - newExtra;
            if (removed >= 1) {
                ChatUtil.sendNotice(player, "Your armor absorbs " + (int) removed + " damaged.");
            }
        }
    }
}
