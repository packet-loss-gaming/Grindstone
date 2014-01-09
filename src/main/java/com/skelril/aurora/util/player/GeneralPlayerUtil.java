package com.skelril.aurora.util.player;

import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class GeneralPlayerUtil {

    /**
     * Make a player state
     *
     */
    public static PlayerState makeComplexState(Player player) {

        return new PlayerState(player.getName(),
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getLevel(),
                player.getExp());
    }

    /**
     * This method is used to hide a player
     *
     * @param player - The player to hide
     * @param to     - The player who can no longer see the player
     * @return - true if change occurred
     */
    public static boolean hide(Player player, Player to) {

        if (to.canSee(player)) {
            to.hidePlayer(player);
            return true;
        }
        return false;
    }

    /**
     * This method is used to show a player
     *
     * @param player - The player to show
     * @param to     - The player who can now see the player
     * @return - true if change occurred
     */
    public static boolean show(Player player, Player to) {

        if (!to.canSee(player)) {
            to.showPlayer(player);
            return true;
        }
        return false;
    }
}
