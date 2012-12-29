package us.arrowcraft.aurora.prayer.PrayerFX;
import com.sk89q.worldedit.blocks.BlockID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Author: Turtle9598
 */
public class InventoryFX extends AbstractPrayer {

    @Override
    public void add(Player player) {

        player.setItemInHand(new ItemStack(BlockID.DIRT, 1));
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
