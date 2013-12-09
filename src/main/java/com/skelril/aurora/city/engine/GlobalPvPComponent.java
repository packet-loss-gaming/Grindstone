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
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by Wyatt on 12/8/13.
 */
@ComponentInformation(friendlyName = "Global PvP", desc = "Global PvP Toggling.")
@Depend(components = SessionComponent.class, plugins = "WorldGuard")
public class GlobalPvPComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;

    private WorldGuardPlugin WG;

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

        this.WG = (WorldGuardPlugin) plugin;
    }

    public class Commands {

        @Command(aliases = {"pvp"},
                usage = "", desc = "Toggle PvP",
                flags = "csl", min = 0, max = 0)
        public void prayerCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) {
                throw new CommandException("You must be a player to use this command.");
            }

            PvPSession session = sessions.getSession(PvPSession.class, sender);

            session.setPvP(!session.hasPvPOn());

            ChatUtil.sendNotice(sender, "Global PvP has been: " + (session.hasPvPOn() ? "enabled" : "disabled") + ".");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvPBlock(DisallowedPVPEvent event) {

        Player attacker = event.getAttacker();
        Player defender = event.getDefender();

        PvPSession attackerSession = sessions.getSession(PvPSession.class, attacker);
        PvPSession defenderSession = sessions.getSession(PvPSession.class, defender);

        if (attackerSession.hasPvPOn() && defenderSession.hasPvPOn()) {

            String attackerHome = getHome(attacker);
            String defenderHome = getHome(defender);

            RegionManager manager = WG.getRegionManager(attacker.getWorld());
            ApplicableRegionSet attackerApplicable = manager.getApplicableRegions(attacker.getLocation());
            for (ProtectedRegion region : attackerApplicable) {
                String id = region.getId();

                if (id.equalsIgnoreCase(attackerHome) || id.equals(defenderHome)) {
                    return;
                }
            }

            ApplicableRegionSet defenderApplicable = manager.getApplicableRegions(defender.getLocation());
            for (ProtectedRegion region : defenderApplicable) {
                String id = region.getId();

                if (id.equalsIgnoreCase(attackerHome) || id.equals(defenderHome)) {
                    return;
                }
            }
            event.setCancelled(true);
        }
    }

    private String getHome(String player) {

        return player.toLowerCase() + "'s-house";
    }

    private String getHome(Player player) {

        return getHome(player.getName());
    }

    // PvP Session
    private static class PvPSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        private boolean hasPvPOn = false;

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
    }
}
