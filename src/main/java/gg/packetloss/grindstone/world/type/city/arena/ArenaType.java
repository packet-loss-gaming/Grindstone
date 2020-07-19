/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena;


public enum ArenaType {

    /*
     * Used for Ever Changing Arenas
     */
    DYNMAIC,
    /*
     * Used for Arenas that store blocks
     */
    MONITORED,
    /*
     * Used for Arenas that are command triggered
     */
    COMMAND_TRIGGERED,
    /*
     * Generic Arenas
     */
    GENERIC
}
