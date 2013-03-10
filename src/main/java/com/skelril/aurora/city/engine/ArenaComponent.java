package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.city.engine.arena.*;
import com.skelril.aurora.economic.ImpersonalComponent;
import com.skelril.aurora.jail.JailComponent;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Arena", desc = "Arena Control.")
@Depend(components = {
        AdminComponent.class, JailComponent.class, PrayerComponent.class, SacrificeComponent.class,
        ImpersonalComponent.class
}, plugins = {"WorldEdit", "WorldGuard"})
public class ArenaComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private JailComponent jailComponent;
    @InjectComponent
    private PrayerComponent prayerComponent;
    @InjectComponent
    private ImpersonalComponent impersonalComponent;

    private final World world = Bukkit.getWorld("City");
    private LocalConfiguration config;
    private ArenaManager arenaManager;

    private List<GenericArena> arenas = new ArrayList<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        this.config = configure(new LocalConfiguration());
        this.arenaManager = new ArenaManager();
        server.getScheduler().runTaskLater(inst, new Runnable() {

            @Override
            public void run() {

                arenaManager.setupArenas();
            }
        }, 1);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20 * 4);

        registerCommands(Commands.class);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    @Override
    public void disable() {

        for (GenericArena arena : arenas) {
            arena.disable();
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("snow-spleef-arenas")
        protected Set<String> snowSpleefRegions = new HashSet<>(Arrays.asList(
                "glacies-mare-district-spleef-snow"
        ));
        @Setting("sand-dynamic-arenas")
        protected Set<String> dynamicSandRegions = new HashSet<>(Arrays.asList(
                "oblitus-district-arena-pvp"
        ));
        @Setting("sand-dynamic-arenas-increase-rate")
        public int dynamicSandRegionsIR = 8;
        @Setting("sand-dynamic-arenas-decrease-rate")
        public int dynamicSandRegionsDR = 16;
        @Setting("cursed-mines")
        protected Set<String> cursedMines = new HashSet<>(Arrays.asList(
                "oblitus-district-cursed-mine"
        ));
        @Setting("enchanted-forest")
        protected Set<String> enchantedForest = new HashSet<>(Arrays.asList(
                "carpe-diem-district-enchanted-forest"
        ));
        @Setting("hot-springs")
        protected Set<String> hotSprings = new HashSet<>(Arrays.asList(
                "glacies-mare-district-hot-spring"
        ));
        @Setting("party-rooms")
        protected Set<String> partyRooms = new HashSet<>(Arrays.asList(
                "oblitus-district-party-room-drop-zone"
        ));
        @Setting("gold-rushes")
        protected Set<String> goldRushes = new HashSet<>(Arrays.asList(
                "vineam-district-gold-rush"
        ));
        @Setting("prison-raids")
        protected Set<String> prisonRaids = new HashSet<>(Arrays.asList(
                "vineam-district-prison"
        ));
        @Setting("zombie-bosses")
        protected Set<String> zombieBosses = new HashSet<>(Arrays.asList(
                "vineam-district-giant-boss-area"
        ));
    }

    private class ArenaManager {

        public void setupArenas() {

            // Add Snow Spleef Arenas
            for (String region : config.snowSpleefRegions) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    arenas.add(new SnowSpleefArena(world, pr, adminComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }

            // Add Dynamic Arenas
            for (String region : config.dynamicSandRegions) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    arenas.add(new DynamicSandArena(world, pr, config.dynamicSandRegionsIR, config.dynamicSandRegionsDR,
                            adminComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }

            // Add Cursed Mines
            for (String region : config.cursedMines) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    arenas.add(new CursedMine(world, pr, adminComponent, prayerComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }

            // Add Enchanted Forest
            for (String region : config.enchantedForest) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    arenas.add(new EnchantedForest(world, pr, adminComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }

            // Add Hot springs
            for (String region : config.hotSprings) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    arenas.add(new HotSpringArena(world, pr, adminComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }

            // Add Party rooms
            for (String region : config.partyRooms) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    arenas.add(new DropPartyArena(world, pr));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                }
            }

            // Add Gold Rushes
            for (String region : config.goldRushes) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    ProtectedRegion lb = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region
                            + "-lobby");
                    ProtectedRegion r1 = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region
                            + "-room-one");
                    ProtectedRegion r2 = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region
                            + "-room-two");
                    ProtectedRegion r3 = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region
                            + "-room-three");
                    ProtectedRegion d1 = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region
                            + "-door-one");
                    ProtectedRegion d2 = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region
                            + "-door-two");
                    arenas.add(new GoldRush(world, pr, lb, r1, r2, r3, d1, d2, adminComponent, impersonalComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                }
            }

            // Add Prison Raids
            for (String region : config.prisonRaids) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    ProtectedRegion of = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region
                            + "-office");
                    arenas.add(new Prison(world, pr, of, adminComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                }
            }

            // Add Zombie bosses
            for (String region : config.zombieBosses) {
                try {
                    ProtectedRegion pr = getWorldGuard().getGlobalRegionManager().get(world).getRegion(region);
                    arenas.add(new GiantBossArena(world, pr, adminComponent));
                    log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {

        for (GenericArena arena : Collections.unmodifiableList(arenas)) {
            if (!(arena instanceof CommandTriggeredArena)) arena.run();
        }
    }

    public class Commands {

        @Command(aliases = {"triggerarena"},
                usage = "[area name]", desc = "Run all command triggered arena",
                flags = "", min = 0, max = 1)
        @CommandPermissions("aurora.arena.trigger")
        public void areaTrigger(CommandContext args, CommandSender sender) throws CommandException {

            for (GenericArena arena : arenas) {
                if (arena instanceof CommandTriggeredArena) {
                    if (args.argsLength() > 0 && !args.getString(0).equalsIgnoreCase(arena.getId())) {
                        continue;
                    }
                    arena.run();
                    ChatUtil.sendNotice(sender, "Triggered arena: " + arena.getId() + ".");
                }
            }
        }
    }

    protected WorldGuardPlugin getWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }
}
