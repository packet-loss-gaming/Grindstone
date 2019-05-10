/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

public class ArenaVote {
    private String arena;
    private int votes;

    public ArenaVote(String arena) {
        this.arena = arena;
    }

    public String getArena() {
        return arena;
    }

    public void addVote() {
        ++votes;
    }

    public int getVotes() {
        return votes;
    }
}
