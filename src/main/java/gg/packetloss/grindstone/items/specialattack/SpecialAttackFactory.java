package gg.packetloss.grindstone.items.specialattack;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.items.CustomItemSession;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class SpecialAttackFactory {
    private final Server server = CommandBook.server();

    private SessionComponent sessions;

    public SpecialAttackFactory(SessionComponent sessions) {
        this.sessions = sessions;
    }

    private SpecialAttackEvent callSpec(Player owner, SpecType context, SpecialAttack spec) {
        SpecialAttackEvent event = new SpecialAttackEvent(owner, context, spec);
        server.getPluginManager().callEvent(event);
        return event;
    }

    public void process(Player player, SpecialAttack spec, SpecType specType, Consumer<SpecialAttackEvent> Modifier) {
        CustomItemSession session = sessions.getSession(CustomItemSession.class, player);

        if (session.canSpec(specType)) {
            SpecialAttackEvent specEvent = callSpec(player, specType, spec);

            Modifier.accept(specEvent);

            if (!specEvent.isCancelled()) {
                session.updateSpec(specType, specEvent.getContextCoolDown());
                specEvent.getSpec().activate();
            }
        }
    }

    public void process(Player player, SpecialAttack spec, SpecType specType) {
        process(player, spec, specType, (specEvent) -> {});
    }
}
