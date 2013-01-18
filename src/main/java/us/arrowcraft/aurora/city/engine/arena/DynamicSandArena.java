package us.arrowcraft.aurora.city.engine.arena;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import us.arrowcraft.aurora.admin.AdminComponent;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.ItemUtil;
import us.arrowcraft.aurora.util.LocationUtil;
import us.arrowcraft.aurora.util.player.PlayerState;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class DynamicSandArena extends AbstractRegionedArena implements DynamicArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private static Economy economy = null;
    private int increaseRate;
    private int decreaseRate;
    private final HashMap<String, PlayerState> playerState = new HashMap<>();

    public DynamicSandArena(World world, ProtectedRegion region, int increaseRate, int decreaseRate,
                            AdminComponent adminComponent) {

        super(world, region);
        this.increaseRate = increaseRate;
        this.decreaseRate = decreaseRate;
        this.adminComponent = adminComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        setupEconomy();
    }

    @Override
    public void addBlocks() {

        if (!isEmpty()) {
            try {

                CuboidRegion dropArea = new CuboidRegion(getRegion().getMaximumPoint(), getRegion().getMinimumPoint());

                if (dropArea.getArea() > 75000) {
                    log.warning("The region: " + getRegion().getId() + " is too large.");
                    return;
                }

                com.sk89q.worldedit.Vector min = getRegion().getMinimumPoint();
                com.sk89q.worldedit.Vector max = getRegion().getMaximumPoint();

                int minX = min.getBlockX();
                int minZ = min.getBlockZ();
                int minY = min.getBlockY();
                int maxX = max.getBlockX();
                int maxZ = max.getBlockZ();
                int maxY = max.getBlockY();

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = maxY; y >= minY; --y) {
                            Block block = getWorld().getBlockAt(x, y, z);
                            Block topBlock = getWorld().getBlockAt(x, y + 1, z);

                            if (y == minY) {
                                block.setTypeIdAndData(BlockID.SAND, (byte) 0, false);
                            }

                            if (!(y + 1 > getWorld().getMaxHeight())
                                    && !(y + 1 > maxY)
                                    && block.getTypeId() != BlockID.AIR
                                    && topBlock.getTypeId() == BlockID.AIR
                                    && !LocationUtil.isCloseToPlayer(block, 4)) {
                                if (ChanceUtil.getChance(increaseRate)) {
                                    topBlock.setTypeIdAndData(BlockID.SAND, (byte) 0, false);
                                }
                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                log.warning("An error has occurred while attempting to drop sand in the region: "
                        + getRegion().getId() + ".");
            }
        }
    }

    @Override
    public void removeBlocks() {

        try {
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

                        if (!block.getChunk().isLoaded()) break;
                        if (!isEmpty()) {
                            if (y + minY < getWorld().getMaxHeight()
                                    && ChanceUtil.getChance(decreaseRate - (y * ChanceUtil.getRandom(5)))
                                    && !LocationUtil.isCloseToPlayer(block, 4)) {
                                block.setTypeIdAndData(BlockID.AIR, (byte) 0, false);
                            } else {
                                break;
                            }
                        } else {
                            if (y + minY < getWorld().getMaxHeight()
                                    && ChanceUtil.getChance((decreaseRate - (y * ChanceUtil.getRandom(5))) / 4)) {
                                block.setTypeIdAndData(BlockID.AIR, (byte) 0, false);
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warning("An error has occurred while attempting to remove sand in the region: "
                    + getRegion().getId() + ".");
        }
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        for (Player player : getContainedPlayers()) {
            try {
                adminComponent.standardizePlayer(player);
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.DYNMAIC;
    }

    @Override
    public void run() {

        equalize();
        addBlocks();
        removeBlocks();
    }

    @Override
    public void disable() {

        // No disable code
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamaged(EntityDamageEvent event) {

        Entity e = event.getEntity();

        if (!(e instanceof Player)
                || event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                || event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) return;

        damageCheck((Player) e, null, event.getDamage());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        Entity e = event.getEntity();

        if (!(e instanceof Player)) return;

        Entity d = event.getDamager();
        if (d instanceof Player) {
            damageCheck((Player) e, (Player) d, event.getDamage());
        } else if (d instanceof Arrow && ((Arrow) d).getShooter() instanceof Player) {
            damageCheck((Player) e, (Player) ((Arrow) d).getShooter(), event.getDamage());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (LocationUtil.isInRegion(getWorld(), getRegion().getParent(), player)
                || LocationUtil.isBelowPlayer(getWorld(), getRegion().getParent(), player)) {
            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepLevel(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();
        final Location fallBack = event.getRespawnLocation();

        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !adminComponent.isAdmin(player)) {

            try {
                PlayerState identity = playerState.get(player.getName());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                player.setLevel(identity.getLevel());
                player.setExp(identity.getExperience());

                Vector v = getRespawnLocation();
                event.setRespawnLocation(new Location(getWorld(), v.getX(), v.getY(), v.getZ()));
            } catch (Exception e) {
                e.printStackTrace();
                event.setRespawnLocation(fallBack);
            } finally {
                playerState.remove(player.getName());
            }
        }
    }

    private void damageCheck(final Player player, final Player attacker, final int damage) {

        if (attacker != null && !contains(attacker)) return;

        if (contains(player)) {

            final int orgDrop = damage * ChanceUtil.getRandom(3);
            int drop = orgDrop;

            if (!player.isDead()) {
                PlayerInventory pInventory = player.getInventory();
                int contained = ItemUtil.countItemsOfType(pInventory.getContents(), ItemID.GOLD_NUGGET);

                if (contained > drop) contained = drop;

                if (contained > 0) {
                    pInventory.removeItem(new ItemStack(ItemID.GOLD_NUGGET, contained));
                    drop -= contained;
                }
            }

            if (drop > 0) {
                if (!economy.has(player.getName(), drop)) {
                    Vector v = getRespawnLocation();
                    player.teleport(new Location(getWorld(), v.getX(), v.getY(), v.getZ()));
                    return;
                }
                economy.withdrawPlayer(player.getName(), drop);
                drop = 0;
            }

            for (short s = 0; s < orgDrop - drop; s++) {
                player.getLocation().getWorld().dropItemNaturally(player.getLocation(),
                        new ItemStack(ItemID.GOLD_NUGGET));
            }
        }
    }

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    private Vector getRespawnLocation() {

        Vector v;
        Vector min = getRegion().getParent().getMinimumPoint();
        Vector max = getRegion().getParent().getMaximumPoint();

        do {
            v = LocationUtil.pickLocation(min.getX(), max.getX(), min.getY(), max.getY(), min.getZ(), max.getZ());
        } while (getRegion().contains(v) || !isRespawnBlock(v) || getBlock(v).getTypeId() != 0);
        return v;
    }

    private boolean isRespawnBlock(Vector v) {

        int[] blocks = new int[2];
        blocks[0] = BlockID.WOOD;
        blocks[1] = BlockID.STONE_BRICK;

        for (int block : blocks) {
            if (block == getBlock(v.add(0, -1, 0)).getTypeId()) return true;
        }
        return false;
    }

    private Block getBlock(Vector v) {

        return getWorld().getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }
}
