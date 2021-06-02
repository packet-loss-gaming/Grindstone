/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.MirageArena;

import com.sk89q.commandbook.component.session.PersistentSession;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MirageSession extends PersistentSession {

    private MirageArenaSchematic vote;
    private Set<UUID> ignored = new HashSet<UUID>();
    private double dmgTaken = 0;

    protected MirageSession() {
        super(TimeUnit.MINUTES.toMillis(30));
    }

    public void vote(MirageArenaSchematic vote) {
        this.vote = vote;
    }

    public MirageArenaSchematic getVote() {
        return vote;
    }

    public boolean isIgnored(UUID player) {
        return ignored.contains(player);
    }

    public void ignore(UUID player) {
        ignored.add(player);
    }

    public void unignore(UUID player) {
        ignored.remove(player);
    }

    public double getDamage() {
        return dmgTaken;
    }

    public void resetDamage() {
        dmgTaken = 0;
    }

    public void addDamage(double amt) {
        dmgTaken += amt;
    }
}
