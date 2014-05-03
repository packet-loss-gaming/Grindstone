/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.SandArena;

import com.skelril.aurora.city.engine.area.AreaListener;
import com.skelril.aurora.events.apocalypse.GemOfLifeUsageEvent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.Disarm;
import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SandArenaListener extends AreaListener<SandArena> {
    public SandArenaListener(SandArena parent) {
        super(parent);
    }

    private static Set<Class> blacklistedSpecs = new HashSet<>();

    static {
        blacklistedSpecs.add(Disarm.class);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {

        SpecialAttack attack = event.getSpec();

        if (!parent.contains(attack.getLocation())) return;

        if (blacklistedSpecs.contains(attack.getClass())) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {

        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        HashMap<String, PlayerState> playerState = parent.playerState;

        Player player = event.getEntity();
        if (!playerState.containsKey(player.getName()) && parent.contains(player, 1) && !parent.admin.isAdmin(player)) {

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

        HashMap<String, PlayerState> playerState = parent.playerState;

        Player player = event.getPlayer();
        final Location fallBack = event.getRespawnLocation();

        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !parent.admin.isAdmin(player)) {

            try {
                PlayerState identity = playerState.get(player.getName());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                player.setLevel(identity.getLevel());
                player.setExp(identity.getExperience());

                event.setRespawnLocation(parent.getRespawnLocation());
            } catch (Exception e) {
                e.printStackTrace();
                event.setRespawnLocation(fallBack);
            } finally {
                playerState.remove(player.getName());
            }
        }
    }
}
