/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.events.entity.item.DropClearPulseEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.TimeUtil;
import com.skelril.aurora.util.item.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

    private List<ItemStack> drops;
    private BukkitTask task = null;
    private long lastDropPulse = 0;
    private double dropPartyPulses = 0;

    public DropPartyArena(World world, ProtectedRegion region) {

        super(world, region);
        drops = new ArrayList<>();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        long ticks = TimeUtil.getTicksTill(20, 7);
        server.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, ticks, 20 * 60 * 60 * 24 * 7);
    }

    @Override
    public void run() {

        drop(ChanceUtil.getRangedRandom(1460, 5836));
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

    public void drop(int populatorValue) {

        drop(populatorValue, 24);
    }

    public void drop(int populatorValue, int modifier) {

        // Check for online players
        final int playerCount = server.getOnlinePlayers().length;

        if (playerCount < 1) return;

        // Notify online players
        Bukkit.broadcastMessage(ChatColor.GOLD + "Drop party in 60 seconds!");

        // Setup region variable
        final CuboidRegion rg = new CuboidRegion(getRegion().getMinimumPoint(), getRegion().getMaximumPoint());

        // Use the SacrificeComponent to populate the drop party if a populator value is given
        final boolean populate = populatorValue > 0;
        if (populate) {
            for (int i = 0; i < playerCount * modifier; i++) {
                drops.addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 64, populatorValue));
            }
            drops.add(ItemUtil.makeSkull(server.getOnlinePlayers()[ChanceUtil.getRandom(playerCount) - 1].getName()));
        }

        // Remove null drops and shuffle all other drops
        drops.removeAll(Collections.singleton(null));
        Collections.shuffle(drops);

        // Set a maximum drop count
        dropPartyPulses = drops.size() * .15;

        if (task != null) task.cancel();

        task = server.getScheduler().runTaskTimer(inst, new Runnable() {

            @Override
            public void run() {

                if (lastDropPulse != 0 && System.currentTimeMillis() - lastDropPulse < TimeUnit.SECONDS.toMillis(3)) {
                    return;
                }

                Iterator<ItemStack> it = drops.iterator();

                for (int k = 10; it.hasNext() && k > 0; k--) {

                    // Pick a random Location
                    Location l = LocationUtil.pickLocation(getWorld(), rg.getMaximumY(), rg);
                    if (!getWorld().getChunkAt(l).isLoaded()) getWorld().getChunkAt(l).load(true);
                    getWorld().dropItem(l, it.next());

                    // Remove the drop
                    it.remove();

                    // Drop the xp
                    if (populate) {
                        // Throw in some xp cause why not
                        for (short s = (short) ChanceUtil.getRandom(5); s > 0; s--) {
                            ExperienceOrb e = (ExperienceOrb) getWorld().spawnEntity(l, EntityType.EXPERIENCE_ORB);
                            e.setExperience(8);
                        }
                    }
                }

                // Cancel if we've ran out of drop party pulses or if there is nothing more to drop
                if (drops.size() < 1 || dropPartyPulses <= 0) {
                    task.cancel();
                    task = null;
                }

                dropPartyPulses--;
            }
        }, 20 * 60, 20 * 3);
    }

    @EventHandler
    public void onDropClearPulse(DropClearPulseEvent event) {

        if (task != null) {
            lastDropPulse = System.currentTimeMillis();
            ChatUtil.sendNotice(getContainedPlayers(1), "Drop Party temporarily suspended for: Drop Clear.");
            for (Entity entity : getContainedEntities(1, Item.class, ExperienceOrb.class)) {
                if (entity instanceof Item) {
                    drops.add(((Item) entity).getItemStack());
                }
                entity.remove();
            }
        }
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

                            Inventory chestInventory = ((Chest) chest).getBlockInventory();
                            Collections.addAll(drops, chestInventory.getContents());

                            chestInventory.clear();
                            chest.update(true);
                        }
                    }
                }
            }

            drop(0);
        }
    }
}
