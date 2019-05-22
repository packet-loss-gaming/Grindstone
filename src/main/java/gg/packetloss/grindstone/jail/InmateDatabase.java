/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.jail;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
   * Jails a player
   */
  void jail(Player player, String prison, CommandSender source, String reason, long end, boolean mute);

  /**
   * Jails a player by ID
   */
  void jail(UUID ID, String prison, CommandSender source, String reason, long end, boolean mute);

  /**
   * Unjails a player
   */
  boolean unjail(Player player, CommandSender source, String reason);

  /**
   * Unjails a player by ID
   */
  boolean unjail(UUID ID, CommandSender source, String reason);

  /**
   * Returns a list of inmates
   *
   * @return A list of inmates
   */
  List<Inmate> getInmatesList();
}