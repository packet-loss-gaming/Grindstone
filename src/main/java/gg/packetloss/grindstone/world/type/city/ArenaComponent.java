/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.EggComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.jail.JailComponent;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.state.block.BlockStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.restoration.RestorationUtil;
import gg.packetloss.grindstone.world.type.city.arena.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Arena", desc = "Arena Control.")
@Depend(components = {
        AdminComponent.class, JailComponent.class, PrayerComponent.class, SacrificeComponent.class,
        RestorationUtil.class, EggComponent.class, HighScoresComponent.class,
        PlayerStateComponent.class, BlockStateComponent.class, GuildComponent.class
}, plugins = {"WorldEdit", "WorldGuard"})
public class ArenaComponent extends BukkitComponent implements Listener, Runnable {
    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private JailComponent jailComponent;
    @InjectComponent
    private PrayerComponent prayerComponent;
    @InjectComponent
    private RestorationUtil restorationUtil;
    @InjectComponent
    private EggComponent eggComponent;
    @InjectComponent
    private HighScoresComponent highScoresComponent;
    @InjectComponent
    private PlayerStateComponent playerStateComponent;
    @InjectComponent
    private BlockStateComponent blockStateComponent;
    @InjectComponent
    private GuildComponent guildComponent;

    private final World world = Bukkit.getWorld("City");
    private LocalConfiguration config;
    private ArenaManager arenaManager;

    private List<GenericArena> arenas = new ArrayList<>();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        this.config = configure(new LocalConfiguration());
        this.arenaManager = new ArenaManager();

        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(CommandBook.inst(), arenaManager::setupArenas, 1);
        scheduler.scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 20 * 4);

        registerCommands(Commands.class);
    }

    @Override
    public void disable() {

        for (GenericArena arena : arenas) {
            arena.disable();
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("list-regions-on-startup")
        public boolean listRegions = false;

        @Setting("snow-spleef-arenas")
        protected Set<String> snowSpleefRegions = new HashSet<>(Arrays.asList(
                "glacies-mare-district-spleef-snow"
        ));
        @Setting("enchanted-forest")
        protected Set<String> enchantedForest = new HashSet<>(Arrays.asList(
                "carpe-diem-district-enchanted-forest"
        ));
        @Setting("hot-springs")
        protected Set<String> hotSprings = new HashSet<>(Arrays.asList(
                "glacies-mare-district-hot-spring",
                "carpe-diem-distrcit-hot-spring"
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
    }

    private class ArenaManager {

        public void setupArenas() {
            Logger log = CommandBook.logger();
            RegionManager mgr = WorldGuardBridge.getManagerFor(world);
            // Add Enchanted Forest
            for (String region : config.enchantedForest) {
                try {
                    ProtectedRegion pr = mgr.getRegion(region);
                    arenas.add(new EnchantedForest(world, pr, adminComponent, eggComponent, blockStateComponent));
                    if (config.listRegions) log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }

            // Add Hot springs
            for (String region : config.hotSprings) {
                try {
                    ProtectedRegion pr = mgr.getRegion(region);
                    arenas.add(new HotSpringArena(world, pr, adminComponent));
                    if (config.listRegions) log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                    e.printStackTrace();
                }
            }

            // Add Party rooms
            for (String region : config.partyRooms) {
                try {
                    ProtectedRegion pr = mgr.getRegion(region);
                    arenas.add(new DropPartyArena(world, pr));
                    if (config.listRegions) log.info("Added region: " + pr.getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                }
            }

            // Add Gold Rushes
            for (String region : config.goldRushes) {
                try {
                    ProtectedRegion[] PRs = new ProtectedRegion[7];
                    PRs[0] = mgr.getRegion(region);
                    PRs[1] = mgr.getRegion(region + "-lobby");
                    PRs[2] = mgr.getRegion(region + "-room-one");
                    PRs[3] = mgr.getRegion(region + "-room-two");
                    PRs[4] = mgr.getRegion(region + "-room-three");
                    PRs[5] = mgr.getRegion(region + "-door-one");
                    PRs[6] = mgr.getRegion(region + "-door-two");
                    arenas.add(new GoldRush(
                            world, PRs, guildComponent,
                            highScoresComponent, playerStateComponent
                    ));
                    if (config.listRegions) log.info("Added region: " + PRs[0].getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                }
            }

            // Add Prison Raids
            for (String region : config.prisonRaids) {
                try {
                    ProtectedRegion[] PRs = new ProtectedRegion[2];
                    PRs[0] = mgr.getRegion(region);
                    PRs[1] = mgr.getRegion(region + "-office");
                    arenas.add(new Prison(world, PRs, adminComponent, guildComponent, playerStateComponent));
                    if (config.listRegions) log.info("Added region: " + PRs[0].getId() + " to Arenas.");
                } catch (Exception e) {
                    log.warning("Failed to add arena: " + region + ".");
                }
            }
        }
    }

    @Override
    public void run() {
        Collections.synchronizedList(arenas).stream()
                .filter(arena -> !(arena instanceof CommandTriggeredArena))
                .forEach(GenericArena::run);
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

        @Command(aliases = {"dropparty"},
                usage = "<area name> <value>", desc = "Create a drop party",
                flags = "", min = 2, max = 2)
        @CommandPermissions("aurora.arena.trigger")
        public void dropParty(CommandContext args, CommandSender sender) throws CommandException {

            for (GenericArena arena : arenas) {
                if (arena instanceof DropPartyArena) {
                    if (!args.getString(0).equalsIgnoreCase(arena.getId())) {
                        continue;
                    }
                    ((DropPartyArena) arena).drop(args.getInteger(1), 5);
                    return;
                }
            }
        }
    }
}
