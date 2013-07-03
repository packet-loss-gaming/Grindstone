package com.skelril.aurora.guilds;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public abstract class AbstractGuild {

    protected final CommandBook inst = CommandBook.inst();
    protected final Logger log = inst.getLogger();
    protected final Server server = CommandBook.server();

    private final String groupName;                                                 // Group Name
    private final HashMap<Player, List<Character>> activePlayers = new HashMap<>(); // Flags

    public AbstractGuild(String groupName) {

        this.groupName = groupName;
    }

    /**
     * This method is used to get the name of the group.
     *
     * @return - The group's name
     */
    public String getGroupName() {

        return groupName;
    }

    /**
     * This method is used to get the group permission string.
     *
     * @return - The permission string
     */
    public String getGroupPermissionString() {

        return "aurora.guild." + groupName;
    }

    /**
     * This method is used to get all members of the guild that are online.
     *
     * @return - The online players
     */
    public Player[] getOnlineMembers() {

        List<Player> returnedList = new ArrayList<>();

        for (Player player : server.getOnlinePlayers()) {

            if (inst.hasPermission(player, getGroupPermissionString())) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    public Player[] getOnlineNonmembers() {

        List<Player> returnedList = new ArrayList<>();

        for (Player player : server.getOnlinePlayers()) {

            if (!inst.hasPermission(player, getGroupPermissionString())) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    public Player[] getActivePlayers() {

        return activePlayers.keySet().toArray(new Player[activePlayers.keySet().size()]);
    }

    public Player[] getInactivePlayers() {

        List<Player> returnedList = new ArrayList<>();

        for (Player player : server.getOnlinePlayers()) {

            if (!activePlayers.containsKey(player)) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    /**
     * This method returns all active flags for the player.
     *
     * @param player - The player who's flags you would like to receive
     * @return - null if the player has no flags, otherwise a flag list
     */
    public Character[] getActiveFlags(Player player) {

        if (!activePlayers.containsKey(player)) return null;
        return activePlayers.get(player).toArray(new Character[activePlayers.get(player).size()]);
    }

    public boolean hasFlag(Player player, char flag) {

        Character[] flags = getActiveFlags(player);
        if (flags == null) return false;
        for (char aFlag : flags) {
            if (aFlag == flag) return true;
        }
        return false;
    }

    /**
     * This method is used to determine whether a player is in the guild.
     *
     * @param player - The player to check
     * @return - true if the player is a member of this guild
     */
    public boolean isInGuild(Player player) {

        return inst.hasPermission(player, getGroupPermissionString());
    }

    /**
     * This method is used to determine whether a player is in the guild and active.
     *
     * @param player - The player to check
     * @return - true if the player is a member of this guild
     */
    public boolean isActive(Player player) {

        for (Player aPlayer : getActivePlayers()) {
            if (aPlayer.equals(player)) return true;
        }
        return false;
    }

    /**
     * This method is used to add the player to the guild.
     *
     * @param player - The player to add
     * @return - true if successfully added
     */
    public boolean addToGuild(Player player) {

        if (inst.hasPermission(player, getGroupPermissionString())) return false;
        // TODO Addition Code
        return false;
    }

    /**
     * This method is used to remove the player from the guild.
     *
     * @param player - The player to remove
     * @return - true if successfully removed
     */
    public boolean removeFromGuild(Player player) {

        if (!inst.hasPermission(player, getGroupPermissionString())) return false;
        // TODO Removal Code
        return false;
    }

    /**
     * This method is used to active a player's guild
     *
     * @param player - The player to activate
     * @return - true if the player was activated
     */
    public boolean activate(Player player, Character[] flags) {

        if (!activePlayers.containsKey(player)) {
            List<Character> flagList = new ArrayList<>();
            Collections.addAll(flagList, flags);
            activePlayers.put(player, flagList);
            return true;
        }
        return false;
    }

    public boolean deactivate(Player player) {

        if (activePlayers.containsKey(player)) {
            activePlayers.remove(player);
            return true;
        }
        return false;
    }
}
