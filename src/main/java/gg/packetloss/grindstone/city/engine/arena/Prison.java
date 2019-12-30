/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.PrayerApplicationEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class Prison extends AbstractRegionedArena implements GenericArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

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
                    if (block.getType() == Material.CHEST) {
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

        for (Player player : server.getOnlinePlayers()) {
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
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (event.isFlying() && contains(player) && !adminComponent.isAdmin(player)) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, "You cannot fly here!");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (event.getCause().getEffect().getType().isHoly() && contains(event.getPlayer())) {
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
    public void onPlayerInteractEvent(PlayerInteractEvent event) {

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        BlockState state = event.getClickedBlock().getLocation().getBlock().getState();
        if (state.getType() == Material.CHEST && rewardChest.equals(state.getLocation())) {

            List<ItemStack> loot = SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 10, 4000);
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
