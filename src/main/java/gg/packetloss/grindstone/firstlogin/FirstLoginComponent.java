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
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.betterweather.WeatherType;
import gg.packetloss.grindstone.buff.Buff;
import gg.packetloss.grindstone.buff.BuffComponent;
import gg.packetloss.grindstone.events.BetterWeatherChangeEvent;
import gg.packetloss.grindstone.events.PlayerDeathDropRedirectEvent;
import gg.packetloss.grindstone.events.PortalRecordEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseOverflowEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.invite.PlayerInviteComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.playerhistory.PlayerHistoryComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ErrorUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.parser.HelpTextParser;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.warps.WarpsComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

@ComponentInformation(friendlyName = "First Login", desc = "Get stuff the first time you come.")
@Depend(components = {ManagedWorldComponent.class, BuffComponent.class, PlayerHistoryComponent.class,
                      PlayerInviteComponent.class, WarpsComponent.class})
public class FirstLoginComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private BuffComponent buffs;
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PlayerHistoryComponent playerHistory;
    @InjectComponent
    private PlayerInviteComponent playerInvite;
    @InjectComponent
    private WarpsComponent warps;

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        CommandBook.registerEvents(this);

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            //  WarpPointConverter.register(commandManager, this);
            registrar.register(FirstLoginCommandsRegistration.builder(), new FirstLoginCommands(this));
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

    public TaskFuture<Location> getNewPlayerStartingLocation(Player player) {
        UUID playerID = player.getUniqueId();

        return playerInvite.getInvitingPlayer(playerID).thenApply(
            (optInvitor) -> {
                if (optInvitor.isPresent()) {
                    UUID invitor = optInvitor.get();
                    Optional<Location> inviteDestination = playerInvite.getInviteDestination(invitor);

                    if (inviteDestination.isPresent()) {
                        return inviteDestination.get();
                    }
                }

                World world = managedWorld.get(ManagedWorldGetQuery.RANGE_OVERWORLD, ManagedWorldTimeContext.LATEST);
                return world.getSpawnLocation();
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
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

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseSpawn(ApocalypseOverflowEvent event) {
        if (event.getSpawnKind() == ApocalypseOverflowEvent.SpawnKind.NONE) {
            return;
        }

        Player player = event.getPlayer();
        if (!isNewerPlayer(player)) {
            return;
        }

        event.setKillChance(3);
        event.setSpawnKind(ApocalypseOverflowEvent.SpawnKind.NONE);

        ChatUtil.sendNotice(player, ChatColor.GOLD, "[Friendly Spirit] I've protected you from a great evil!");
        ChatUtil.sendNotice(player, ChatColor.GOLD, "[Friendly Spirit] Try to keep the number of zombies down!");
    }

    private void applyNewPlayerBuffs(Player player) {
        buffs.notifyFillToLevel(Buff.APOCALYPSE_DAMAGE_BOOST, player, 20);
        buffs.notifyFillToLevel(Buff.APOCALYPSE_MAGIC_SHIELD, player, 20);
        buffs.notifyFillToLevel(Buff.APOCALYPSE_LIFE_LEACH, player, 3);
        buffs.notifyFillToLevel(Buff.APOCALYPSE_OVERLORD, player, 1);

        ChatUtil.sendNotice(player, ChatColor.GOLD, "[Friendly Spirit] Here, have some of my strength new one!");
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
            Bukkit.getScheduler().runTask(CommandBook.inst(), () -> {
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

    private final Set<UUID> freePhantomPotion = new HashSet<>();

    @EventHandler
    public void onGraveCreate(PlayerDeathDropRedirectEvent event) {
        Player player = event.getPlayer();

        if (isNewerPlayer(player)) {
            freePhantomPotion.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (freePhantomPotion.remove(player.getUniqueId())) {
            Inventory playerInv = player.getInventory();

            playerInv.addItem(new ItemStack(Material.WOODEN_PICKAXE));
            playerInv.addItem(CustomItemCenter.build(CustomItems.NEWBIE_PHANTOM_POTION));

            ChatUtil.sendStaggered(player, Stream.of(
                "Oh no! You died without a gem of life!",
                "Your stuff has been sent to The Graveyard.",
                "Since you're new around here, I've given you a phantom potion.",
                "Drink the Phantom Potion to return to your grave.",
                "Then simply break the cracked stone or dirt blocks."
            ).map((text) -> Text.of(ChatColor.YELLOW, "[Friendly Spirit] ", text)).toList());
        }
    }

    private static final HelpTextParser WELCOME_MESSAGES_PARSER = new HelpTextParser(ChatColor.YELLOW);

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
        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
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

    private final Set<UUID> playersBeingTeleportedToFirstLoc = new HashSet<>();

    private void sendToInitialLocation(Player player) {
        UUID playerID = player.getUniqueId();

        if (playersBeingTeleportedToFirstLoc.contains(playerID)) {
            return;
        }

        playersBeingTeleportedToFirstLoc.add(playerID);
        getNewPlayerStartingLocation(player).thenComposeNative(
            (destination) -> player.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)
        ).thenFinally(() -> {
            playersBeingTeleportedToFirstLoc.remove(playerID);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(EntityPortalReadyEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!isInSafeRoom(player.getLocation())) {
            return;
        }

        event.setCancelled(true);
        sendToInitialLocation(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalRecord(PortalRecordEvent event) {
        if (isInSafeRoom(event.getPortalLocation())) {
            event.setCancelled(true);
        }
    }
}