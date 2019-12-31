/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.skywars;

import com.destroystokyo.paper.Title;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.EconomyComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.anticheat.AntiCheatCompatibilityComponent;
import gg.packetloss.grindstone.city.engine.minigame.Win;
import gg.packetloss.grindstone.city.engine.minigame.WinType;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLightningStrikeSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePrePopEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.flag.BooleanFlagState;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.signwall.SignWall;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagClickHandler;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagDataBackend;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagPainter;
import gg.packetloss.grindstone.util.timer.CountdownTask;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.Door;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.item.ItemUtil.NO_ARMOR;


@ComponentInformation(friendlyName = "Sky Wars", desc = "Sky warfare at it's best!")
@Depend(components = {AdminComponent.class, PrayerComponent.class, PlayerStateComponent.class},
        plugins = {"WorldEdit", "WorldGuard"})
public class SkyWarsComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private ProtectedRegion region;
    private ProtectedRegion lobbyRegion;
    private World world;

    private LocalConfiguration config;

    private Location lobbySpawnLocation;
    private Location lobbyExitLocation;
    private Location gameStartLocation;

    private SkyWarsGameState gameState = new SkyWarsGameState();
    private BooleanFlagDataBackend<SkyWarsFlag> flagState = new BooleanFlagDataBackend<>(SkyWarsFlag.class);

    private SkyWarsState state = SkyWarsState.LOBBY;

    @InjectComponent
    private GuildComponent guilds;
    @InjectComponent
    private AntiCheatCompatibilityComponent antiCheat;
    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private PlayerStateComponent playerStateComponent;
    @InjectComponent
    private EconomyComponent economy;
    @InjectComponent
    private HighScoresComponent highScores;

    private void setupRegionInfo() {
        world = Bukkit.getWorld(config.worldName);
        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        region = manager.getRegion(config.region);
        lobbyRegion = manager.getRegion(config.region + "-lobby");

        lobbySpawnLocation = new Location(world, 464, 81, 109, 180, 0);
        lobbyExitLocation = new Location(world, 464, 81, 112.5);
        gameStartLocation = new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z);

        SignWall<BooleanFlagState<SkyWarsFlag>> flagWall = new SignWall<>(
                new Location(world, 460, 82, 77),
                BlockFace.EAST,
                8,
                new BooleanFlagPainter<>(),
                new BooleanFlagClickHandler<>(),
                flagState
        );
        flagWall.init();
    }

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        server.getScheduler().runTaskLater(inst, this::setupRegionInfo, 1);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(new SkyWarsListener());
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    public void adjustPoints(Player player, SkyWarsPointEvent event) {
        gameState.get(player).adjustPoints(event.getAdjustment());
        player.sendMessage(Text.of(
                ChatColor.DARK_GREEN,
                Text.of(ChatColor.GOLD, event.getCaption()),
                " +", Text.of(ChatColor.BLUE, event.getAdjustment()),
                " points!"
        ).build());
    }

    private void rewardPlayer(Player player, boolean won) {
        if (!economy.isEnabled()) {
            return;
        }

        double adjustedPoints = 1.5 * gameState.get(player).getPoints();
        double amt = adjustedPoints * (won ? 1 : .5);

        Economy economyHandle = economy.getHandle();
        economyHandle.depositPlayer(player, amt);
        ChatUtil.sendNotice(player, "You received: " + economyHandle.format(amt) + '.');
    }

    public void died(Player player) {
        rewardPlayer(player, false);
        gameState.removePlayer(player);
    }

    private void removePlayer(Player player) {
        try {
            playerStateComponent.popState(PlayerStateKind.SKY_WARS, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isFriendlyFire(Player attacker, Player defender) {
        SkyWarsProfile attackerProfile = gameState.get(attacker);
        SkyWarsProfile defenderProfile = gameState.get(defender);

        // One of the players is not in the game, don't consider this friendly fire.
        if (attackerProfile == null || defenderProfile == null) {
            return false;
        }

        SkyWarsTeam attackerTeam = attackerProfile.getTeam();
        SkyWarsTeam defenderTeam = defenderProfile.getTeam();

        // If either player does not have a team, there is no friendly fire.
        if (attackerTeam == null || defenderTeam == null) {
            return false;
        }

        // If either player is on the free for all team, there is no friendly fire.
        if (attackerTeam == SkyWarsTeam.FREE_FOR_ALL || defenderTeam == SkyWarsTeam.FREE_FOR_ALL) {
            return false;
        }

        // Otherwise, there is friendly fire if the teams match.
        return attackerTeam == defenderTeam;
    }

    public boolean arenaContains(Location location) {
        return LocationUtil.isInRegion(world, region, location);
    }

    public boolean arenaContains(Entity entity) {
        return arenaContains(entity.getLocation());
    }

    public Collection<Player> getPlayersInGame() {
        return gameState.getPlayers();
    }

    public boolean lobbyContains(Location location) {
        return LocationUtil.isInRegion(world, lobbyRegion, location);
    }

    public boolean lobbyContains(Entity entity) {
        return lobbyContains(entity.getLocation());
    }

    public boolean anythingContains(Location location) {
        return arenaContains(location) || lobbyContains(location);
    }

    public boolean anythingContains(Entity entity) {
        return anythingContains(entity.getLocation());
    }

    private void launchPlayer(Player player, double mod) {
        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
        player.setVelocity(new Vector(0, 3.5, 0).multiply(mod));
    }

    private boolean isTeamLastStanding(SkyWarsTeam currentTeam, int[] liveCounts) {
        // If this team has no players alive, short circuit.
        if (liveCounts[currentTeam.ordinal()] < 1) {
            return false;
        }

        for (SkyWarsTeam team : SkyWarsTeam.all()) {
            if (team == currentTeam) {
                continue;
            }

            if (liveCounts[team.ordinal()] > 0) {
                return false;
            }
        }

        return true;
    }

    public Optional<Win> getWinner() {
        // Do a quick check to see if everyone is dead.
        if (gameState.hasParticipants()) {
            return Optional.of(new Win(null, WinType.DRAW));
        }

        // Count the live counts per team.
        int[] liveCounts = new int[SkyWarsTeam.all().length];
        for (SkyWarsProfile profile : gameState.getProfiles()) {
            ++liveCounts[profile.getTeam().ordinal()];
        }

        // Check the FFA team first, it's the special case.
        int ffaCount = liveCounts[SkyWarsTeam.FREE_FOR_ALL.ordinal()];
        if (ffaCount == 1 && isTeamLastStanding(SkyWarsTeam.FREE_FOR_ALL, liveCounts)) {
            Player player = gameState.getPlayers().iterator().next();
            return Optional.of(new Win(player.getName(), WinType.SOLO));
        }

        for (SkyWarsTeam team : SkyWarsTeam.normal()) {
            if (isTeamLastStanding(team, liveCounts)) {
                return Optional.of(new Win(StringUtil.toTitleCase(team.name()), WinType.TEAM));
            }
        }

        return Optional.empty();
    }

    private void initFightCountdown() {
        CountdownTask fightCountdown = new CountdownTask() {
            @Override
            public boolean matchesFilter(int times) {
                return true;
            }

            @Override
            public void performStep(int times) {
                for (Player player : getPlayersInGame()) {
                    player.sendTitle(Title.builder().title(Text.of(ChatColor.DARK_RED, times).build()).build());
                }
            }

            @Override
            public void performFinal() {
                state = SkyWarsState.IN_PROGRESS;

                Collection<Player> players = getPlayersInGame();

                for (Player player : players) {
                    player.sendTitle(Title.builder().title(Text.of(ChatColor.DARK_GREEN, "FIGHT!").build()).build());
                }

                for (Player player : players) {
                    launchPlayer(player, 1);
                    sessions.getSession(SkyWarSession.class, player).stopPushBack();
                }

                editStartingPad(BlockTypes.AIR);
            }
        };

        TimedRunnable countdown = new TimedRunnable(fightCountdown, 3);
        BukkitTask task = server.getScheduler().runTaskTimer(inst, countdown, 0, 20);
        countdown.setTask(task);
    }

    public void smartStart() {
        Collection<Player> containedPlayers = getPlayersInGame();
        if (containedPlayers.size() <= 1) {
            return;
        }

        boolean allReady = true;

        for (Player player : containedPlayers) {
            Block block = player.getLocation().add(0, -1, 0).getBlock();

            SkyWarsProfile profile = gameState.get(player);
            if (!EnvironmentUtil.isStainedGlassBlock(block)) {
                profile.setTeam(null);
                allReady = false;
                continue;
            }

            switch (block.getData()) {
                case 0:
                    profile.setTeam(SkyWarsTeam.FREE_FOR_ALL);
                    break;
                case 1:
                    profile.setTeam(SkyWarsTeam.ORANGE);
                    break;
                case 4:
                    profile.setTeam(SkyWarsTeam.YELLOW);
                    break;
                case 5:
                    profile.setTeam(SkyWarsTeam.LIME);
                    break;
                case 7:
                    profile.setTeam(SkyWarsTeam.GRAY);
                    break;
                case 10:
                    profile.setTeam(SkyWarsTeam.PURPLE);
                    break;
                case 11:
                    profile.setTeam(SkyWarsTeam.BLUE);
                    break;
                case 13:
                    profile.setTeam(SkyWarsTeam.GREEN);
                    break;
                case 14:
                    profile.setTeam(SkyWarsTeam.RED);
                    break;
                case 15:
                    profile.setTeam(SkyWarsTeam.BLACK);
                    break;
                default:
                    break;
            }
        }

        if (!allReady) {
            return;
        }

        if (getWinner().isPresent()) {
            ChatUtil.sendError(getPlayersInGame(), "All players are on one team, the game will not start.");
            return;
        }

        if (gameStartLocation.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR)) {
            editStartingPad(BlockTypes.BLACK_STAINED_GLASS);
        }

        state = SkyWarsState.INITIALIZE;
        gameState.getPlayers().forEach(this::addPlayer);

        initFightCountdown();
    }

    private void handleWin(Player player) {
        highScores.update(player, ScoreTypes.SKY_WARS_WINS, 1);

        // Give some final points for victory
        adjustPoints(player, SkyWarsPointEvent.GAME_WON);

        rewardPlayer(player, true);
    }

    private void processWin(Win win) {
        state = SkyWarsState.DONE;

        String rawWinMessage;
        switch (win.getWinType()) {
            case SOLO:
                rawWinMessage = win.getName() + " has won the sky war!";
                break;
            case TEAM:
                rawWinMessage = win.getName() + " team has won the sky war!";
                break;
            case DRAW:
                rawWinMessage = "The sky war was a draw!";
                break;
            default:
                return;
        }

        gameState.getPlayers().forEach(this::handleWin);

        Bukkit.broadcastMessage(ChatColor.GOLD + rawWinMessage);
    }

    public void end() {
        for (Player player : Lists.newArrayList(gameState.getPlayers())) {
            removePlayer(player);
        }

        for (Entity chicken : getContainedChickens()) {
            chicken.remove();
        }

        state = SkyWarsState.LOBBY;
    }

    private void applyPlayerEquipment(Player player) {
        player.getInventory().clear();

        List<ItemStack> gear = new ArrayList<>();

        int uses = isFlagEnabled(SkyWarsFlag.SIXTY_FOUR_CLICK) ? 64 : -1;

        if (isFlagEnabled(SkyWarsFlag.DOOM)) {
            gear.add(makeDoomFeather(uses));
        } else {
            gear.add(makeStandardFeather(uses));
        }

        player.getInventory().addItem(gear.toArray(new ItemStack[0]));
    }

    public Optional<Color> getTeamColorForPlayer(Player player) {
        SkyWarsProfile profile = gameState.get(player);
        if (profile == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(profile.getTeam().getColor());
    }

    private void applyTeamEquipment(Player player) {
        Color teamColor = getTeamColorForPlayer(player).get();

        ItemStack[] leatherArmour = ItemUtil.LEATHER_ARMOR;

        LeatherArmorMeta helmMeta = (LeatherArmorMeta) leatherArmour[3].getItemMeta();
        helmMeta.setDisplayName(ChatColor.WHITE + "Team Hood");
        helmMeta.setColor(teamColor);
        leatherArmour[3].setItemMeta(helmMeta);

        LeatherArmorMeta chestMeta = (LeatherArmorMeta) leatherArmour[2].getItemMeta();
        chestMeta.setDisplayName(ChatColor.WHITE + "Team Chestplate");
        chestMeta.setColor(teamColor);
        leatherArmour[2].setItemMeta(chestMeta);

        LeatherArmorMeta legMeta = (LeatherArmorMeta) leatherArmour[1].getItemMeta();
        legMeta.setDisplayName(ChatColor.WHITE + "Team Leggings");
        legMeta.setColor(teamColor);
        leatherArmour[1].setItemMeta(legMeta);

        LeatherArmorMeta bootMeta = (LeatherArmorMeta) leatherArmour[0].getItemMeta();
        bootMeta.setDisplayName(ChatColor.WHITE + "Team Boots");
        bootMeta.setColor(teamColor);
        leatherArmour[0].setItemMeta(bootMeta);

        player.getInventory().setArmorContents(leatherArmour);
    }

    public void addToLobby(Player player) {
        try {
            playerStateComponent.pushState(PlayerStateKind.SKY_WARS, player);

            GeneralPlayerUtil.takeFlightSafely(player);
            guilds.getState(player).ifPresent(GuildState::disablePowers);

            // prayerComponent.uninfluencePlayer(player);

            player.getInventory().clear();
            player.getInventory().setArmorContents(NO_ARMOR);

            player.getActivePotionEffects().clear();

            gameState.addPlayer(player);

            // Delay by a tick to prevent leaping at the door
            server.getScheduler().runTaskLater(inst, () -> {
                if (lobbyContains(player) && gameState.containsPlayer(player)) {
                    applyPlayerEquipment(player);
                }
            }, 1);
        } catch (ConflictingPlayerStateException | IOException e) {
            e.printStackTrace();
        }
    }

    private void resetPlayerProperties(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExhaustion(0);
    }

    private void addPlayer(Player player) {
        applyPlayerEquipment(player);
        applyTeamEquipment(player);

        resetPlayerProperties(player);

        player.teleport(gameStartLocation);
    }

    private void edit(BlockType toType, Location battleLoc) {
        battleLoc = battleLoc.clone().add(0, -1, 0);

        EditSession editor = WorldEditBridge.getSystemEditSessionFor(world);
        BlockVector3 origin = WorldEditBridge.toBlockVec3(battleLoc);

        try {
            editor.makeCylinder(origin, toType.getDefaultState(), 12, 1, true);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    private void editStartingPad(BlockType toType) {
        edit(toType, gameStartLocation);
    }

    private ItemStack makeBookOfOmens() {
        ItemStack powerup = new ItemStack(Material.BOOK);

        ItemMeta powerMeta = powerup.getItemMeta();
        powerMeta.setDisplayName(ChatColor.WHITE + "Book o' Omens");
        powerup.setItemMeta(powerMeta);

        return powerup;
    }

    private ItemStack makeFrostOrb() {
        ItemStack powerup = new ItemStack(Material.SNOWBALL, 16);

        ItemMeta powerMeta = powerup.getItemMeta();
        powerMeta.setDisplayName(ChatColor.BLUE + "Frost Orb");
        powerup.setItemMeta(powerMeta);

        return powerup;
    }

    private ItemStack makeFlightFeather() {
        int uses = 5;
        double radius = 3;
        double flight = 6;
        double pushBack = 4;

        return makeSkyFeather("Flight", uses, radius, flight ,pushBack);
    }

    private ItemStack makePushBackFeather() {
        int uses = 5;
        double radius = 5;
        double flight = 2;
        double pushBack = 6;

        return makeSkyFeather("Push Back", uses, radius, flight ,pushBack);
    }

    private ItemStack makeStandardFeather(int uses) {
        double radius = 3;
        double flight = 2;
        double pushBack = 2;

        return makeSkyFeather("Standard", uses, radius, flight, pushBack);
    }

    private ItemStack makeDoomFeather(int uses) {
        double radius = 7;
        double flight = 6;
        double pushBack = 6;

        return makeSkyFeather(uses == -1 ? "Infinite Doom" : "Doom", uses, radius, flight, pushBack);
    }

    private void awardPowerup(Player player, ItemStack held) {
        ItemStack powerup;

        if (ChanceUtil.getChance(12)) {
            if (ChanceUtil.getChance(2)) {
                powerup = makeBookOfOmens();
            } else {
                powerup = makeFrostOrb();
            }
        } else if (ChanceUtil.getChance(5)) {
            powerup = new ItemStack(Material.CLOCK);
            ItemMeta powerMeta = powerup.getItemMeta();
            powerMeta.setDisplayName(ChatColor.GOLD + "Defroster");
            powerup.setItemMeta(powerMeta);
        } else {
            if (ItemUtil.matchesFilter(held, ChatColor.AQUA + "Sky Feather [Infinite Doom]", false)) return;

            if (ChanceUtil.getChance(50)) {
                for (Player aPlayer : getPlayersInGame()) {
                    if (player.equals(aPlayer)) continue;
                    ChatUtil.sendWarning(aPlayer, player.getName() + " has been given an Infinite Doom feather!");
                }

                // FIXME: This should really only remove feathers
                player.getInventory().clear();

                powerup = makeDoomFeather(-1);
            } else if (ChanceUtil.getChance(2)) {
                powerup = makeFlightFeather();
            } else {
                powerup = makePushBackFeather();
            }
        }

        player.getInventory().addItem(powerup);
        //noinspection deprecation
        player.updateInventory();

        // Display name doesn't need checked as all power ups have one assigned
        ChatUtil.sendNotice(player, "You obtain a power-up: "
                + powerup.getItemMeta().getDisplayName() + ChatColor.YELLOW + "!");
    }

    private void decrementUses(final Player player, ItemStack itemStack, int uses) {
        if (uses == -1) return;

        final ItemStack newStack;
        if (itemStack.getAmount() > 1) {
            newStack = itemStack.clone();
            newStack.setAmount(newStack.getAmount() - 1);
        } else {
            newStack = null;
        }

        server.getScheduler().runTaskLater(inst, () -> {
            player.getInventory().setItemInMainHand(newStack);

            //noinspection deprecation
            player.updateInventory();
        }, 1);
    }

    private ItemStack makeSkyFeather(String type, int uses, double radius, double flight, double pushBack) {
        boolean infinite = uses == -1;

        ItemStack baseStack = new ItemStack(Material.FEATHER, infinite ? 1 : uses);
        return modifySkyFeather(baseStack, type, infinite, radius, flight, pushBack);
    }

    private ItemStack modifySkyFeather(ItemStack skyFeather, String type, boolean infinite,
                                       double radius, double flight, double pushBack) {
        ItemMeta skyMeta = skyFeather.getItemMeta();

        skyMeta.setDisplayName(ChatColor.AQUA + "Sky Feather [" + type + "]");
        skyMeta.setLore(Arrays.asList(
                ChatColor.GOLD + "Uses: " + (infinite ? "Infinite": 1),
                ChatColor.GOLD + "Radius: " + radius,
                ChatColor.GOLD + "Flight: " + flight,
                ChatColor.GOLD + "Push Back: " + pushBack
        ));
        skyFeather.setItemMeta(skyMeta);
        return skyFeather;
    }

    public List<Entity> getContainedChickens() {
        List<Entity> chickens = new ArrayList<>();

        for (Entity entity : world.getEntitiesByClasses(Chicken.class)) {
            if (entity.isValid() && LocationUtil.isInRegion(region, entity)) {
                chickens.add(entity);
            }
        }

        return chickens;
    }

    public List<Entity> getContainedTargets() {
        List<Entity> returnedList = new ArrayList<>(getPlayersInGame());
        returnedList.addAll(getContainedChickens());
        return returnedList;
    }

    private void updatePlayers() {
        for (Player player : getPlayersInGame()) {
            player.setFoodLevel(20);
            player.setSaturation(5F);

            Location pLoc = player.getLocation();
            if (anythingContains(pLoc)) {
                if (EnvironmentUtil.isWater(pLoc.getBlock())) {
                    if (state == SkyWarsState.LOBBY || state == SkyWarsState.IN_PROGRESS) {
                        int damageModifier = isFlagEnabled(SkyWarsFlag.ACID_PLUS_PLUS) ? 3 : 1;
                        player.damage(ChanceUtil.getRandom(damageModifier * 3));
                    } else {
                        player.teleport(gameStartLocation);
                    }
                }
            } else {
                player.setHealth(0);
            }
        }
    }

    public boolean isFlagEnabled(SkyWarsFlag flag) {
        return state != SkyWarsState.LOBBY && flagState.isEnabled(flag);
    }

    @Override
    public void run() {
        updatePlayers();

        if (state == SkyWarsState.LOBBY) {
            smartStart();
            return;
        }

        if (state == SkyWarsState.INITIALIZE) {
            return;
        }

        try {
            Optional<Win> optWin = getWinner();
            if (optWin.isPresent()) {
                processWin(optWin.get());
                end();
                return;
            }

            RegionChecker checker = new RegionChecker(region);
            for (int i = 0; i < gameState.getPlayers().size(); ++i) {

                if (!ChanceUtil.getChance(4) && !isFlagEnabled(SkyWarsFlag.CHICKEN_PLUS_PLUS)) continue;

                Location target = LocationUtil.pickLocation(world, region.getMaximumPoint().getY() - 10, checker);
                Chicken c = world.spawn(target, Chicken.class);
                c.setRemoveWhenFarAway(true);
            }

            if (isFlagEnabled(SkyWarsFlag.REGEN_ENABLED) && ChanceUtil.getChance(5)) {
                for (Player player : gameState.getPlayers()) {
                    if (player.isValid()) {
                        EntityUtil.heal(player, 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(ChatColor.RED + "[WARNING] Sky Wars logic failed to process.");
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("start.World")
        public String worldName = "City";
        @Setting("start.X")
        public int x = 631;
        @Setting("start.Y")
        public int y = 81;
        @Setting("start.Z")
        public int z = 205;
        @Setting("region")
        public String region = "vineam-district-sky-wars";
        @Setting("cmd-whitelist")
        private List<String> commandWhitelist = List.of(
                "stopweather", "daylight", "me", "say", "pm", "msg", "message", "whisper",
                "tell", "reply", "r", "mute", "unmute", "debug", "dropclear", "dc"
        );
    }

    private static EDBEExtractor<Player, Player, Snowball> extractor = new EDBEExtractor<>(
            Player.class,
            Player.class,
            Snowball.class
    );

    private class SkyWarsListener implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            if (gameState.containsPlayer(player)) {
                new CommandBlocker(config.commandWhitelist).handle(event);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onGuildPowersEnable(GuildPowersEnableEvent event) {
            if (gameState.containsPlayer(event.getPlayer())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onItemDrop(PlayerDropItemEvent event) {
            Player player = event.getPlayer();
            if (gameState.containsPlayer(player)) {
                if (!arenaContains(player) || state != SkyWarsState.IN_PROGRESS) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onHealthRegain(EntityRegainHealthEvent event) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (gameState.containsPlayer(player)) {
                    event.setCancelled(true);
                }
            }
        }

        private boolean attemptToDoLobbyAction(Player player) {
            if (state != SkyWarsState.LOBBY) {
                ChatUtil.sendError(player, "A sky war is in progress, please wait.");
                return true;
            }

            return false;
        }

        private void handleLobbyDoorClick(PlayerInteractEvent event) {
            event.setCancelled(true);

            Player player = event.getPlayer();
            if (attemptToDoLobbyAction(player)) {
                return;
            }

            if (gameState.containsPlayer(player)) {
                removePlayer(player);
            } else {
                addToLobby(player);
                player.teleport(lobbySpawnLocation);
            }
        }

        private void handleLobbySigns(PlayerInteractEvent event) {
        }

        private void handleLobbyOnClick(PlayerInteractEvent event) {
            Block block = event.getClickedBlock();

            if (block.getState().getData() instanceof Door) {
                handleLobbyDoorClick(event);
            } else if (block.getType() == Material.WALL_SIGN) {
                handleLobbySigns(event);
            }
        }

        private void handleItemUse(PlayerInteractEvent event) {
            final Player player = event.getPlayer();
            ItemStack stack = player.getItemInHand();

            if (state != SkyWarsState.LOBBY && state != SkyWarsState.IN_PROGRESS) {
                return;
            }

            if (!gameState.containsPlayer(player)) {
                return;
            }

            SkyWarSession session = sessions.getSession(SkyWarSession.class, player);
            if (ItemUtil.matchesFilter(stack, ChatColor.AQUA + "Sky Feather")) {

                Vector vel = player.getLocation().getDirection();

                int uses = -1;
                double radius = 3;
                double flight = 2;
                double pushBack = 4;

                Map<String, String> map = ItemUtil.getItemTags(stack);

                String currentValue;

                currentValue = map.get(ChatColor.GOLD + "Uses");
                if (currentValue != null) {
                    try {
                        uses = Integer.parseInt(currentValue);
                    } catch (NumberFormatException ignored) {
                    }
                }

                currentValue = map.get(ChatColor.GOLD + "Radius");
                if (currentValue != null) {
                    try {
                        radius = Double.parseDouble(currentValue);
                    } catch (NumberFormatException ignored) {
                    }
                }

                currentValue = map.get(ChatColor.GOLD + "Flight");
                if (currentValue != null) {
                    try {
                        flight = Double.parseDouble(currentValue);
                    } catch (NumberFormatException ignored) {
                    }
                }

                currentValue = map.get(ChatColor.GOLD + "Push Back");
                if (currentValue != null) {
                    try {
                        pushBack = Double.parseDouble(currentValue);
                    } catch (NumberFormatException ignored) {
                    }
                }

                switch (event.getAction()) {
                    case LEFT_CLICK_AIR:
                    case LEFT_CLICK_BLOCK:

                        if (!session.canFly()) break;

                        vel.multiply(flight);

                        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                        player.setVelocity(vel);

                        session.stopFlight(250);

                        decrementUses(player, stack, uses);
                        break;
                    case RIGHT_CLICK_AIR:
                    case RIGHT_CLICK_BLOCK:

                        if (!session.canPushBack()) break;

                        vel.multiply(pushBack * 2);

                        BlockIterator it = new BlockIterator(player, 50);
                        Location k = new Location(null, 0, 0, 0);

                        List<Entity> targets = getContainedTargets();

                        while (it.hasNext()) {
                            Block block = it.next();

                            if (block.getType().isSolid()) {
                                break;
                            }

                            block.getWorld().playEffect(block.getLocation(k), Effect.MOBSPAWNER_FLAMES, 0);

                            for (Entity aEntity : targets) {
                                innerLoop:
                                {
                                    if (!aEntity.isValid() || aEntity.equals(player)) break innerLoop;

                                    if (aEntity.getLocation().distanceSquared(block.getLocation()) <= Math.pow(radius, 2)) {
                                        if (aEntity instanceof Player) {
                                            Player aPlayer = (Player) aEntity;

                                            if (isFriendlyFire(player, aPlayer)) break innerLoop;

                                            // Handle Sender
                                            session.stopPushBack(250);
                                            ChatUtil.sendNotice(player, "You push back: " + aPlayer.getName() + "!");

                                            // Handle Target
                                            server.getPluginManager().callEvent(new ThrowPlayerEvent(aPlayer));
                                            aPlayer.setVelocity(vel);
                                            if (isFlagEnabled(SkyWarsFlag.FLAMMABLE)) {
                                                aPlayer.setFireTicks((int) aPlayer.getHealth() * 20 * 2);
                                            }

                                            SkyWarSession aSession = sessions.getSession(SkyWarSession.class, aPlayer);
                                            if (aSession.canDefrost()) {
                                                aSession.stopFlight();
                                            }
                                        } else {
                                            awardPowerup(player, stack);
                                            aEntity.remove();
                                        }
                                    }
                                }
                            }
                        }
                        decrementUses(player, stack, uses);
                        break;
                }
            } else if (ItemUtil.matchesFilter(stack, ChatColor.WHITE + "Book o' Omens")) {

                if (!session.canUseOmen()) return;

                ChatUtil.sendNotice(player, "You used the Book o' Omens!");

                session.stopOmen();

                for (Player aPlayer : getPlayersInGame()) {
                    if (player.equals(aPlayer)) continue;
                    ChatUtil.sendWarning(aPlayer, player.getName() + " used the Book o' Omens!");
                    if (isFriendlyFire(player, aPlayer)) continue;
                    launchPlayer(aPlayer, -1);

                    SkyWarSession aSession = sessions.getSession(SkyWarSession.class, aPlayer);
                    if (aSession.canDefrost()) {
                        aSession.stopFlight();
                    }
                }

                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                    //noinspection deprecation
                    player.updateInventory();
                } else {
                    server.getScheduler().runTaskLater(inst, () -> {
                        player.setItemInHand(null);
                        //noinspection deprecation
                        player.updateInventory();
                    }, 1);
                }
            } else if (ItemUtil.matchesFilter(stack, ChatColor.GOLD + "Defroster")) {

                if (!session.canDefrost()) return;

                ChatUtil.sendNotice(player, "You used the Defroster!");

                session.stopDefrost();
                session.stopFlight(0);
                session.stopPushBack(0);

                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                    //noinspection deprecation
                    player.updateInventory();
                } else {
                    server.getScheduler().runTaskLater(inst, () -> {
                        player.setItemInHand(null);
                        //noinspection deprecation
                        player.updateInventory();
                    }, 1);
                }
            }
        }

        @EventHandler
        public void onClick(PlayerInteractEvent event) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && lobbyContains(clickedBlock.getLocation())) {
                handleLobbyOnClick(event);
            }

            handleItemUse(event);
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamageEvent(EntityDamageEvent event) {

            Entity e = event.getEntity();

            if (!(e instanceof Player)) return;

            Player player = (Player) e;

            if (gameState.containsPlayer(player)) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                    return;
                }

                if (state == SkyWarsState.INITIALIZE) {
                    event.setCancelled(true);

                    Entity attacker = ((EntityDamageByEntityEvent) event).getDamager();
                    if (attacker instanceof Projectile) {
                        ProjectileSource source = ((Projectile) attacker).getShooter();
                        if (source instanceof Entity) {
                            attacker = (Entity) source;
                        }
                    }
                    if (!(attacker instanceof Player)) return;
                    ChatUtil.sendError(attacker, "The game has not yet started!");
                } else if (state != SkyWarsState.IN_PROGRESS && player.getHealth() - event.getFinalDamage() < 1) {
                    event.setCancelled(true);

                    player.setHealth(player.getMaxHealth());
                    player.teleport(lobbySpawnLocation);
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

            CombatantPair<Player, Player, Snowball> result = extractor.extractFrom(event);

            if (result == null) return;

            Player attackingPlayer = result.getAttacker();
            Player defendingPlayer = result.getDefender();

            if (!gameState.containsPlayer(attackingPlayer)) {
                return;
            }

            if (isFriendlyFire(attackingPlayer, defendingPlayer)) {
                event.setCancelled(true);
                ChatUtil.sendWarning(attackingPlayer, "Don't hit your team mates!");
            } else {
                if (result.hasProjectile()) {
                    SkyWarSession session = sessions.getSession(SkyWarSession.class, defendingPlayer);
                    session.stopFlight(3000 + (1000 * ChanceUtil.getRandom(5)));
                    session.stopDefrost(15000);
                }
                ChatUtil.sendNotice(attackingPlayer, "You've hit " + defendingPlayer.getName() + "!");
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent event) {

            final Player player = event.getEntity();
            if (gameState.containsPlayer(player)) {
                Player killer = player.getKiller();

                if (killer != null) {
                    event.setDeathMessage(player.getName() + " has been taken out by " + killer.getName());
                } else {
                    event.setDeathMessage(player.getName() + " is out");
                }

                event.getDrops().clear();
                event.setDroppedExp(0);

                died(player);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();

            if (gameState.containsPlayer(player))  {
                gameState.removePlayer(player);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onApocalypseLightningStrikeSpawn(ApocalypseLightningStrikeSpawnEvent event) {
            if (anythingContains(event.getLocation())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onApocalypsePersonSpawn(ApocalypsePersonalSpawnEvent event) {
            Player player = event.getPlayer();
            if (gameState.containsPlayer(player)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onPlayerStatePrePop(PlayerStatePrePopEvent event) {
            if (event.getKind() != PlayerStateKind.SKY_WARS) {
                return;
            }

            Player player = event.getPlayer();

            gameState.removePlayer(player);

            player.setFallDistance(0);
            player.teleport(lobbyExitLocation);
        }
    }

    // Sky War Session
    private static class SkyWarSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private long nextFlight = 0;
        private long nextPushBack = 0;
        private long nextOmen = 0;
        private long nextDefrost = 0;

        protected SkyWarSession() {

            super(MAX_AGE);
        }

        public boolean canFly() {

            return nextFlight == 0 || System.currentTimeMillis() >= nextFlight;
        }

        public void stopFlight() {

            stopFlight(2250);
        }

        public void stopFlight(long time) {

            nextFlight = System.currentTimeMillis() + time;
        }

        public boolean canPushBack() {

            return nextPushBack == 0 || System.currentTimeMillis() >= nextPushBack;
        }

        public void stopPushBack() {

            stopPushBack(5000);
        }

        public void stopPushBack(long time) {

            nextPushBack = System.currentTimeMillis() + time;
        }

        public boolean canUseOmen() {

            return nextOmen == 0 || System.currentTimeMillis() >= nextOmen;
        }

        public void stopOmen() {

            stopOmen(1000);
        }

        public void stopOmen(long time) {

            nextOmen = System.currentTimeMillis() + time;
        }

        public boolean canDefrost() {

            return nextDefrost == 0 || System.currentTimeMillis() >= nextDefrost;
        }

        public void stopDefrost() {

            stopDefrost(1000);
        }

        public void stopDefrost(long time) {

            nextDefrost = System.currentTimeMillis() + time;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}