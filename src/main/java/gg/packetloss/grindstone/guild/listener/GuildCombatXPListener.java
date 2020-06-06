package gg.packetloss.grindstone.guild.listener;

import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;
import java.util.function.Function;

public class GuildCombatXPListener implements Listener {
    private Function<Player, Optional<GuildState>> stateLookup;

    public GuildCombatXPListener(Function<Player, Optional<GuildState>> stateLookup) {
        this.stateLookup = stateLookup;
    }

    private static EDBEExtractor<Player, LivingEntity, Arrow> extractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Arrow.class
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() < 1) {
            return;
        }

        CombatantPair<Player, LivingEntity, Arrow> result = extractor.extractFrom(event);
        if (result == null) return;

        final Player attacker = result.getAttacker();
        stateLookup.apply(attacker).ifPresent((guildState) -> {
            double maxDamage = Math.min(500, Math.min(result.getDefender().getMaxHealth(), event.getFinalDamage()));
            guildState.grantExp(maxDamage * .1);
        });
    }
}
