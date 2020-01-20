package gg.packetloss.grindstone.spectator;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.function.Supplier;

class SpectatorSkull {
    private static final ParticleBuilder PASSIVE_PARTICLE_EFFECT = new ParticleBuilder(Particle.ENCHANTMENT_TABLE).allPlayers();

    private final Location location;
    private final Supplier<Boolean> hasPlayers;

    SpectatorSkull(Location location, Supplier<Boolean> hasPlayers) {
        this.location = location.clone().add(.5, .5, .5);
        this.hasPlayers = hasPlayers;
    }

    private void glow(int particleCount) {
        PASSIVE_PARTICLE_EFFECT
                .location(location)
                .count(particleCount)
                .spawn();
    }

    public void glow() {
        if (hasPlayers.get()) {
            glow(15);
        } else {
            glow(1);
        }
    }
}
