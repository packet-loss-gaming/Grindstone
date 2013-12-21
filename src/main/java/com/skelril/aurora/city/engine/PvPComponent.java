package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
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
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by Wyatt on 12/8/13.
 */
@ComponentInformation(friendlyName = "PvP", desc = "Skelril PvP management.")
@Depend(components = SessionComponent.class, plugins = "WorldGuard")
public class PvPComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private static SessionComponent sessions;

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
        public void prayerCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) {
                throw new CommandException("You must be a player to use this command.");
            }

            PvPSession session = sessions.getSession(PvPSession.class, sender);

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        sessions.getSession(PvPSession.class, event.getPlayer()).setPvP(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvPBlock(DisallowedPVPEvent event) {

        if (allowsPvP(event.getAttacker(), event.getDefender(), false)) event.setCancelled(true);
    }

    public static boolean allowsPvP(Player attacker, Player defender) {

        return allowsPvP(attacker, defender, true);
    }

    public static boolean allowsPvP(Player attacker, Player defender, boolean checkRegions) {

        PvPSession attackerSession = sessions.getSession(PvPSession.class, attacker);
        PvPSession defenderSession = sessions.getSession(PvPSession.class, defender);

        if (attackerSession.hasPvPOn() && defenderSession.hasPvPOn()) {

            String attackerHome = getHome(attacker);
            String defenderHome = getHome(defender);

            if (!attackerSession.useSafeSpots()) attackerHome = "";
            if (!defenderSession.useSafeSpots()) defenderHome = "";

            RegionManager manager = WG.getRegionManager(attacker.getWorld());
            ApplicableRegionSet attackerApplicable = manager.getApplicableRegions(attacker.getLocation());
            for (ProtectedRegion region : attackerApplicable) {
                String id = region.getId();

                if (id.equalsIgnoreCase(attackerHome) || id.equals(defenderHome)) {
                    return false;
                }
            }

            ApplicableRegionSet defenderApplicable = manager.getApplicableRegions(defender.getLocation());
            for (ProtectedRegion region : defenderApplicable) {
                String id = region.getId();

                if (id.equalsIgnoreCase(attackerHome) || id.equals(defenderHome)) {
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

    private static String getHome(String player) {

        return player.toLowerCase() + "'s-house";
    }

    private static String getHome(Player player) {

        return getHome(player.getName());
    }

    // PvP Session
    private static class PvPSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        private boolean hasPvPOn = false;
        private boolean useSafeSpots = true;

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
    }
}
