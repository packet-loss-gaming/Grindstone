package us.arrowcraft.aurora.city.engine.arena;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import us.arrowcraft.aurora.SacrificeComponent;
import us.arrowcraft.aurora.events.DropClearPulseEvent;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.LocationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class DropPartyArena extends AbstractRegionedArena implements CommandTriggeredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private SacrificeComponent sacrificeComponent;

    private List<ItemStack> drops;
    private BukkitTask task = null;
    private long lastDropPulse = 0;

    public DropPartyArena(World world, ProtectedRegion region, SacrificeComponent sacrificeComponent) {

        super(world, region);
        this.sacrificeComponent = sacrificeComponent;
        drops = new ArrayList<>();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void run() {

        drop(true);
    }

    @Override
    public void disable() {

        // No disabling code
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {
        // Nothing to do
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.COMMAND_TRIGGERED;
    }

    private static final ItemStack[] sacrifices = new ItemStack[] {
            new ItemStack(ItemID.BLAZE_ROD, 64)
    };

    private void drop(final boolean populate) {

        Bukkit.broadcastMessage(ChatColor.GOLD + "Drop party in 60 seconds!");

        if (populate) {
            for (int i = 0; i < server.getOnlinePlayers().length * 12; i++) {
                drops.addAll(sacrificeComponent.getCalculatedLoot(server.getConsoleSender(), sacrifices));
            }
        }

        if (task != null) server.getScheduler().cancelTask(task.getTaskId());

        task = server.getScheduler().runTaskTimer(inst, new Runnable() {

            @Override
            public void run() {

                if (lastDropPulse != 0 && System.currentTimeMillis() - lastDropPulse < TimeUnit.SECONDS.toMillis(5)) {
                    ChatUtil.sendNotice(getContainedPlayers(1), "Drop Party temporarily suspended for: Drop Clear.");
                    return;
                }

                List<ItemStack> dropped = new ArrayList<>();

                Vector min = getRegion().getMinimumPoint();
                Vector max = getRegion().getMaximumPoint();

                // Remove null drops and shuffle all other drops
                drops.removeAll(Collections.singleton(null));
                Collections.shuffle(drops);

                for (ItemStack drop : drops) {

                    if (dropped.size() > 10) break;

                    Vector v = LocationUtil.pickLocation(min.getX(), max.getX(), min.getZ(), max.getZ());
                    Location l = new Location(getWorld(), v.getX(), v.getY(), v.getZ()).add(0, max.getBlockY(), 0);
                    if (!getWorld().getChunkAt(l).isLoaded()) getWorld().getChunkAt(l).load(true);
                    getWorld().dropItem(l, drop);
                    dropped.add(drop);

                    if (populate) {
                        // Throw in some xp cause why not
                        for (short s = (short) ChanceUtil.getRandom(5); s > 0; s--) {
                            ExperienceOrb e = (ExperienceOrb) getWorld().spawnEntity(l, EntityType.EXPERIENCE_ORB);
                            e.setExperience(8);
                        }
                    }
                }

                for (ItemStack drop : dropped) {
                    if (drops.contains(drop)) drops.remove(drop);
                }

                if (drops.size() < 1) {
                    server.getScheduler().cancelTask(task.getTaskId());
                    task = null;
                }
            }
        }, 20 * 60, 20 * 3);
    }

    @EventHandler
    public void onDropClearPulse(DropClearPulseEvent event) {

        if (task != null) lastDropPulse = System.currentTimeMillis();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && block.getTypeId() == BlockID.STONE_BUTTON
                && getRegion().getParent().contains(new Vector(block.getX(), block.getY(), block.getZ()).floor())) {

            Player player = event.getPlayer();

            if (task != null) {

                ChatUtil.sendError(player, "There is already a drop party in progress!");
                return;
            }

            // Scan for, and absorb chest contents
            Vector min = getRegion().getParent().getMinimumPoint();
            Vector max = getRegion().getParent().getMaximumPoint();

            int minX = min.getBlockX();
            int minY = min.getBlockY();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxY = max.getBlockY();
            int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        BlockState chest = getWorld().getBlockAt(x, y, z).getState();

                        if (chest instanceof Chest) {

                            Inventory chestInventory = ((Chest) chest).getInventory();
                            Collections.addAll(drops, chestInventory.getContents());

                            chestInventory.clear();
                            chest.update(true);
                        }
                    }
                }
            }

            drop(false);
        }
    }
}
