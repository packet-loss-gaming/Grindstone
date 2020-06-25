/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.firstlogin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.betterweather.WeatherType;
import gg.packetloss.grindstone.buff.Buff;
import gg.packetloss.grindstone.buff.BuffComponent;
import gg.packetloss.grindstone.events.BetterWeatherChangeEvent;
import gg.packetloss.grindstone.events.PortalRecordEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.playerhistory.PlayerHistoryComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.parser.HelpTextParser;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

@ComponentInformation(friendlyName = "First Login", desc = "Get stuff the first time you come.")
@Depend(components = {ManagedWorldComponent.class, BuffComponent.class, PlayerHistoryComponent.class})
public class FirstLoginComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private BuffComponent buffs;
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PlayerHistoryComponent playerHistory;

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        CommandBook.getComponentRegistrar().registerTopLevelCommands((commandManager, registration) -> {
            //  WarpPointConverter.register(commandManager, this);
            registration.register(commandManager, FirstLoginCommandsRegistration.builder(), new FirstLoginCommands(this));
        });
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("enable-lucky-diamond")
        public boolean luckyDiamond = true;
        @Setting("text.welcome-line")
        public String welcomeLine = "Welcome to the Packet Loss Gaming Minecraft server!";
        @Setting("text.lines")
        public List<String> introText = List.of(
                "Here's some information to get you started...",
                "Place your first chest to get a land claim.",
                "Place your bed and right click on it, to set your respawn point.",
                "You can get back to your bed with /home.",
                "Nether portals will take you to the city.",
                "You can also access a clickable list of warps via /warps list.",
                "To see this message again, use /welcome"
        );
        @Setting("welcome-protection-hours")
        public int welcomeProtectionHours = 24;
    }

    public Location getNewPlayerStartingLocation(Player player, ManagedWorldTimeContext timeContext) {
        return managedWorld.get(ManagedWorldGetQuery.RANGE_OVERWORLD, timeContext).getSpawnLocation();
    }

    public Location getSafeRoomLocation() {
        return new Location(managedWorld.get(ManagedWorldGetQuery.CITY), 8, 25, 8);
    }

    private ProtectedRegion getSafeRoomRegion() {
        return WorldGuardBridge.getManagerFor(managedWorld.get(ManagedWorldGetQuery.CITY)).getRegion("city-dung");
    }

    private boolean isInSafeRoom(Location location) {
        if (!managedWorld.is(ManagedWorldIsQuery.CITY, location.getWorld())) {
            return false;
        }

        return getSafeRoomRegion().contains(toBlockVec3(location));
    }

    private void giveStarterKit(Player player) {
        // Declare Item Stacks
        ItemStack[] startKit = new ItemStack[]{
                // BookUtil.Tutorial.newbieBook(),
                new ItemStack(Material.COOKED_BEEF, 32),
                new ItemStack(Material.CHEST),
                new ItemStack(Material.RED_BED),
                CustomItemCenter.build(CustomItems.GEM_OF_LIFE, 3)
        };


        Inventory pInv = player.getInventory();
        pInv.addItem(startKit);

        // Surprise!
        if (ChanceUtil.getChance(10) && config.luckyDiamond) {
            pInv.addItem(new ItemStack(Material.DIAMOND));

            ChatUtil.sendNotice(player, ChatColor.GOLD, "What's this, a diamond! You are very luck!");
        }
    }

    private boolean isNewerPlayer(Player player) {
        try {
            long timePlayed = playerHistory.getTimePlayed(player).get();
            return timePlayed < TimeUnit.HOURS.toSeconds(config.welcomeProtectionHours);
        }  catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseSpawn(ApocalypsePersonalSpawnEvent event) {
        Player player = event.getPlayer();

        if (isNewerPlayer(player)) {
            event.setCancelled(true);
        }
    }

    private void applyNewPlayerBuffs(Player player) {
        buffs.notifyFillToLevel(Buff.APOCALYPSE_DAMAGE_BOOST, player, 20);
        buffs.notifyFillToLevel(Buff.APOCALYPSE_MAGIC_SHIELD, player, 20);
        buffs.notifyFillToLevel(Buff.APOCALYPSE_LIFE_LEACH, player, 3);

        ChatUtil.sendNotice(player, ChatColor.GOLD, "New player assistance applied.");
    }

    private void maybeApplyNewPlayerBuffs(Player player) {
        if (isNewerPlayer(player) && player.getWorld().isThundering()) {
            applyNewPlayerBuffs(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onThunderChange(BetterWeatherChangeEvent event) {
        if (event.getNewWeatherType() == WeatherType.THUNDERSTORM) {
            server.getScheduler().runTask(inst, () -> {
                for (Player player : event.getWorld().getPlayers()) {
                    maybeApplyNewPlayerBuffs(player);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        if (!event.getFrom().isThundering()) {
            maybeApplyNewPlayerBuffs(event.getPlayer());
        }
    }

    private HelpTextParser WELCOME_MESSAGES_PARSER = new HelpTextParser(ChatColor.YELLOW);

    private void sendStaggered(CommandSender sender, Collection<String> lines) {
        ChatUtil.sendStaggered(sender, lines.stream()
                .map(WELCOME_MESSAGES_PARSER::parse)
                .collect(Collectors.toList()));
    }

    private void welcome(Player player) {
        List<String> combinedText = new ArrayList<>();

        combinedText.add(config.welcomeLine);
        combinedText.addAll(config.introText);

        sendStaggered(player, combinedText);

        // Tell others to great him/her
        for (Player otherPlayer : server.getOnlinePlayers()) {
            // Don't tell the player we are sending this message
            if (otherPlayer != player) {
                ChatUtil.sendNotice(otherPlayer, "Please welcome, " + player.getDisplayName() + " to the server.");
            }
        }
    }

    protected void sendIntroText(CommandSender sender) {
        sendStaggered(sender, config.introText);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            player.teleport(getSafeRoomLocation());
            giveStarterKit(player);
        }

        if (isInSafeRoom(player.getLocation())) {
            welcome(player);
        }

        maybeApplyNewPlayerBuffs(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (isInSafeRoom(event.getFrom())) {
            event.setCancelled(true);
            event.getPlayer().teleport(
                    getNewPlayerStartingLocation(event.getPlayer(), ManagedWorldTimeContext.LATEST),
                    PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
            );
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalRecord(PortalRecordEvent event) {
        if (isInSafeRoom(event.getPortalLocation())) {
            event.setCancelled(true);
        }
    }
}