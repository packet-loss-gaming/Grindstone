/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena;


public abstract interface GenericArena extends Runnable {

    public void run();

    public void disable();

    public String getId();

    public void equalize();

    public ArenaType getArenaType();

}
