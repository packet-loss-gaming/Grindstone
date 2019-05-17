/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.HomeTeleportEvent;
import gg.packetloss.grindstone.events.PrayerApplicationEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseBedSpawnEvent;
import gg.packetloss.grindstone.events.egg.EggDropEvent;
import gg.packetloss.grindstone.homes.CSVHomeDatabase;
import gg.packetloss.grindstone.homes.EnderPearlHomesComponent;
import gg.packetloss.grindstone.homes.HomeDatabase;
import gg.packetloss.grindstone.homes.HomeManager;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.player.PlayerState;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Legit Core", desc = "Operate the legit world.")
@Depend(components = {AdminComponent.class, SessionComponent.class, EnderPearlHomesComponent.class})
public class LegitCoreComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private EnderPearlHomesComponent mainHomeDatabase;

    private final File legitFileDir = new File(inst.getDataFolder().getPath() + "/legit/legit");
    private final File normalFileDir = new File(inst.getDataFolder().getPath() + "/legit/main");

    private LocalConfiguration config;
    private HomeManager homeManager;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);

        File homeDirectory = new File(inst.getDataFolder().getPath() + "/home");
        if (!homeDirectory.exists()) homeDirectory.mkdir();

        HomeDatabase homeDatabase = new CSVHomeDatabase("legithomes", homeDirectory);
        homeDatabase.load();

        homeManager = new HomeManager(homeDatabase);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    public Location getBedLocation(Player player) {
        return homeManager.getSafePlayerHome(player).orElse(null);
    }

    public Location getRespawnLocation(Player player) {
        Location respawnLoc = getBedLocation(player);

        // Fallback to the world spawn
        if (respawnLoc == null) {
            respawnLoc = Bukkit.getWorld(config.legitWorld).getSpawnLocation();
        }

        return respawnLoc;
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("city-world")
        public String cityWorld = "City";
        @Setting("wilderness-world")
        public String legitWorld = "Legit";
    }

    public class Commands {
        @Command(aliases = {"legit", "seemslegit"}, desc = "Enter Legit World",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"multiverse.access.Legit", "multiverse.access.Legit_nether"})
        public void toggleLegitCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            player.teleport(getTo(player, player.getWorld().getName().contains(config.legitWorld)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {
        World world = event.getPlayer().getWorld();

        if (world.getName().contains(config.legitWorld) && event.getCause().getEffect().getType().isHoly()) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEggDrop(EggDropEvent event) {
        World world = event.getLocation().getWorld();

        if (world.getName().contains(config.legitWorld)) {

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
        TravelAgent agent = event.getPortalTravelAgent();

        final Player player = event.getPlayer();
        final Location pLoc = player.getLocation().clone();
        final Location from = event.getFrom();

        final World legit = Bukkit.getWorld(config.legitWorld);
        final World legitNether = Bukkit.getWorld(config.legitWorld + "_nether");
        boolean kill = false;

        if (legit == null) {
            log.warning("Please verify the world: " + config.legitWorld + " exist.");
            kill = true;
        }
        if (legitNether == null) {
            log.warning("Please verify the world: " + config.legitWorld + "_nether exist.");
            kill = true;
        }
        if (kill) return;


        switch (event.getCause()) {
            case NETHER_PORTAL:

                event.useTravelAgent(true);
                if (from.getWorld().equals(legit)) {
                    pLoc.setWorld(legitNether);
                    pLoc.setX(pLoc.getBlockX() / 8);
                    pLoc.setZ(pLoc.getBlockZ() / 8);
                    agent.setCanCreatePortal(true);
                    event.setPortalTravelAgent(agent);
                    event.setTo(agent.findOrCreate(pLoc));
                    return;
                } else if (from.getWorld().getName().contains(config.legitWorld)) {
                    pLoc.setWorld(legit);
                    pLoc.setX(pLoc.getBlockX() * 8);
                    pLoc.setZ(pLoc.getBlockZ() * 8);
                    agent.setCanCreatePortal(true);
                    event.setPortalTravelAgent(agent);
                    event.setTo(agent.findOrCreate(pLoc));
                    return;
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        String fromName = from.getWorld().getName();
        String toName = to.getWorld().getName();

        LegitSession session = sessions.getSession(LegitSession.class, event.getPlayer());

        if (toName.contains(config.legitWorld) && !fromName.contains(config.legitWorld)) {
            session.setFromIndex(from);
        } else if (fromName.contains(config.legitWorld) && !toName.contains(config.legitWorld)) {
            session.setLegitIndex(from);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        check(player, event.getFrom().getName(), player.getWorld().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHomeTeleport(HomeTeleportEvent event) {
        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.legitWorld)) {

            event.setDestination(getRespawnLocation(player));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseBedSpawn(ApocalypseBedSpawnEvent event) {
        Player player = event.getPlayer();

        if (player.getWorld().getName().contains(config.legitWorld) && getBedLocation(player) != null) {

            event.setLocation(getBedLocation(player));
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        Location bedLoc = event.getBed().getLocation();

        if (player == null || bedLoc == null) return;
        if (!player.getWorld().getName().toLowerCase().contains("legit")) return;

        homeManager.setPlayerHomeAndNotify(player, bedLoc);
    }

    public void check(final Player player, String from, String to) {
        boolean result = false;
        boolean fromMain = false;
        if (to.contains(config.legitWorld) && !from.contains(config.legitWorld)) {

            ChatUtil.sendNotice(player, "You have entered legit world.");

            World fromW = Bukkit.getWorld(from);
            if (fromW != null && fromW.isThundering()) {

                World toW = Bukkit.getWorld(to);
                if (toW != null) {
                    toW.setThundering(true);
                    toW.setThunderDuration(fromW.getThunderDuration());
                }
            }

            result = true;
            fromMain = true;
        } else if (from.contains(config.legitWorld) && !to.contains(config.legitWorld)) {

            ChatUtil.sendNotice(player, "You have left legit world.");

            result = true;
        }

        if (result) {
            adminComponent.deadmin(player);

            final File fromDir = fromMain ? normalFileDir : legitFileDir;
            final File toDir = fromMain ? legitFileDir : normalFileDir;

            final PlayerState unlegitState = new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getExhaustion(),
                    player.getLevel(),
                    player.getExp());

            server.getScheduler().runTaskAsynchronously(inst,
                    () -> IOUtil.toBinaryFile(fromDir, unlegitState.getOwnerName(), unlegitState));

            File target = new File(toDir.getPath() + "/" + unlegitState.getOwnerName() + ".dat");

            if (target.exists()) {
                PlayerState identity = (PlayerState) IOUtil.readBinaryFile(target);

                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                player.setHealth(Math.min(player.getMaxHealth(), identity.getHealth()));
                player.setFoodLevel(identity.getHunger());
                player.setSaturation(identity.getSaturation());
                player.setExhaustion(identity.getExhaustion());
                player.setLevel(identity.getLevel());
                player.setExp(identity.getExperience());
            } else {
                player.getInventory().setArmorContents(null);
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(5);
                player.setExhaustion(0);
                player.setLevel(0);
                player.setExp(0);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (adminComponent.isAdmin(player)) return;
        if (player.getWorld().getName().contains(config.legitWorld) && event.isFlying()) event.setCancelled(true);
    }

    public Location getTo(Player player, boolean fromLegit) {
        LegitSession session = sessions.getSession(LegitSession.class, player);
        if (fromLegit) {
            if (session.isSet()) {
                session.setLegitIndex(player.getLocation());
                return session.getFromIndex();
            } else {
                Location target = mainHomeDatabase.getRespawnLocation(player);
                session.setLegitIndex(player.getLocation());
                session.setFromIndex(target);
                return target;
            }
        } else {
            if (session.isSet()) {
                session.setFromIndex(player.getLocation());
                return session.getLegitIndex();
            } else {
                Location target = getRespawnLocation(player);
                session.setLegitIndex(target);
                session.setFromIndex(player.getLocation());
                return target;
            }
        }
    }

    private static class LegitSession extends PersistentSession {
        private static final long MAX_AGE = TimeUnit.DAYS.toMillis(3);

        @Setting("is-from-set")
        private boolean isFromSet = false;
        @Setting("is-legit-set")
        private boolean isLegitSet = false;

        @Setting("legit-x")
        private double legit_x = 0;
        @Setting("legit-y")
        private double legit_y = 0;
        @Setting("legit-z")
        private double legit_z = 0;
        @Setting("legit-pitch")
        private float legit_pitch = 0;
        @Setting("legit-yaw")
        private float legit_yaw = 0;

        @Setting("from-world")
        private String from_world = "City";
        @Setting("from-x")
        private double from_x = 0;
        @Setting("from-y")
        private double from_y = 0;
        @Setting("from-z")
        private double from_z = 0;
        @Setting("from-pitch")
        private float from_pitch = 0;
        @Setting("from-yaw")
        private float from_yaw = 0;

        protected LegitSession() {
            super(MAX_AGE);
        }

        public boolean isSet() {
            return isFromSet && isLegitSet;
        }

        public Location getFromIndex() {
            return new Location(Bukkit.getWorld(from_world), from_x, from_y, from_z, from_yaw, from_pitch);
        }

        public void setFromIndex(Location legitIndex) {
            from_world = legitIndex.getWorld().getName();
            from_x = legitIndex.getX();
            from_y = legitIndex.getY();
            from_z = legitIndex.getZ();
            from_yaw = legitIndex.getYaw();
            from_pitch = legitIndex.getPitch();

            isFromSet = true;
        }

        public Location getLegitIndex() {
            return new Location(Bukkit.getWorld("Legit"), legit_x, legit_y, legit_z, legit_yaw, legit_pitch);
        }

        public void setLegitIndex(Location legitIndex) {
            legit_x = legitIndex.getX();
            legit_y = legitIndex.getY();
            legit_z = legitIndex.getZ();
            legit_yaw = legitIndex.getYaw();
            legit_pitch = legitIndex.getPitch();

            isLegitSet = true;
        }
    }
}