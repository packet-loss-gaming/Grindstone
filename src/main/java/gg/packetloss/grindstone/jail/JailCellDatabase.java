/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.jail;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.List;


public interface JailCellDatabase extends Iterable<JailCell> {

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
     * Checks if a cell exist.
     *
     * @param prisonName The name of the prison which contains the cell
     * @param cellName   The name of the cell
     * @return Whether cell exist
     */
    boolean cellExist(String prisonName, String cellName);

    /**
     * Jails a player
     *
     * @param prisonName
     * @param cellName
     * @param source
     * @param location
     */
    void createJailCell(String prisonName, String cellName, CommandSender source, Location location);

    /**
     * Unjails a player by name
     *
     * @param prisonName
     * @param cellName
     * @param source
     */
    boolean deleteJailCell(String prisonName, String cellName, CommandSender source);

    /**
     * Returns a JailCell with the given name
     *
     * @param prisonName The name of the prison which contains the cel
     * @param cellName   The name of the cell to retrieve
     * @return The applicable Inmate
     */
    JailCell getJailCell(String prisonName, String cellName);

    /**
     * Checks if a prison exist.
     *
     * @param prisonName The name of the prison to check for
     * @return Whether the prison exist
     */
    boolean prisonExist(String prisonName);

    /**
     * Returns an unmodifiable list of the cells for that prison
     *
     * @param prisonName The name of the prison to get
     * @return A map of the prison's cells
     */
    List<JailCell> getPrison(String prisonName);

    /**
     * Returns an unmodifiable list of prison names
     *
     * @return A list of prison names
     */
    List<String> getPrisons();

    /**
     * Returns an unmodifiable list of jail cells
     *
     * @return A list of jail cells
     */
    List<JailCell> getJailCells();
}