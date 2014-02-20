/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.environment.FrostBiteEvent;
import com.skelril.aurora.util.LocationUtil;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class SnowSpleefArena extends AbstractRegionedArena implements SpleefArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    public SnowSpleefArena(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void restoreFloor() {

        try {
            if (LocationUtil.getPlayersStandingOnRegion(getWorld(), getRegion()).size() > 1
                    || !LocationUtil.containsPlayer(getWorld(), getRegion().getParent())) return;

            CuboidRegion snow = new CuboidRegion(getRegion().getMaximumPoint(), getRegion().getMinimumPoint());

            if (snow.getArea() > 8208 || snow.getHeight() > 1) {
                log.warning("The region: " + getRegion().getId() + " is too large.");
                return;
            }

            for (BlockVector bv : snow) {
                Block b = getWorld().getBlockAt(bv.getBlockX(), bv.getBlockY(), bv.getBlockZ());
                if (b.getTypeId() != BlockID.SNOW_BLOCK) {
                    b.setTypeIdAndData(BlockID.SNOW_BLOCK, (byte) 0, false);
                }
            }
        } catch (Exception e) {
            log.warning("An error has occurred while trying to create snow in the region: "
                    + getRegion().getId() + ".");
        }
    }

    @Override
    public void feed() {

        for (Player player : getWorld().getPlayers()) {
            try {
                if (LocationUtil.isBelowPlayer(getWorld(), getRegion(), player) && player.getFoodLevel() < 8) {
                    player.setFoodLevel(8);
                }
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        if (LocationUtil.isBelowPlayer(getWorld(), getRegion())) {
            for (Player player : getWorld().getPlayers()) {
                try {
                    if (LocationUtil.isBelowPlayer(getWorld(), getRegion(), player)) {
                        adminComponent.standardizePlayer(player);
                    }
                } catch (Exception e) {
                    log.warning("The player: " + player.getName() + " may have an unfair advantage.");
                }
            }
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.GENERIC;
    }

    @Override
    public void run() {

        equalize();
        feed();
        restoreFloor();
    }

    @Override
    public void disable() {

        // No disable code
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {

        Block block = event.getBlock();

        if (contains(block) && block.getTypeId() == BlockID.SNOW_BLOCK) {
            block.breakNaturally(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrostBite(FrostBiteEvent event) {

        if (contains(event.getPlayer(), 1)) event.setCancelled(true);
    }
}
