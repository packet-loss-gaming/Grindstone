/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.pvp;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.exceptions.UnsupportedPrayerException;
import com.skelril.aurora.homes.HomeManagerComponent;
import com.skelril.aurora.prayer.Prayer;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "PvP", desc = "Skelril PvP management.")
@Depend(components = {SessionComponent.class, PrayerComponent.class}, plugins = "WorldGuard")
public class PvPComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private static SessionComponent sessions;
    @InjectComponent
    private PrayerComponent prayers;

    private static List<PvPScope> pvpLimitors = new ArrayList<>();
    private static WorldGuardPlugin WG;

    @Override
    public void enable() {

        try {
            setUpWorldGuard();
        } catch (UnknownPluginException e) {
            log.warning("Plugin not found: " + e.getMessage() + ".");
            return;
        }
        registerCommands(Commands.class);
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private void setUpWorldGuard() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }

        WG = (WorldGuardPlugin) plugin;
    }

    public class Commands {

        @Command(aliases = {"pvp"},
                usage = "", desc = "Toggle PvP",
                flags = "s", min = 0, max = 0)
        public void pvpCmd(CommandContext args, CommandSender sender) throws CommandException {

            PlayerUtil.checkPlayer(sender);

            PvPSession session = sessions.getSession(PvPSession.class, sender);

            if (session.recentlyHit()) {
                throw new CommandException("You have been hit recently and cannot toggle PvP!");
            }

            if (!args.hasFlag('s') || !session.hasPvPOn()) {
                session.setPvP(!session.hasPvPOn());
                ChatUtil.sendNotice(sender, "Global PvP has been: " + (session.hasPvPOn() ? "enabled" : "disabled") + ".");

                session.useSafeSpots(!args.hasFlag('s'));
            } else {
                if (session.useSafeSpots()) {
                    session.useSafeSpots(!args.hasFlag('s'));
                } else {
                    session.useSafeSpots(args.hasFlag('s'));
                }
            }

            if (session.hasPvPOn()) {
                ChatUtil.sendNotice(sender, "Safe spots are: " + (session.useSafeSpots() ? "enabled" : "disabled") + ".");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        final Player player = event.getPlayer();
        PvPSession session = sessions.getSession(PvPSession.class, player);

        if (session.punishNextLogin()) {
            try {
                Prayer[] targetPrayers = new Prayer[]{
                        PrayerComponent.constructPrayer(player, PrayerType.GLASSBOX, 1000 * 60 * 3),
                        PrayerComponent.constructPrayer(player, PrayerType.STARVATION, 1000 * 60 * 3)
                };

                prayers.influencePlayer(player, targetPrayers);

                server.getScheduler().runTaskLater(inst,
                        () -> ChatUtil.sendWarning(player,
                                "You ran from a fight, the Giant Chicken does not approve!"), 1
                );
            } catch (UnsupportedPrayerException ignored) {
            }
        }
        session.wasKicked(false);
    }


    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {

        PvPSession session = sessions.getSession(PvPSession.class, event.getPlayer());

        if (session.recentlyHit()) {
            session.wasKicked(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        PvPSession session = sessions.getSession(PvPSession.class, event.getPlayer());

        if (session.recentlyHit() || session.punishNextLogin()) {
            session.punishNextLogin(true);
            return;
        }

        session.setPvP(false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        if (!(entity instanceof Player) || !(damager instanceof Player)) return;

        sessions.getSession(PvPSession.class, (Player) entity).updateHit();
        sessions.getSession(PvPSession.class, (Player) damager).updateHit();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        PvPSession session = sessions.getSession(PvPSession.class, event.getEntity());

        if (session.punishNextLogin()) {
            session.punishNextLogin(false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvPBlock(DisallowedPVPEvent event) {

        if (allowsPvP(event.getAttacker(), event.getDefender(), false)) event.setCancelled(true);
    }

    public static void registerScope(PvPScope scope) {
        pvpLimitors.add(scope);
    }

    public static void removeScope(PvPScope scope) {
        pvpLimitors.remove(scope);
    }

    public static boolean allowsPvP(Player attacker, Player defender) {

        return allowsPvP(attacker, defender, true);
    }

    private static boolean checkSafeZone(ApplicableRegionSet regions, Player attacker, Player defender) {
        for (ProtectedRegion region : regions) {
            if (HomeManagerComponent.isPlayerHouse(region, attacker) || HomeManagerComponent.isPlayerHouse(region, defender)) {
                return false;
            }
        }
        return true;
    }

    public static boolean allowsPvP(Player attacker, Player defender, boolean checkRegions) {

        PvPSession attackerSession = sessions.getSession(PvPSession.class, attacker);
        PvPSession defenderSession = sessions.getSession(PvPSession.class, defender);

        for (PvPScope scope : pvpLimitors) {
            if (!scope.checkFor(attacker, defender)) return false;
        }

        if (attackerSession.hasPvPOn() && defenderSession.hasPvPOn()) {
            if (attackerSession.useSafeSpots() || defenderSession.useSafeSpots()) {
                RegionManager manager = WG.getRegionManager(attacker.getWorld());

                if (!checkSafeZone(manager.getApplicableRegions(attacker.getLocation()), attacker, defender)) {
                    return false;
                }

                if (!checkSafeZone(manager.getApplicableRegions(defender.getLocation()), attacker, defender)) {
                    return false;
                }
            }
            return true;
        } else if (checkRegions) {
            RegionManager manager = WG.getRegionManager(attacker.getWorld());
            ApplicableRegionSet attackerApplicable = manager.getApplicableRegions(attacker.getLocation());
            ApplicableRegionSet defenderApplicable = manager.getApplicableRegions(defender.getLocation());

            return attackerApplicable.allows(DefaultFlag.PVP) && defenderApplicable.allows(DefaultFlag.PVP);
        }
        return false;
    }

    // PvP Session
    private static class PvPSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        // Flag booleans
        private boolean hasPvPOn = false;
        private boolean useSafeSpots = true;

        // Punishment booleans & data
        private boolean wasKicked = false;
        private boolean punishNextLogin = false;

        private long nextFreePoint = 0;

        protected PvPSession() {

            super(MAX_AGE);
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }

        public boolean hasPvPOn() {

            return hasPvPOn;
        }

        public void setPvP(boolean hasPvPOn) {

            this.hasPvPOn = hasPvPOn;
        }

        public boolean useSafeSpots() {

            return useSafeSpots;
        }

        public void useSafeSpots(boolean useSafeSpots) {

            this.useSafeSpots = useSafeSpots;
        }

        public boolean punishNextLogin() {

            return punishNextLogin && !wasKicked;
        }

        public void punishNextLogin(boolean witherNextLogin) {

            this.punishNextLogin = witherNextLogin;
        }

        public void wasKicked(boolean wasKicked) {

            this.wasKicked = wasKicked;
        }

        public boolean recentlyHit() {

            return System.currentTimeMillis() < nextFreePoint;
        }

        public void updateHit() {

            nextFreePoint = System.currentTimeMillis() + 7000;
        }


    }
}