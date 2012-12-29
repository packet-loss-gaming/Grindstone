package us.arrowcraft.aurora.util.player;
import org.bukkit.entity.Player;

/**
 * Author: Turtle9598
 */
public class GeneralPlayerUtil {

    /**
     * This method is used to hide a player
     *
     * @param player - The player to hide
     * @param to     - The player who can no longer see the player
     *
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
     *
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
