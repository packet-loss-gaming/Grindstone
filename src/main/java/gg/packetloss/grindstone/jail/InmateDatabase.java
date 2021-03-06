/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.jail;

import java.util.List;
import java.util.UUID;


public interface InmateDatabase extends Iterable<Inmate> {

    /**
     * Load the ban database.
     *
     * @return whether the operation was fully successful
     */
    boolean load();

    /**
     * Save the database.
     *
     * @return whether the operation was fully successful
     */
    boolean save();

    /**
     * Unloads the database
     *
     * @return whether the operation was fully successful
     */
    boolean unload();

    /**
     * Checks if a player's name is jailed.
     *
     * @param ID The ID to check
     * @return Whether name is jailed
     */
    boolean isInmate(UUID ID);

    /**
     * Returns a Inmate with the given ID
     *
     * @param ID The ID of the jailed player
     * @return The applicable Inmate
     */
    Inmate getInmate(UUID ID);

    /**
     * Jails a player by ID
     *
     * @param ID
     * @param prison
     * @param source
     * @param reason
     * @param end
     * @param mute
     */
    void jail(UUID ID, String prison, String source, String reason, long end, boolean mute);

    /**
     * Unjails a player by ID
     *
     * @param ID
     * @param source
     * @param reason
     */
    boolean unjail(UUID ID, String source, String reason);

    /**
     * Returns a list of inmates
     *
     * @return A list of inmates
     */
    List<Inmate> getInmatesList();
}