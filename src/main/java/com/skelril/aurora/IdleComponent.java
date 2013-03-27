package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.GodComponent;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.admin.AdminState;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "AFK Checker", desc = "AFK Checking.")
@Depend(components = {GodComponent.class, AdminState.class})
public class IdleComponent extends BukkitComponent implements Runnable, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    AdminComponent adminComponent;

    @InjectComponent
    GodComponent godComponent;

    private ConcurrentHashMap<Player, Long> afk = new ConcurrentHashMap<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().runTaskTimer(inst, this, 20 * 60, 20 * 10);
    }

    @Override
    public void run() {

        for (final Map.Entry<Player, Long> entry : afk.entrySet()) {

            if (System.currentTimeMillis() - entry.getValue() >= TimeUnit.MINUTES.toMillis(3)) {

                if (System.currentTimeMillis() - entry.getValue() >= TimeUnit.MINUTES.toMillis(60)) {
                    LocationUtil.toGround(entry.getKey());
                    adminComponent.deadmin(entry.getKey(), true);
                    entry.getKey().setSleepingIgnored(false);

                    server.getScheduler().runTaskLater(inst, new Runnable() {

                        @Override
                        public void run() {

                            entry.getKey().kickPlayer("Inactivity - 60 Minutes");
                        }
                    }, 1);
                } else if (!entry.getKey().isSleepingIgnored()) {
                    String name = entry.getKey().getName();
                    entry.getKey().setPlayerListName(ChatColor.GRAY + name.substring(0, Math.min(14, name.length())));
                    entry.getKey().setSleepingIgnored(true);
                    godComponent.enableGodMode(entry.getKey());
                    ChatUtil.sendNotice(entry.getKey(), "You are now marked as AFK.");
                }

            } else if (entry.getKey().isSleepingIgnored()) {

                entry.getKey().setPlayerListName(entry.getKey().getName());
                entry.getKey().setSleepingIgnored(false);
                if (!adminComponent.isAdmin(entry.getKey())) godComponent.disableGodMode(entry.getKey());
                ChatUtil.sendNotice(entry.getKey(), "You are no longer marked as AFK.");
            }
        }
    }

    @EventHandler
    public void onEntityTargetPlayer(EntityTargetEvent event) {

        if (event.getTarget() instanceof Player && afk.containsKey(event.getTarget())) {

            if (System.currentTimeMillis() - afk.get(event.getTarget()) >= TimeUnit.MINUTES.toMillis(3)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        afk.put(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {

        update(event.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        if (event.getPlayer() instanceof Player) update((Player) event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getWhoClicked() instanceof Player) update((Player) event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (event.getPlayer() instanceof Player) update((Player) event.getPlayer());
    }

    @EventHandler
    public void onEntityDamageEntityEvent(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player) update((Player) event.getDamager());
    }

    @EventHandler
    public void onMoveChange(PlayerMoveEvent event) {

        if (event.getFrom().distance(event.getTo()) >= .211) update(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {

        if (afk.containsKey(event.getPlayer())) afk.remove(event.getPlayer());
    }

    /**
     * This method is used to update a player's last active time
     *
     * @param player - The player to update
     */
    public void update(Player player) {

        afk.put(player, System.currentTimeMillis());

        if (godComponent.hasGodMode(player) && !adminComponent.isAdmin(player)) {
            godComponent.disableGodMode(player);
        }
    }
}
