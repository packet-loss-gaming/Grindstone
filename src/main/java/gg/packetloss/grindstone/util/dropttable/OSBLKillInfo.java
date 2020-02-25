package gg.packetloss.grindstone.util.dropttable;

import com.skelril.OSBL.entity.LocalControllable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class OSBLKillInfo implements PerformanceKillInfo {
    private final LocalControllable<?> controllable;

    public OSBLKillInfo(LocalControllable<?> controllable) {
        this.controllable = controllable;
    }

    @Override
    public double getTotalDamage() {
        double total = 0;
        for (UUID damager : controllable.getDamagers()) {
            total += controllable.getDamage(damager).orElseThrow();
        }
        return total;
    }

    @Override
    public Optional<Double> getDamageDone(Player player) {
        return controllable.getDamage(player.getUniqueId());
    }

    @Override
    public Optional<Float> getPercentDamageDone(Player player) {
        Optional<Double> damageDone = getDamageDone(player);
        if (damageDone.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((float) (damageDone.get() / getTotalDamage()));
    }

    @Override
    public Collection<Player> getDamagers() {
        List<Player> damagers = new ArrayList<>();

        for (UUID damager : controllable.getDamagers()) {
            Player player = Bukkit.getPlayer(damager);
            if (player != null) {
                damagers.add(player);
            }
        }

        return damagers;
    }
}
