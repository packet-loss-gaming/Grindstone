package us.arrowcraft.aurora.jail;
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
     * Checks if a player's name is jailed.
     *
     * @param name The name to check
     *
     * @return Whether name is jailed
     */
    public boolean cellExist(String name);

    /**
     * Jails a player
     *
     * @param jailCellName
     * @param location
     */
    public void createJailCell(String jailCellName, String prisonName, CommandSender source, Location location);

    /**
     * Unjails a player by name
     *
     * @param jailCellName
     */
    public boolean deleteJailCell(String jailCellName, CommandSender source);

    /**
     * Returns a jailcell with the given name
     *
     * @param name The name given to the ban.
     *
     * @return The applicable Inmate
     */
    public JailCell getJailCell(String name);

    /**
     * Returns a list of jail cells
     *
     * @return A list of jail cells
     */
    public List<JailCell> getJailCells();
}