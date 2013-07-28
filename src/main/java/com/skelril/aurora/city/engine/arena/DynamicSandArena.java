package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.apocalypse.GemOfLifeUsageEvent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.database.InventoryAuditLogger;
import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class DynamicSandArena extends AbstractRegionedArena implements DynamicArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

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
                log.warning("An error has occurred while attempting to drop sand in the region: " + getRegion().getId() + ".");
                e.printStackTrace();
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
            log.warning("An error has occurred while attempting to remove sand in the region: " + getRegion().getId() + ".");
            e.printStackTrace();
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

        InventoryAuditLogger auditor = adminComponent.getInventoryDumpLogger();
        for (PlayerState state : playerState.values()) {
            String ownerName = state.getOwnerName();
            for (ItemStack stack : state.getArmourContents()) {
                auditor.log(ownerName, stack);
            }
            for (ItemStack stack : state.getInventoryContents()) {
                auditor.log(ownerName, stack);
            }
        }
    }

    private static Set<SpecialAttackEvent.Specs> blacklistedSpecs = new HashSet<>();

    static {
        blacklistedSpecs.add(SpecialAttackEvent.Specs.DISARM);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {

        if (!contains(event.getLocation())) return;

        if (blacklistedSpecs.contains(event.getSpec())) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {

        if (contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /*
    @EventHandler(ignoreCancelled = true)
    public void onBowFire(EntityShootBowEvent event) {

        final Entity entity = event.getEntity();

        if (contains(entity) && entity instanceof Player) {

            final ItemStack weapon = ((Player) entity).getItemInHand().clone();

            server.getScheduler().runTaskLater(inst, new Runnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    ((Player) entity).getInventory().setItemInHand(weapon);
                    ((Player) entity).updateInventory();
                }
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {

        Entity attacker = event.getDamager();
        final Entity defender = event.getEntity();

        if (contains(attacker) && contains(defender)) {
            if (attacker instanceof Player) {

                final ItemStack weapon = ((Player) attacker).getItemInHand().clone();
                final Entity finalAttacker = attacker;

                server.getScheduler().runTaskLater(inst, new Runnable() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void run() {
                        ((Player) finalAttacker).getInventory().setItemInHand(weapon);
                        ((Player) finalAttacker).updateInventory();
                    }
                }, 1);
            }

            if (defender instanceof Player) {
                final ItemStack[] armor = ItemUtil.clone(((Player) defender).getInventory().getArmorContents());

                server.getScheduler().runTaskLater(inst, new Runnable() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void run() {
                        ((Player) defender).getInventory().setArmorContents(armor);
                        ((Player) defender).updateInventory();
                    }
                }, 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {

        final Entity defender = event.getEntity();

        if (contains(defender) && !(event instanceof EntityDamageByEntityEvent)) {
            if (defender instanceof Player) {
                final ItemStack[] armor = ItemUtil.clone(((Player) defender).getInventory().getArmorContents());

                server.getScheduler().runTaskLater(inst, new Runnable() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void run() {
                        ((Player) defender).getInventory().setArmorContents(armor);
                        ((Player) defender).updateInventory();
                    }
                }, 1);
            }
        }
    }
    */

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (!playerState.containsKey(player.getName()) && contains(player, 1) && !adminComponent.isAdmin(player)) {

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

    @EventHandler(priority = EventPriority.HIGHEST)
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
