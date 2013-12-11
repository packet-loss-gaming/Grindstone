package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.GodComponent;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.concurrent.TimeUnit;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "AFK Checker", desc = "AFK Checking.")
@Depend(components = {GodComponent.class, SessionComponent.class})
public class IdleComponent extends BukkitComponent implements Runnable, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    private GodComponent godComponent;
    @InjectComponent
    private SessionComponent sessions;

    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        server.getScheduler().runTaskTimer(inst, this, 20 * 60, 20 * 10);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("movement-threshold")
        public double movementThreshold = .04;
        @Setting("sneak-movement-threshold")
        public double sneakMovementThreshold = .004;
        @Setting("afk-minutes")
        public int afkMinutes = 3;
        @Setting("afk-kick-minutes")
        public int afkKickMinutes = 60;
    }

    public boolean isAfk(Player player) {

        return isAfk(sessions.getSession(AFKSession.class, player).getLastUpdate());
    }

    public boolean isAfk(long time) {

        return time != 0 && System.currentTimeMillis() - time >= TimeUnit.MINUTES.toMillis(config.afkMinutes);
    }

    public boolean shouldKick(Player player) {

        return shouldKick(sessions.getSession(AFKSession.class, player).getLastUpdate());
    }

    public boolean shouldKick(long time) {

        double maxP = server.getMaxPlayers();
        double curP = server.getOnlinePlayers().length;

        double fraction = ((maxP - curP) + maxP * .2) / maxP;
        int duration = (int) Math.max(config.afkMinutes + 2, Math.min(config.afkKickMinutes, config.afkKickMinutes * fraction));
        return time != 0 && System.currentTimeMillis() - time >= TimeUnit.MINUTES.toMillis(duration);
    }

    @Override
    public void run() {

        for (final AFKSession session : sessions.getSessions(AFKSession.class).values()) {

            if (session == null) continue;
            final Player target = session.getPlayer();
            if (target == null || !session.getPlayer().isValid()) continue;

            if (isAfk(session.getLastUpdate())) {
                if (shouldKick(session.getLastUpdate())) {
                    target.setSleepingIgnored(false);

                    server.getScheduler().runTaskLater(inst, new Runnable() {

                        @Override
                        public void run() {

                            target.kickPlayer("Inactivity - " + (System.currentTimeMillis() - session.getLastUpdate()) / 60000 + " Minutes");
                        }
                    }, 1);
                } else if (!target.isSleepingIgnored()) {
                    String name = target.getName();
                    target.setPlayerListName(ChatColor.GRAY + name.substring(0, Math.min(14, name.length())));
                    target.setSleepingIgnored(true);
                    if (!godComponent.hasGodMode(target)) {
                        godComponent.enableGodMode(target);
                        session.setAppliedGodMode(true);
                    }
                    ChatUtil.sendNotice(target, "You are now marked as AFK.");
                }

            } else if (target.isSleepingIgnored()) {

                target.setPlayerListName(target.getName());
                target.setSleepingIgnored(false);
                ChatUtil.sendNotice(target, "You are no longer marked as AFK.");
            }
        }
    }

    @EventHandler
    public void onEntityTargetPlayer(EntityTargetEvent event) {

        if (event.getTarget() instanceof Player) {

            if (isAfk((Player) event.getTarget())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        update(event.getPlayer());
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

        Player player = event.getPlayer();
        double distanceSQ = LocationUtil.distanceSquared2D(event.getFrom(), event.getTo());

        if (player.isSneaking() ? distanceSQ > config.sneakMovementThreshold : distanceSQ > config.movementThreshold) update(player);
    }

    /**
     * This method is used to update a player's last active time
     *
     * @param player - The player to update
     */
    public void update(Player player) {

        AFKSession session = sessions.getSession(AFKSession.class, player);
        session.setLastUpdate(System.currentTimeMillis());

        if (godComponent.hasGodMode(player) && session.appliedGodMode()) {
            godComponent.disableGodMode(player);
            session.setAppliedGodMode(false);
        }
    }

    // AFK Session
    private static class AFKSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        private long lastUpdate = 0;
        private boolean appliedGodMode = false;

        protected AFKSession() {

            super(MAX_AGE);
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }

        public long getLastUpdate() {

            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {

            this.lastUpdate = lastUpdate;
        }

        public boolean appliedGodMode() {

            return appliedGodMode;
        }

        public void setAppliedGodMode(boolean appliedGodMode) {

            this.appliedGodMode = appliedGodMode;
        }
    }
}
