/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.PrayerTriggerEvent;
import gg.packetloss.grindstone.events.entity.EntitySpawnBlockedEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Prison extends AbstractRegionedArena implements GenericArena, Listener {
    private AdminComponent adminComponent;
    private GuildComponent guildComponent;
    private PlayerStateComponent playerStateComponent;

    private ProtectedRegion office;

    // Block - Is unlocked
    private Location rewardChest;
    private Location entranceLoc;

    public Prison(World world, ProtectedRegion[] regions,
                  AdminComponent adminComponent, GuildComponent guildComponent,
                  PlayerStateComponent playerStateComponent) {

        super(world, regions[0]);

        this.office = regions[1];

        this.adminComponent = adminComponent;
        this.guildComponent = guildComponent;
        this.playerStateComponent = playerStateComponent;

        findRewardChest();     // Setup office
        entranceLoc = new Location(getWorld(), 256.18, 81, 136);

        CommandBook.registerEvents(this);
        CommandBook.registerEvents(new FlightBlockingListener(adminComponent, this::contains));
    }

    private void findRewardChest() {
        RegionWalker.testWalk(office, (x, y, z) -> {
            BlockState block = getWorld().getBlockAt(x, y, z).getState();
            if (block.getType() == Material.CHEST) {
                rewardChest = block.getLocation();
                return true;
            }

            return false;
        });
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

    public boolean isGuildBlocked(Player player) {
        return playerStateComponent.hasValidStoredState(PlayerStateKind.PUZZLE_PRISON, player);
    }

    private void addPlayer(Player player) {
        try {
            playerStateComponent.pushState(PlayerStateKind.PUZZLE_PRISON, player);

            guildComponent.getState(player).ifPresent(GuildState::disablePowers);
        } catch (IOException | ConflictingPlayerStateException ex) {
            ex.printStackTrace();
        }
    }

    private void removePlayer(Player player) {
        try {
            playerStateComponent.popState(PlayerStateKind.PUZZLE_PRISON, player);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void equalize() {
        Collection<Player> allPlayers = getContained(Player.class);

        for (Player player : allPlayers) {
            if (!isGuildBlocked(player)) {
                addPlayer(player);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isGuildBlocked(player) && !allPlayers.contains(player)) {
                removePlayer(player);
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
        if (accepted.contains(event.getCause())) {
            return;
        }

        Player player = event.getPlayer();

        if (adminComponent.isAdmin(player)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (contains(from) && contains(to)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(event.getPlayer(), "You cannot teleport to another location in the prison.");
        } else if (contains(to)) {
            event.setTo(entranceLoc);
            ChatUtil.sendWarning(event.getPlayer(), "The warden catches you and throws you outside!");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerTriggerEvent event) {

        if (event.getPrayer().isHoly() && contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuildEnable(GuildPowersEnableEvent event) {
        if (isGuildBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawnBlocked(EntitySpawnBlockedEvent event) {
        Entity entity = event.getEntity();

        if (!contains(entity)) {
            return;
        }

        if (entity instanceof Enderman) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        BlockState state = event.getClickedBlock().getLocation().getBlock().getState();
        if (state.getType() == Material.CHEST && rewardChest.equals(state.getLocation())) {

            List<ItemStack> loot = SacrificeComponent.getCalculatedLoot(Bukkit.getConsoleSender(), 10, 4000);
            int lootSplit = ChanceUtil.getRangedRandom(64 * 2, 64 * 4);
            if (ChanceUtil.getChance(135)) lootSplit *= 10;
            else if (ChanceUtil.getChance(65)) lootSplit *= 2;

            event.setUseInteractedBlock(Event.Result.DENY);
            event.getPlayer().getInventory().addItem(new ItemStack(Material.GOLD_INGOT, lootSplit));
            event.getPlayer().getInventory().addItem(loot.toArray(new ItemStack[0]));

            event.getPlayer().teleport(entranceLoc);
            ChatUtil.sendNotice(event.getPlayer(), "You have successfully raided the jail!");

            //noinspection deprecation
            event.getPlayer().updateInventory();
        }
    }
}
