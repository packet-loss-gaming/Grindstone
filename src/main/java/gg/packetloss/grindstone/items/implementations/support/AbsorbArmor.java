package gg.packetloss.grindstone.items.implementations.support;

import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.function.Predicate;

public class AbsorbArmor {
    private final Predicate<Player> hasArmor;
    private final int basePointOfActivation;
    private final int pointOfActivationFluctuation;
    private final int reductionRolls;

    public AbsorbArmor(Predicate<Player> hasArmor, int basePointOfActivation,
                       int pointOfActivationFluctuation, int reductionRolls) {
        this.hasArmor = hasArmor;
        this.basePointOfActivation = basePointOfActivation;
        this.pointOfActivationFluctuation = pointOfActivationFluctuation;
        this.reductionRolls = reductionRolls;
    }

    public void handleEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        int pointOfActivation = ChanceUtil.getRangedRandom(
                basePointOfActivation,
                basePointOfActivation + pointOfActivationFluctuation
        );

        if (event.getDamage() <= pointOfActivation) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (hasArmor.test(player)) {
            // Calculate the damage that may be removed.
            double extra = event.getDamage() - pointOfActivation;
            double newExtra = ChanceUtil.getRandomNTimes((int) extra, reductionRolls);

            // Change the damage
            event.setDamage(pointOfActivation + newExtra);

            // Inform the player about the armor's effect
            double removed = extra - newExtra;
            if (removed >= 1) {
                ChatUtil.sendNotice(player, "Your armor absorbs " + (int) removed + " damage.");
            }
        }
    }
}
