/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.spectator;

import com.destroystokyo.paper.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.function.Supplier;

class SpectatorSkull {
    private static final ParticleBuilder PASSIVE_PARTICLE_EFFECT = new ParticleBuilder(Particle.ENCHANT).allPlayers();

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
