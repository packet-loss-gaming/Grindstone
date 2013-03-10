package com.skelril.aurora.jail;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Author: Turtle9598
 */
public interface InmateDatabase extends Iterable<Inmate> {

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
     * Checks if a player's name is jailed.
     *
     * @param name The name to check
     *
     * @return Whether name is jailed
     */
    public boolean isJailedName(String name);

    /**
     * Gets the jail message for a jailed name.
     *
     * @param name The name to check
     *
     * @return The jail message for the given name
     */
    public String getJailedNameMessage(String name);

    /**
     * Jails a player
     *
     * @param player
     * @param prison
     * @param source
     * @param reason
     * @param end
     */
    public void jail(Player player, String prison, CommandSender source, String reason, long end);

    /**
     * Jails a player by name
     *
     * @param name
     * @param prison
     * @param source
     * @param reason
     * @param end
     */
    public void jail(String name, String prison, CommandSender source, String reason, long end);

    /**
     * Unjails a player
     *
     * @param player
     * @param source
     * @param reason
     */
    public boolean unjail(Player player, CommandSender source, String reason);

    /**
     * Unjails a player by name
     *
     * @param name
     * @param source
     * @param reason
     */
    public boolean unjail(String name, CommandSender source, String reason);

    /**
     * Returns a Inmate with the given name
     *
     * @param name The name given to the ban.
     *
     * @return The applicable Inmate
     */
    public Inmate getJailedName(String name);

    /**
     * Returns a list of inmates
     *
     * @return A list of inmates
     */
    public List<Inmate> getInmatesList();
}