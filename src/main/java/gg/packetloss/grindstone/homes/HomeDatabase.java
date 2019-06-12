/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.homes;

import org.bukkit.entity.Player;

import java.util.UUID;

@Deprecated
public interface HomeDatabase {

    /**
     * Load the home database.
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
     * Checks if a player has a house
     *
     * @param playerID The playerID who's house to find
     * @return Whether the player has a house
     */
    boolean houseExist(UUID playerID);

    /**
     * Add a house for a player
     *
     * @param player the player who's house to add
     * @param world the world the house is in
     * @param x the house's x loc
     * @param y the house's y loc
     * @param z the house's z loc
     */
    void saveHouse(Player player, String world, int x, int y, int z);

    /**
     * Returns a player's house
     *
     * @param playerID The playerID who's house to delete
     */
    boolean deleteHouse(UUID playerID);

    /**
     * Returns the home with the given name
     *
     * @param playerID The playerID who's house to find
     * @return The applicable player
     */
    Home getHouse(UUID playerID);
}
