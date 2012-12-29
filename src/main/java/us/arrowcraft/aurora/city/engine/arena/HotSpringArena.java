package us.arrowcraft.aurora.city.engine.arena;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Effect;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.admin.AdminComponent;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class HotSpringArena extends AbstractRegionedArena implements GenericArena {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    public HotSpringArena(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;
    }

    @Override
    public void run() {

        smoke();
        effect();
    }

    @Override
    public void disable() {

        // No disable code
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        // Do nothing
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.GENERIC;
    }

    public void smoke() {

        try {
            if (!isEmpty()) {

                com.sk89q.worldedit.Vector min = getRegion().getMinimumPoint();
                com.sk89q.worldedit.Vector max = getRegion().getMaximumPoint();

                int minX = min.getBlockX();
                int minY = min.getBlockY();
                int minZ = min.getBlockZ();
                int maxX = max.getBlockX();
                int maxZ = max.getBlockZ();

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        int sizeY = getWorld().getHighestBlockYAt(x, z) - minY;

                        for (int y = sizeY; y > 0; y--) {
                            Block block = getWorld().getBlockAt(x, y + minY, z);

                            if (EnvironmentUtil.isWater(block) && ChanceUtil.getChance(200)) {
                                getWorld().playEffect(block.getLocation(), Effect.ENDER_SIGNAL, 1);
                                if (getWorld().isThundering() && ChanceUtil.getChance(50))
                                    getWorld().spawnEntity(block.getLocation(), EntityType.ZOMBIE);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

            log.warning("The region: " + getId() + " does not exists in the world: " + getWorld().getName() + ".");
        }
    }

    public void effect() {

        for (Player player : getContainedPlayers()) {
            try {

                player.removePotionEffect(PotionEffectType.CONFUSION);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.WEAKNESS);
                player.removePotionEffect(PotionEffectType.POISON);
                player.removePotionEffect(PotionEffectType.SLOW);

                player.removePotionEffect(PotionEffectType.REGENERATION);
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.JUMP);

                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 300, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 180, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 180, 1));
            } catch (Exception e) {

                log.warning("The player: " + player.getName() + " was not boosted by the hot spring: " + getId() + ".");
            }
        }
    }
}
