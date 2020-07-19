/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.events.PrayerApplicationEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLocalSpawnEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Lever;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

public class GoldRush extends AbstractRegionedArena implements MonitoredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private Economy economy;
    private GuildComponent guildComponent;
    private HighScoresComponent highScoresComponent;
    private PlayerStateComponent playerStateComponent;

    private ProtectedRegion lobby;

    private ProtectedRegion roomOne, roomTwo, roomThree;

    private ProtectedRegion doorOne, doorTwo;

    // Block - Should be flipped
    private ConcurrentHashMap<Location, Boolean> leverBlocks = new ConcurrentHashMap<>();
    private List<Location> floodBlocks = new ArrayList<>();
    private List<Location> chestBlocks = new ArrayList<>();

    // Block - Is unlocked
    private List<Location> locks = new ArrayList<>();
    private Location rewardChest;

    // Session
    private long startTime = System.currentTimeMillis();
    private int lootSplit = 0;
    private Material floodBlockType = Material.WATER;
    private List<String> players = new ArrayList<>();
    private boolean leversTriggered = false;

    public GoldRush(World world, ProtectedRegion[] regions,
                    GuildComponent guildComponent, HighScoresComponent highScoresComponent,
                    PlayerStateComponent playerStateComponent) {

        super(world, regions[0]);

        this.lobby = regions[1];

        this.roomOne = regions[2];
        this.roomTwo = regions[3];
        this.roomThree = regions[4];

        this.doorOne = regions[5];
        this.doorTwo = regions[6];

        this.guildComponent = guildComponent;
        this.highScoresComponent = highScoresComponent;
        this.playerStateComponent = playerStateComponent;

        CommandBook.registerEvents(new FlightBlockingListener(this::contains));

        findChestAndKeys();         // Setup room one
        findLeversAndFloodBlocks(); // Setup room two
        findRewardChest();          // Setup room three

        setupEconomy();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void forceRestoreBlocks() {

        resetChestAndKeys();
        drainAll();
        resetFloodType();
        resetLevers();
        setDoor(doorOne, Material.IRON_BLOCK);
        setDoor(doorTwo, Material.IRON_BLOCK);
    }

    private void clearFloor() {

        getWorld().getEntitiesByClass(Item.class).stream().filter(i -> i.isValid()
                && getRegion().contains(toBlockVec3(i.getLocation()))).forEach(Item::remove);
    }

    private void findRewardChest() {
        RegionWalker.testWalk(roomThree, (x, y, z) -> {
            BlockState block = getWorld().getBlockAt(x, y, z).getState();
            if (block.getType() == Material.CHEST) {
                rewardChest = block.getLocation();
                return true;
            }

            return false;
        });
    }

    private void findChestAndKeys() {
        RegionWalker.walk(roomOne, (x, y, z) -> {
            BlockState block = getWorld().getBlockAt(x, y, z).getState();
            if (block.getType() == Material.CHEST) {
                ((Chest) block).getInventory().clear();
                block.update(true);
                chestBlocks.add(block.getLocation());
            } else if (EnvironmentUtil.isSign(block.getType())) {
                ((Sign) block).setLine(2, "- Locked -");
                ((Sign) block).setLine(3, "Unlocked");
                block.update(true);
                locks.add(block.getLocation());
            }
        });
    }

    private static final ItemStack goldBar = new ItemStack(Material.GOLD_INGOT);
    private static final ItemStack[] keys = new ItemStack[]{
            new ItemStack(Material.BLUE_WOOL),
            new ItemStack(Material.RED_WOOL)
    };

    private void populateChest() {

        Chest chestState;

        for (Location chest : chestBlocks) {
            if (chest.getBlock().getType() != Material.CHEST) continue;

            chestState = (Chest) chest.getBlock().getState();
            Inventory inventory = chestState.getBlockInventory();
            for (int i = ChanceUtil.getRandom(9 * 3); i > 0; --i) {

                ItemStack targetStack = goldBar.clone();
                targetStack.setAmount(ChanceUtil.getRandom(3));

                if (ChanceUtil.getChance(300)) {
                    inventory.addItem(CustomItemCenter.build(CustomItems.PIXIE_DUST, ChanceUtil.getRandom(12)));
                }
                if (ChanceUtil.getChance(1000)) {
                    targetStack = CustomItemCenter.build(CustomItems.PHANTOM_GOLD, ChanceUtil.getRandom(6));
                }
                if (ChanceUtil.getChance(10000)) {
                    inventory.addItem(CustomItemCenter.build(CustomItems.PHANTOM_HYMN));
                }

                inventory.setItem(ChanceUtil.getRandom(inventory.getSize()) - 1, targetStack);
            }
        }

        for (int i = 0; i < 2; i++) {
            Block block = CollectionUtil.getElement(chestBlocks).getBlock();

            Chest chest = (Chest) block.getState();
            chest.getInventory().setItem(ChanceUtil.getRandom(chest.getBlockInventory().getSize() - 1), keys[i]);
        }
    }

    public boolean checkKeys() {
        for (Location lock : locks) {
            Sign aSign = (Sign) lock.getBlock().getState();
            if (aSign.getLine(2).startsWith("-")) return false;
        }
        return true;
    }

    private void resetChestAndKeys() {
        for (Location chest : chestBlocks) {
            if (chest.getBlock().getType() != Material.CHEST) continue;

            Chest chestState = (Chest) chest.getBlock().getState();
            chestState.getBlockInventory().clear();
        }

        for (Location lock : locks) {
            Sign signState = (Sign) lock.getBlock().getState();
            signState.setLine(2, "- Locked -");
            signState.setLine(3, "Unlocked");
            signState.update(true);
        }
    }

    private void findLeversAndFloodBlocks() {
        BlockVector3 min = roomTwo.getMinimumPoint();
        BlockVector3 max = roomTwo.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; --y) {
                    BlockState block = getWorld().getBlockAt(x, y, z).getState();
                    if (block.getType() == Material.LEVER) {
                        Lever lever = (Lever) block.getData();
                        lever.setPowered(false);
                        block.setData(lever);
                        block.update(true);
                        leverBlocks.put(block.getLocation(), !ChanceUtil.getChance(3));
                        for (int i = y; i < maxY; i++) {
                            block = getWorld().getBlockAt(x, i, z).getState();
                            if (block.getType() == Material.AIR) {
                                floodBlocks.add(block.getLocation());
                                break;
                            }
                        }
                        break; // One lever a column only
                    }
                }
            }
        }
    }

    private boolean checkLevers() {
        for (Map.Entry<Location, Boolean> lever : leverBlocks.entrySet()) {
            Lever aLever = (Lever) lever.getKey().getBlock().getState().getData();
            if (aLever.isPowered() != lever.getValue()) return false;
        }
        return true;
    }

    private void resetLevers() {
        for (Location entry : leverBlocks.keySet()) {
            BlockState state = entry.getBlock().getState();
            Lever lever = (Lever) state.getData();
            lever.setPowered(false);
            state.setData(lever);
            state.update(true);
            leverBlocks.put(entry, !ChanceUtil.getChance(3));
        }
    }

    private long lastSwitch = System.currentTimeMillis();

    private void randomizeLevers() {
        if (System.currentTimeMillis() - lastSwitch >= TimeUnit.SECONDS.toMillis(14)) {
            for (Location entry : leverBlocks.keySet()) {
                BlockState state = entry.getBlock().getState();
                Lever lever = (Lever) state.getData();
                lever.setPowered(false);
                state.setData(lever);
                state.update(true);
                leverBlocks.put(entry, !ChanceUtil.getChance(3));
            }
            lastSwitch = System.currentTimeMillis();
            randomizeLevers();
        } else if (System.currentTimeMillis() - lastSwitch == 0) {
            for (Map.Entry<Location, Boolean> entry : leverBlocks.entrySet()) {
                Location mutable = entry.getKey().clone();
                mutable.add(0, -2, 0);

                mutable.getBlock().setType(Material.STONE_BRICKS);
            }
            server.getScheduler().runTaskLater(inst, () -> {
                for (Map.Entry<Location, Boolean> entry : leverBlocks.entrySet()) {
                    Location mutable = entry.getKey().clone();
                    mutable.add(0, -2, 0);

                    Material targetBlock = entry.getValue() ? Material.REDSTONE_BLOCK : Material.STONE_BRICKS;
                    mutable.getBlock().setType(targetBlock);
                }
                server.getScheduler().runTaskLater(inst, this::randomizeLevers, 1);
            }, 15);
        } else {
            for (Location entry : leverBlocks.keySet()) {
                Location mutable = entry.clone();
                mutable.add(0, -2, 0);

                mutable.getBlock().setType(Material.STONE_BRICKS);
            }
        }
    }

    private void checkFloodType() {
        for (Player player : getContained(Player.class)) {
            if (!players.contains(player.getName())) continue;
            if (ItemUtil.findItemOfName(player.getInventory().getContents(), CustomItems.PHANTOM_HYMN.toString())) {
                drainAll(); // Force away all water
                floodBlockType = Material.LAVA;
                break;
            }
        }
    }

    private void resetFloodType() {
        floodBlockType = Material.WATER;
    }

    private long lastFlood = System.currentTimeMillis();

    private void flood() {

        final int playerMod = Math.max(1, players.size() / 2);
        if (System.currentTimeMillis() - startTime >= TimeUnit.SECONDS.toMillis((3 * 60) / playerMod)) {

            for (Location floodBlock : floodBlocks) {
                floodBlock.getBlock().setType(floodBlockType);
            }

            if (System.currentTimeMillis() - lastFlood >= TimeUnit.SECONDS.toMillis(30 / Math.max(1, playerMod))) {
                BlockVector3 min = roomTwo.getMinimumPoint();
                BlockVector3 max = roomTwo.getMaximumPoint();

                int minX = min.getBlockX();
                int minZ = min.getBlockZ();
                int minY = min.getBlockY();
                int maxX = max.getBlockX();
                int maxZ = max.getBlockZ();
                int maxY = max.getBlockY();

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            Block block = getWorld().getBlockAt(x, y, z);
                            if (block.getType() == Material.AIR) {
                                block.setType(floodBlockType, false);
                                break;
                            }
                        }
                    }
                }
                lastFlood = System.currentTimeMillis();
            }
        }
    }

    private void setDoor(ProtectedRegion door, Material type) {
        RegionWalker.walk(door, (x, y, z) -> {
            Block block = getWorld().getBlockAt(x, y, z);
            block.setType(type);
        });
    }

    private void drainAll() {
        RegionWalker.walk(roomTwo, (x, y, z) -> {
            Block block = getWorld().getBlockAt(x, y, z);
            if (EnvironmentUtil.isLiquid(block.getType())) {
                block.setType(Material.AIR);
            }
        });
    }

    private void killAll() {

        getContained(Player.class).stream()
                .filter(player -> !lobby.contains(toBlockVec3(player.getLocation())))
                .forEach(player -> player.setHealth(0));
    }

    public void start() {

        startTime = System.currentTimeMillis(); // Reset start clock
        leversTriggered = false;                // Reset lever check
        forceRestoreBlocks();                   // Reset blocks
        clearFloor();                           // Clean floor
        populateChest();                        // Add content

    }

    public int moveLobby() {

        long timeSpent = System.currentTimeMillis() - startTime;
        if (timeSpent > TimeUnit.MINUTES.toMillis(7)) killAll();
        if (!players.isEmpty()) return (int) ((TimeUnit.MINUTES.toMillis(7) - timeSpent) / 1000);
        for (Player aPlayer : server.getOnlinePlayers()) {

            if (LocationUtil.isInRegion(getWorld(), lobby, aPlayer)) {
                try {
                    playerStateComponent.pushState(PlayerStateKind.GOLD_RUSH, aPlayer);

                    // Teleport
                    Location location;
                    do {
                        location = LocationUtil.pickLocation(
                                getWorld(), roomOne.getMinimumPoint().getBlockY() + 1,
                                roomOne.getMinimumPoint(), roomOne.getMaximumPoint()
                        );
                    } while (location.getBlock().getType() != Material.AIR);
                    aPlayer.teleport(location, PlayerTeleportEvent.TeleportCause.UNKNOWN);

                    // Reset vitals
                    aPlayer.setHealth(20);
                    aPlayer.setFoodLevel(20);
                    aPlayer.setSaturation(5F);
                    aPlayer.setExhaustion(0F);

                    // Add
                    players.add(aPlayer.getName());

                    guildComponent.getState(aPlayer).ifPresent(GuildState::disablePowers);

                    // Partner talk
                    ChatUtil.sendWarning(aPlayer, "[Partner] I've disabled the security systems for now.");
                    ChatUtil.sendWarning(aPlayer, "[Partner] For your sake kid I hope you can move quickly.");
                } catch (IOException | ConflictingPlayerStateException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!players.isEmpty()) {
            lootSplit = 0;
            for (String player : players) {
                lootSplit += Math.max(ChanceUtil.getRangedRandom(11.52, 34.56), economy.getBalance(player) * .0015);
            }
            lootSplit /= players.size();
            if (ChanceUtil.getChance(35)) lootSplit *= 10;
            if (ChanceUtil.getChance(15)) lootSplit *= 2;
            if (ModifierComponent.getModifierCenter().isActive(ModifierType.QUAD_GOLD_RUSH)) lootSplit *= 4;
            start(); // Start if someone was teleported
        }
        return 0;
    }

    @Override
    public void run() {
        if (!LocationUtil.containsPlayer(getWorld(), getRegion())) return;

        equalize();
        if (checkKeys()) {
            if (LocationUtil.containsPlayer(getWorld(), roomOne)) {
                setDoor(doorOne, Material.AIR);
            } else {
                setDoor(doorOne, Material.IRON_BLOCK);
                if (checkLevers() || leversTriggered) {
                    drainAll();
                    setDoor(doorTwo, Material.AIR);
                } else {
                    randomizeLevers();
                    checkFloodType();
                    flood();
                }
            }
        }
    }

    @Override
    public void disable() {

        forceRestoreBlocks();
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        if (players.contains(event.getPlayer().getName()) || (contains(event.getTo()) && !(contains(event.getFrom())))) {

            if (event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)) return;

            event.setCancelled(true);
            ChatUtil.sendWarning(event.getPlayer(), "You cannot teleport to that location.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLocalSpawn(ApocalypseLocalSpawnEvent event) {

        if (players.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (event.getCause().getEffect().getType().isHoly() && players.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        if (players.contains(event.getPlayer().getName())) {
            ChatUtil.sendNotice(event.getPlayer(), ChatColor.DARK_RED, "Are you sure that's a good idea right now?");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (players.contains(event.getPlayer().getName())) {
            ChatUtil.sendNotice(event.getPlayer(), ChatColor.DARK_RED, "Are you sure that's a good idea right now?");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        BlockState state = event.getClickedBlock().getLocation().getBlock().getState();
        if (state.getType() == Material.STONE_BUTTON && lobby.contains(toBlockVec3(state.getBlock()))) {
            int waitingTime = moveLobby();
            if (waitingTime != 0) {
                ChatUtil.sendWarning(event.getPlayer(), "There is already a robbery in progress.");
                ChatUtil.sendWarning(event.getPlayer(), "The current robbery will end within: "
                        + waitingTime + " seconds.");
            }
        } else if (EnvironmentUtil.isSign(state.getType()) && locks.contains(state.getLocation())) {
            Sign sign = (Sign) state;
            if (sign.getLine(1).toLowerCase().contains("blue")) {
                if (event.getPlayer().getInventory().containsAtLeast(keys[0], 1)) {
                    event.getPlayer().getInventory().removeItem(keys[0]);
                    ((Sign) state).setLine(2, "Locked");
                    ((Sign) state).setLine(3, "- Unlocked -");
                    sign.update(true);

                    //noinspection deprecation
                    event.getPlayer().updateInventory();
                }
            } else if (sign.getLine(1).toLowerCase().contains("red")) {
                if (event.getPlayer().getInventory().containsAtLeast(keys[1], 1)) {
                    event.getPlayer().getInventory().removeItem(keys[1]);
                    ((Sign) state).setLine(2, "Locked");
                    ((Sign) state).setLine(3, "- Unlocked -");
                    sign.update(true);

                    //noinspection deprecation
                    event.getPlayer().updateInventory();
                }
            }
        } else if (EnvironmentUtil.isSign(state.getType())) {
            Sign sign = (Sign) state;
            if (sign.getLine(1).equals("Play Gold Rush")) {

                String playerName = event.getPlayer().getName();

                if (ItemUtil.countFilledSlots(event.getPlayer()) > 0) {
                    ChatUtil.sendError(event.getPlayer(), "[Partner] Don't bring anything with ya kid, it'll weigh you down.");
                    return;
                } else if (!economy.has(playerName, 100)) {
                    ChatUtil.sendError(event.getPlayer(), "[Partner] Kid you don't have enough cash, your balance will never pay bail.");
                    return;
                }

                Location location;
                do {
                    location = LocationUtil.pickLocation(
                            getWorld(), lobby.getMinimumPoint().getBlockY() + 1,
                            lobby.getMinimumPoint(), lobby.getMaximumPoint()
                    );
                } while (location.getBlock().getType() != Material.AIR);
                event.getPlayer().teleport(location);
                ChatUtil.sendNotice(event.getPlayer(), "[Partner] Ey there kid, just press that button over there to start.");
            }
        } else if (state.getType() == Material.LEVER && leverBlocks.containsKey(state.getLocation())) {
            server.getScheduler().runTaskLater(inst, () -> {
                if (checkLevers()) leversTriggered = true;
            }, 1);
        } else if (state.getType() == Material.CHEST && contains(state.getLocation())) {
            if (!players.contains(event.getPlayer().getName())) {
                event.setUseInteractedBlock(Event.Result.DENY);
                ChatUtil.sendWarning(event.getPlayer(), "[Partner] Thought you'd scam me huh kid?");
                ChatUtil.sendWarning(event.getPlayer(), "[Partner] Well I'll teach you kid!");
                ChatUtil.sendWarning(event.getPlayer(), "The alarm goes off.");
                event.getPlayer().setHealth(0);
                return;
            }
            if (rewardChest.equals(state.getLocation())) {
                try {
                    players.remove(event.getPlayer().getName());
                    event.setUseInteractedBlock(Event.Result.DENY);
                    event.getPlayer().teleport(LocationUtil.grandBank(getWorld()));

                    playerStateComponent.popState(PlayerStateKind.GOLD_RUSH, event.getPlayer());

                    highScoresComponent.update(event.getPlayer(), ScoreTypes.GOLD_RUSH_ROBBERIES, 1);
                    long duration = System.currentTimeMillis() - startTime;
                    highScoresComponent.update(event.getPlayer(), ScoreTypes.FASTEST_GOLD_RUSH, duration);

                    ChatUtil.sendNotice(event.getPlayer(), "You have successfully robbed the bank!\n");
                    ChatUtil.sendNotice(event.getPlayer(), "[Partner] I've put your split of the money in your account.");
                    ChatUtil.sendNotice(event.getPlayer(), "[Partner] Don't question my logic...\n");
                    ChatUtil.sendNotice(event.getPlayer(), "You obtain: "
                            + ChatUtil.makeCountString(ChatColor.YELLOW, economy.format(lootSplit), " as your split."));

                    economy.depositPlayer(event.getPlayer().getName(), lootSplit);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        final Player player = event.getPlayer();
        if (contains(player) || players.contains(player.getName())) {
            server.getScheduler().runTaskLater(inst, () -> {
                ChatUtil.sendWarning(player, "[Partner] Thought you'd scam me huh kid?");
                ChatUtil.sendWarning(player, "[Partner] Well I'll teach you kid!");
                ChatUtil.sendWarning(player, "The alarm goes off.");
                player.setHealth(0);
            }, 1);
        }
    }

    @EventHandler
    public void onSignPlace(SignChangeEvent event) {
        if (event.getLine(1).equals("Play Gold Rush")) {
            if (!event.getPlayer().hasPermission("aurora.goldrush.sign")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        String playerName = event.getEntity().getName();
        if (contains(event.getEntity()) && players.contains(playerName)) {
            double amt = Math.min(economy.getBalance(playerName), Math.max(100, economy.getBalance(playerName) * .005));
            economy.withdrawPlayer(playerName, amt);
            ChatUtil.sendWarning(event.getEntity(), "You are forced to pay a fine of: " + economy.format(amt) + '.');
            players.remove(playerName);
            String deathMessage;
            switch (ChanceUtil.getRandom(6)) {
                case 1:
                    deathMessage = " needs to find a new profession";
                    break;
                case 2:
                    deathMessage = " is now at the mercy of Hallow";
                    break;
                case 3:
                    deathMessage = " is now folding and hanging... though mostly hanging...";
                    break;
                case 4:
                    if (event.getDeathMessage().contains("drown")) {
                        deathMessage = " discovered H2O is not always good for ones health";
                        break;
                    }
                case 5:
                    if (event.getDeathMessage().contains("starved")) {
                        deathMessage = " should take note of the need to bring food with them";
                        break;
                    }
                default:
                    deathMessage = " was killed by police while attempting to rob a bank";
                    break;
            }
            event.setDeathMessage(playerName + deathMessage);
        }
    }

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}
