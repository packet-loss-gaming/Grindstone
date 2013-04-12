package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Prison extends AbstractRegionedArena implements GenericArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private ProtectedRegion office;

    // Block - Is unlocked
    private Location rewardChest;

    public Prison(World world, ProtectedRegion region, ProtectedRegion office, AdminComponent adminComponent) {

        super(world, region);
        this.office = office;
        this.adminComponent = adminComponent;

        findRewardChest();     // Setup office

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private void findRewardChest() {

        com.sk89q.worldedit.Vector min = office.getMinimumPoint();
        com.sk89q.worldedit.Vector max = office.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        BlockState block;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; --y) {
                    block = getWorld().getBlockAt(x, y, z).getState();
                    if (!block.getChunk().isLoaded()) block.getChunk().load();
                    if (block.getTypeId() == BlockID.CHEST) {
                        rewardChest = block.getLocation();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void run() {

        equalize();
    }

    @Override
    public void disable() {

        // Nothing to do here
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

        return ArenaType.MONITORED;
    }

    private static List<PlayerTeleportEvent.TeleportCause> accepted = new ArrayList<>();

    static {
        accepted.add(PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        if (contains(event.getTo()) && !accepted.contains(event.getCause())) {
            event.setCancelled(true);
            ChatUtil.sendWarning(event.getPlayer(), "You cannot teleport to that location.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (event.getCause().getEffect().getType().isHoly() && contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        BlockState state = event.getClickedBlock().getLocation().getBlock().getState();
        if (state.getTypeId() == BlockID.CHEST && rewardChest.equals(state.getLocation())) {

            List<ItemStack> loot = SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 10, 2000);
            int lootSplit = ChanceUtil.getRangedRandom(64 * 2, 64 * 4);
            if (ChanceUtil.getChance(135)) lootSplit *= 10;
            else if (ChanceUtil.getChance(65)) lootSplit *= 2;

            event.setUseInteractedBlock(Event.Result.DENY);
            event.getPlayer().getInventory().addItem(new ItemStack(ItemID.GOLD_BAR, lootSplit));
            event.getPlayer().getInventory().addItem(loot.toArray(new ItemStack[loot.size()]));

            event.getPlayer().teleport(new Location(getWorld(), 256.18, 81, 136));
            ChatUtil.sendNotice(event.getPlayer(), "You have successfully raided the jail!");

            //noinspection deprecation
            event.getPlayer().updateInventory();
        }
    }
}
