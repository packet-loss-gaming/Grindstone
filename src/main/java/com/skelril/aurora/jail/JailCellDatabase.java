/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.jail;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Author: Turtle9598
 */
public interface JailCellDatabase extends Iterable<JailCell> {

    /**
     * Load the ban database.
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
     * Unloads the database
     *
     * @return whether the operation was fully successful
     */
    public boolean unload();

    /**
     * Checks if a cell exist.
     *
     * @param prisonName The name of the prison which contains the cell
     * @param cellName   The name of the cell
     * @return Whether cell exist
     */
    public boolean cellExist(String prisonName, String cellName);

    /**
     * Jails a player
     *
     * @param prisonName
     * @param cellName
     * @param source
     * @param location
     */
    public void createJailCell(String prisonName, String cellName, CommandSender source, Location location);

    /**
     * Unjails a player by name
     *
     * @param prisonName
     * @param cellName
     * @param source
     */
    public boolean deleteJailCell(String prisonName, String cellName, CommandSender source);

    /**
     * Returns a JailCell with the given name
     *
     * @param prisonName The name of the prison which contains the cel
     * @param cellName   The name of the cell to retrieve
     * @return The applicable Inmate
     */
    public JailCell getJailCell(String prisonName, String cellName);

    /**
     * Checks if a prison exist.
     *
     * @param prisonName The name of the prison to check for
     * @return Whether the prison exist
     */
    public boolean prisonExist(String prisonName);

    /**
     * Returns an unmodifiable list of the cells for that prison
     *
     * @param prisonName The name of the prison to get
     * @return A map of the prison's cells
     */
    public List<JailCell> getPrison(String prisonName);

    /**
     * Returns an unmodifiable list of prison names
     *
     * @return A list of prison names
     */
    public List<String> getPrisons();

    /**
     * Returns an unmodifiable list of jail cells
     *
     * @return A list of jail cells
     */
    public List<JailCell> getJailCells();
}