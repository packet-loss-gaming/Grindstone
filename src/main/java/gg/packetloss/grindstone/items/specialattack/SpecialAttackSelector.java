package gg.packetloss.grindstone.items.specialattack;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackSelectEvent;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Supplier;

public class SpecialAttackSelector {
    private final Player player;
    private final SpecType specType;
    private final Supplier<SpecialAttack> supplier;

    public SpecialAttackSelector(Player player, SpecType specType, Supplier<SpecialAttack> supplier) {
        this.player = player;
        this.specType = specType;
        this.supplier = supplier;
    }

    public Optional<SpecialAttack> getSpecial() {
        while (true) {
            SpecialAttackSelectEvent selectEvent = new SpecialAttackSelectEvent(player, specType, supplier.get());
            CommandBook.callEvent(selectEvent);

            if (selectEvent.shouldTryAgain()) {
                continue;
            }

            if (selectEvent.isCancelled()) {
                return Optional.empty();
            } else {
                return Optional.of(selectEvent.getSpec());
            }
        }
    }
}
