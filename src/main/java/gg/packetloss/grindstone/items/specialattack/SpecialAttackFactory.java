package gg.packetloss.grindstone.items.specialattack;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackPreDamageEvent;
import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.util.tracker.CauseStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Consumer;

public class SpecialAttackFactory {
    private static final CauseStack<SpecialAttack> attackStack = new CauseStack<>();

    private SessionComponent sessions;

    public SpecialAttackFactory(SessionComponent sessions) {
        this.sessions = sessions;
    }

    public static Optional<SpecialAttack> getCurrentSpecialAttack() {
        return attackStack.getCurCause();
    }

    public static boolean processDamage(LivingEntity attacker, LivingEntity defender,
                                        SpecialAttack spec, double amount) {
        if (defender.isDead() || defender.getNoDamageTicks() > 0) {
            return false;
        }

        SpecialAttackPreDamageEvent event = new SpecialAttackPreDamageEvent(attacker, defender, spec, amount);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        attackStack.executeOnStackWithCause(spec, () -> {
            event.getDefender().damage(event.getDamage(), attacker);
        });

        return true;
    }

    private SpecialAttackEvent callSpec(Player owner, SpecType context, SpecialAttack spec) {
        SpecialAttackEvent event = new SpecialAttackEvent(owner, context, spec);
        CommandBook.server().getPluginManager().callEvent(event);
        return event;
    }

    public void process(Player player, SpecialAttack spec, SpecType specType, Consumer<SpecialAttackEvent> Modifier) {
        CustomItemSession session = sessions.getSession(CustomItemSession.class, player);

        if (!session.canSpec(specType)) {
            return;
        }

        SpecialAttackEvent specEvent = callSpec(player, specType, spec);

        Modifier.accept(specEvent);

        if (specEvent.isCancelled()) {
            return;
        }

        session.updateSpec(specType, specEvent.getContextCoolDown());

        attackStack.executeOnStackWithCause(spec, () -> {
            specEvent.getSpec().activate();
        });
    }

    public void process(Player player, SpecialAttack spec, SpecType specType) {
        process(player, spec, specType, (specEvent) -> {});
    }
}
