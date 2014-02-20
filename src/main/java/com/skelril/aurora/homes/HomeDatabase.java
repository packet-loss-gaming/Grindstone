/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.homes;

import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public interface HomeDatabase {

    /**
     * Load the home database.
     *
     * @return whether the operation was fully successful
     */
    public boolean load();

    /**
     * Save the database.
     *
     * @return whether the operation was fully successful
     */
    public boolean save();

    /**
     * Checks if a player has a house
     *
     * @param name The name to check
     * @return Whether the player has a house
     */
    public boolean houseExist(String name);

    /**
     * Jails a player
     *
     * @param player
     * @param world
     * @param x
     * @param y
     * @param z
     */
    public void saveHouse(Player player, String world, int x, int y, int z);

    /**
     * Unjails a player by name
     *
     * @param player
     */
    public boolean deleteHouse(String player);

    /**
     * Returns the home with the given name
     *
     * @param name The name given to the ban.
     * @return The applicable player
     */
    public Home getHouse(String name);
}
