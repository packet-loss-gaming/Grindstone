/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.jungleraid;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitConfiguration;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.snapshot.SnapshotRestore;
import com.sk89q.worldedit.world.storage.ChunkStore;
import com.sk89q.worldedit.world.storage.MissingWorldException;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.anticheat.AntiCheatCompatibilityComponent;
import gg.packetloss.grindstone.city.engine.minigame.Win;
import gg.packetloss.grindstone.city.engine.minigame.WinType;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLightningStrikeSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.events.egg.EggDropEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePopEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.flag.BooleanFlagState;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.signwall.SignWall;
import gg.packetloss.grindstone.util.signwall.SignWallClickHandler;
import gg.packetloss.grindstone.util.signwall.enumname.EnumNameDataBackend;
import gg.packetloss.grindstone.util.signwall.enumname.EnumNamePainter;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagClickHandler;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagDataBackend;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagPainter;
import gg.packetloss.hackbook.ChunkBook;
import gg.packetloss.hackbook.ModifierBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.Door;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.item.ItemUtil.NO_ARMOR;

@ComponentInformation(friendlyName = "Jungle Raid", desc = "Warfare at it's best!")
@Depend(components = {AdminComponent.class, PrayerComponent.class}, plugins = {"WorldEdit", "WorldGuard"})
public class JungleRaidComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private ProtectedRegion region;
    private ProtectedRegion lobbyRegion;
    private World world;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private LocalConfiguration config;
    private static Economy economy = null;
    private List<BukkitTask> restorationTask = new ArrayList<>();

    private JungleRaidGameState gameState = new JungleRaidGameState();

    private JungleRaidState state = JungleRaidState.LOBBY;
    private long startTime;

    private Location lobbySpawnLocation;
    private Location lobbyExitLocation;

    private SignWall<JungleRaidClass> classWall;
    private Location classSelectionModeSign;

    private FlagEffectData flagData = new FlagEffectData();
    private BooleanFlagDataBackend<JungleRaidFlag> flagState = new BooleanFlagDataBackend<>(JungleRaidFlag.class);

    private JungleRaidClassSelectionMode classSelectionMode = JungleRaidClassSelectionMode.SELECTION;

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    PrayerComponent prayerComponent;
    @InjectComponent
    AntiCheatCompatibilityComponent antiCheat;
    @InjectComponent
    HighScoresComponent highScoresComponent;
    @InjectComponent
    PlayerStateComponent playerStateComponent;

    public JungleRaidState getState() {
        return state;
    }

    public long getStartTime() {
        return startTime;
    }

    private void applyClassEquipment(Player player) {
        player.getInventory().clear();

        if (classSelectionMode == JungleRaidClassSelectionMode.SURVIVAL) {
            return;
        }

        JungleRaidClass combatClass = getClassForPlayer(player).get();

        List<ItemStack> gear = new ArrayList<>();
        switch (combatClass) {
            case MELEE: {
                ItemStack enchantedSword = new ItemStack(Material.IRON_SWORD);
                ItemMeta meta = enchantedSword.getItemMeta();
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
                enchantedSword.setItemMeta(meta);

                gear.add(enchantedSword);

                ItemStack shield = new ItemStack(Material.SHIELD);
                gear.add(shield);
                break;
            }
            case LUMBERJACK: {
                ItemStack enchantedAxe = new ItemStack(Material.DIAMOND_AXE);
                ItemMeta meta = enchantedAxe.getItemMeta();
                meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true);
                meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
                enchantedAxe.setItemMeta(meta);

                gear.add(enchantedAxe);
                break;
            }
            case ARCHER: {
                ItemStack dmgBow = new ItemStack(Material.BOW);
                ItemMeta dmgBowMeta = dmgBow.getItemMeta();
                dmgBowMeta.addEnchant(Enchantment.ARROW_KNOCKBACK, 2, true);
                dmgBow.setItemMeta(dmgBowMeta);

                gear.add(dmgBow);

                ItemStack fireBow = new ItemStack(Material.BOW);
                ItemMeta fireBowMeta = fireBow.getItemMeta();
                fireBowMeta.addEnchant(Enchantment.ARROW_FIRE, 2, true);
                fireBow.setItemMeta(fireBowMeta);

                gear.add(fireBow);
                break;
            }
            case SNIPER: {
                ItemStack superBow = new ItemStack(Material.BOW);
                ItemMeta superBowMeta = superBow.getItemMeta();
                superBowMeta.addEnchant(Enchantment.ARROW_DAMAGE, 5, true);
                superBowMeta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
                superBow.setItemMeta(superBowMeta);

                superBow.setDurability((short) (superBow.getType().getMaxDurability() - combatClass.getArrowAmount()));

                gear.add(superBow);

                ItemStack woodSword = new ItemStack(Material.WOOD_SWORD);
                gear.add(woodSword);
                break;
            }
            case ENGINEER: {
                ItemStack ironSword = new ItemStack(Material.IRON_SWORD);
                gear.add(ironSword);

                ItemStack diamondPickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
                gear.add(diamondPickaxe);

                ItemStack shield = new ItemStack(Material.SHIELD);
                gear.add(shield);
                break;
            }
            case BALANCED: {
                ItemStack standardSword = new ItemStack(Material.IRON_SWORD);
                gear.add(standardSword);

                ItemStack standardBow = new ItemStack(Material.BOW);
                gear.add(standardBow);
                break;
            }
        }

        int tntAmt = combatClass.getTNTAmount();
        int tntStacks = tntAmt / 64;
        int tntRemainder = tntAmt % 64;
        for (int i = 0; i < tntStacks; ++i) {
            gear.add(new ItemStack(Material.TNT, 64));
        }
        if (tntRemainder > 0) {
            gear.add(new ItemStack(Material.TNT, tntRemainder));
        }

        if (combatClass.hasFlintAndSteel()) {
            gear.add(new ItemStack(Material.FLINT_AND_STEEL));
        }
        if (combatClass.hasShears()) {
            gear.add(new ItemStack(Material.SHEARS));
        }
        if (combatClass.hasAxe()) {
            try {
                gear.add(ModifierBook.cloneWithSpecifiedModifiers(
                        new ItemStack(Material.IRON_AXE),
                        Lists.newArrayList(
                                ModifierBook.ITEM_ATTACK_DAMAGE.get(
                                        1,
                                        ModifierBook.ModifierOperation.ADDITIVE,
                                        ModifierBook.Slot.MAIN_HAND
                                ),
                                ModifierBook.ITEM_ATTACK_SPEED.get(
                                        .9,
                                        ModifierBook.ModifierOperation.ADDITIVE,
                                        ModifierBook.Slot.MAIN_HAND
                                )
                        )
                ));
            } catch (UnsupportedFeatureException ex) {
                ex.printStackTrace();
            }
        }
        gear.add(new ItemStack(Material.COOKED_BEEF, 64));
        gear.add(new ItemStack(Material.COMPASS));

        int arrowAmt = combatClass.getArrowAmount();
        int arrowStacks = arrowAmt / 64;
        int arrowRemainder = arrowAmt % 64;
        for (int i = 0; i < arrowStacks; ++i) {
            gear.add(new ItemStack(Material.ARROW, 64));
        }
        if (arrowRemainder > 0) {
            gear.add(new ItemStack(Material.ARROW, arrowRemainder));
        }

        if (state == JungleRaidState.INITIALIZE && classSelectionMode == JungleRaidClassSelectionMode.SCAVENGER) {
            for (int i = 0; i < 3; ++i) {
                Block block = getRandomLocation().getBlock();
                block.setType(Material.CHEST);

                Chest state = (Chest) block.getState();
                for (ItemStack stack : gear) {
                    state.getBlockInventory().addItem(stack);
                }
            }
        } else {
            for (ItemStack stack : gear) {
                player.getInventory().addItem(stack);
            }
        }
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

    private void resetPlayerVitals(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExhaustion(0);
    }

    public void addToLobby(Player player) {
        try {
            playerStateComponent.pushState(PlayerStateKind.JUNGLE_RAID, player);

            GeneralPlayerUtil.takeFlightSafely(player);

            player.getInventory().clear();
            player.getInventory().setArmorContents(NO_ARMOR);

            resetPlayerVitals(player);

            gameState.addPlayer(player);
            applyClassEquipment(player);
        } catch (ConflictingPlayerStateException | IOException e) {
            e.printStackTrace();
        }
    }

    private void rewardPlayer(Player player, boolean won) {
        if (economy == null) {
            return;
        }

        double adjustedPoints = 1.5 * gameState.get(player).getPoints();
        double amt = adjustedPoints * (won ? 1 : .5);

        for (long i = (long) adjustedPoints; i > 50; i /= 50) {
            if (ChanceUtil.getChance(1000)) {
                ChatUtil.sendNotice(player, ChatColor.DARK_GREEN, "You found a rare jungle artifact!");
                amt += ChanceUtil.getRangedRandom(10000, 100000);
            } else if (ChanceUtil.getChance(10)) {
                ChatUtil.sendNotice(player, ChatColor.DARK_GREEN, "You found a jungle artifact!");
                amt += ChanceUtil.getRandom(10000);
            }
        }

        economy.depositPlayer(player, amt);
        ChatUtil.sendNotice(player, "You received: " + economy.format(amt) + '.');
    }

    public void died(Player player) {
        rewardPlayer(player, false);
        gameState.removePlayer(player);
    }

    private void removePlayer(Player player) {
        try {
            playerStateComponent.popState(PlayerStateKind.JUNGLE_RAID, player);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isFriendlyFire(Player attacker, Player defender) {
        JungleRaidProfile attackerProfile = gameState.get(attacker);
        JungleRaidProfile defenderProfile = gameState.get(defender);

        // One of the players is not in the game, don't consider this friendly fire.
        if (attackerProfile == null || defenderProfile == null) {
            return false;
        }

        JungleRaidTeam attackerTeam = attackerProfile.getTeam();
        JungleRaidTeam defenderTeam = defenderProfile.getTeam();

        // If either player is on the free for all team, there is no friendly fire.
        if (attackerTeam == JungleRaidTeam.FREE_FOR_ALL || defenderTeam == JungleRaidTeam.FREE_FOR_ALL) {
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

    public Collection<Player> getPlayersInArena() {
        return server.getOnlinePlayers().stream()
                .filter(p -> arenaContains(p.getLocation()))
                .collect(Collectors.toList());
    }

    public boolean lobbyContains(Location location) {
        return LocationUtil.isInRegion(world, lobbyRegion, location);
    }

    public boolean lobbyContains(Entity entity) {
        return lobbyContains(entity.getLocation());
    }

    public Collection<Player> getPlayersInLobby() {
        return server.getOnlinePlayers().stream()
                .filter(p -> lobbyContains(p.getLocation()))
                .collect(Collectors.toList());
    }

    public boolean anythingContains(Location location) {
        return arenaContains(location) || lobbyContains(location);
    }

    public boolean anythingContains(Entity entity) {
        return anythingContains(entity.getLocation());
    }

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        setupEconomy();
        server.getScheduler().runTaskLater(inst, this::setupRegionInfo, 1);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(new JungleRaidListener());

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    public void end() {
        for (Player player : Lists.newArrayList(gameState.getPlayers())) {
            removePlayer(player);
        }

        restore();
        flagData = new FlagEffectData();
    }

    private void setupRegionInfo() {
        world = Bukkit.getWorld(config.worldName);
        try {
            RegionManager manager = getWorldGuard().getGlobalRegionManager().get(world);
            region = manager.getRegion(config.region);
            lobbyRegion = manager.getRegion(config.region + "-lobby");
        } catch (UnknownPluginException e) {
            e.printStackTrace();
        }

        lobbySpawnLocation = new Location(world, -752, 81, -340, 180, 0);
        lobbyExitLocation = new Location(world, -752, 81, -336);

        classSelectionModeSign = new Location(world, -742, 83, -364);

        SignWall<BooleanFlagState<JungleRaidFlag>> flagWall = new SignWall<>(
                new Location(world, -766, 82, -364),
                BlockFace.EAST,
                7,
                new BooleanFlagPainter<>(),
                new BooleanFlagClickHandler<>(),
                flagState
        );
        flagWall.init();

        classWall = new SignWall<>(
                new Location(world, -745, 82, -364),
                BlockFace.EAST,
                7,
                new EnumNamePainter<>() {
                    private void paintSelectionMode(Sign targetSign) {
                        targetSign.setLine(1, classSelectionMode.name());
                        targetSign.update();
                    }

                    @Override
                    public void paintFirst(int numElementsPreceding, Sign targetSign) {
                        if (classSelectionMode.allowsSelection()) {
                            super.paintFirst(numElementsPreceding, targetSign);
                            return;
                        }

                        paintSelectionMode(targetSign);
                    }

                    @Override
                    public void paintLast(int numElementsSucceeding, Sign targetSign) {
                        if (classSelectionMode.allowsSelection()) {
                            super.paintLast(numElementsSucceeding, targetSign);
                            return;
                        }

                        paintSelectionMode(targetSign);
                    }

                    @Override
                    public void paint(JungleRaidClass value, Sign targetSign) {
                        if (classSelectionMode.allowsSelection()) {
                            super.paint(value, targetSign);
                            return;
                        }

                        paintSelectionMode(targetSign);
                    }
                },
                new SignWallClickHandler<>() {
                    @Override
                    public JungleRaidClass handleLeftClick(Player player, JungleRaidClass value) {
                        return tryUseClassSign(player, value);
                    }

                    @Override
                    public boolean allowNavigation() {
                        return classSelectionMode.allowsSelection();
                    }
                },
                new EnumNameDataBackend<>(JungleRaidClass.class)
        );
        classWall.init();

        classModeSignPopulate();
    }

    private void classModeSignPopulate() {
        Sign modeSign = (Sign) classSelectionModeSign.getBlock().getState();
        modeSign.setLine(1, "Mode");
        modeSign.setLine(2, ChatColor.BLUE + classSelectionMode.name());
        modeSign.update(true);
    }

    public void classModeSign() {
        JungleRaidClassSelectionMode[] modes = JungleRaidClassSelectionMode.values();
        classSelectionMode = modes[(classSelectionMode.ordinal() + 1) % modes.length];

        classModeSignPopulate();
        classWall.paint();
    }

    private void manualApplyClass(JungleRaidClass targetClass, Player player) {
        gameState.get(player).setCombatClass(targetClass);
        applyClassEquipment(player);
    }

    private JungleRaidClass tryUseClassSign(Player player, JungleRaidClass jungleRaidClass) {
        switch (classSelectionMode) {
            case SELECTION:
            case SCAVENGER:
                manualApplyClass(jungleRaidClass, player);
                break;
            case RANDOM:
            case SURVIVAL:
                break;
        }

        return jungleRaidClass;
    }

    public boolean isFlagEnabled(JungleRaidFlag flag) {
        return flagState.isEnabled(flag);
    }

    public boolean isSuddenDeath() {
        return !isFlagEnabled(JungleRaidFlag.NO_TIME_LIMIT) && System.currentTimeMillis() - getStartTime() >= TimeUnit.MINUTES.toMillis(15);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    @Override
    public void disable() {
        super.disable();
        stopRestore();
    }

    private boolean isTeamLastStanding(JungleRaidTeam currentTeam, int[] liveCounts) {
        // If this team has no players alive, short circuit.
        if (liveCounts[currentTeam.ordinal()] < 1) {
            return false;
        }

        for (JungleRaidTeam team : JungleRaidTeam.all()) {
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
        int[] liveCounts = new int[JungleRaidTeam.all().length];
        for (JungleRaidProfile profile : gameState.getProfiles()) {
            ++liveCounts[profile.getTeam().ordinal()];
        }

        // Check the FFA team first, it's the special case.
        int ffaCount = liveCounts[JungleRaidTeam.FREE_FOR_ALL.ordinal()];
        if (ffaCount == 1 && isTeamLastStanding(JungleRaidTeam.FREE_FOR_ALL, liveCounts)) {
            Player player = gameState.getPlayers().iterator().next();
            return Optional.of(new Win(player.getName(), WinType.SOLO));
        }

        for (JungleRaidTeam team : JungleRaidTeam.normal()) {
            if (isTeamLastStanding(team, liveCounts)) {
                return Optional.of(new Win(StringUtil.toTitleCase(team.name()), WinType.TEAM));
            }
        }

        return Optional.empty();
    }

    private void maybeDisableGuild(Player player) {
        boolean allowGuilds = isFlagEnabled(JungleRaidFlag.ALLOW_GUILDS);
        if (allowGuilds) {
            return;
        }

        adminComponent.deguildPlayer(player);
    }

    private void resetPlayerProperties(Player player) {
        resetPlayerVitals(player);

        player.getActivePotionEffects().clear();

        prayerComponent.uninfluencePlayer(player);
        maybeDisableGuild(player);
    }

    private void tryRandomAssignClass(Player player) {
        if (classSelectionMode != JungleRaidClassSelectionMode.RANDOM) {
            return;
        }

        JungleRaidClass playerClass = CollectionUtil.getElement(JungleRaidClass.values());
        gameState.get(player).setCombatClass(playerClass);

        ChatUtil.sendNotice(player, "You've been assigned the class:" + playerClass.name());
    }

    private void addPlayer(Player player, Supplier<Location> startingPos) {
        tryRandomAssignClass(player);

        applyClassEquipment(player);
        applyTeamEquipment(player);

        resetPlayerProperties(player);

        player.teleport(startingPos.get());
    }

    private void addPlayer(Player player) {
        addPlayer(player, this::getRandomLocation);
    }

    public Location getRandomLocation() {
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();

        while (true) {
            Location randomDest = new Location(
                    world,
                    ChanceUtil.getRangedRandom(min.getX(), max.getX()),
                    ChanceUtil.getRangedRandom(16, 80),
                    ChanceUtil.getRangedRandom(min.getZ(), max.getZ())
            );

            Location freePos = LocationUtil.findFreePosition(randomDest);
            if (freePos != null) {
                if (16 < freePos.getY() && freePos.getY() < 79) {
                    return freePos;
                }
            }
        }
    }

    private void enforceBounds() {
        for (Player player : gameState.getPlayers()) {
            if (!player.isValid()) continue;
            if (anythingContains(player)) continue;

            player.setHealth(0);
        }
    }

    public Optional<JungleRaidClass> getClassForPlayer(Player player) {
        JungleRaidProfile profile = gameState.get(player);
        if (profile == null) {
            return Optional.empty();
        }

        return Optional.of(profile.getCombatClass());
    }

    public Optional<Color> getTeamColorForPlayer(Player player) {
        JungleRaidProfile profile = gameState.get(player);
        if (profile == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(profile.getTeam().getColor());
    }

    public void adjustPoints(Player player, JungleRaidPointEvent event) {
        gameState.get(player).adjustPoints(event.getAdjustment());
        player.sendMessage(Text.of(
                ChatColor.DARK_GREEN,
                Text.of(ChatColor.GOLD, event.getCaption()),
                " +", Text.of(ChatColor.BLUE, event.getAdjustment()),
                " points!"
        ).build());
    }

    public void smartStart() {
        Collection<Player> containedPlayers = getPlayersInLobby();
        if (containedPlayers.size() <= 1) {
            return;
        }

        for (Player player : containedPlayers) {
            Block block = player.getLocation().add(0, -1, 0).getBlock();
            if (block.getType() != Material.CONCRETE) {
                return;
            }

            JungleRaidProfile profile = gameState.get(player);
            switch (block.getData()) {
                case 0:
                    profile.setTeam(JungleRaidTeam.FREE_FOR_ALL);
                    break;
                case 11:
                    profile.setTeam(JungleRaidTeam.BLUE);
                    break;
                case 14:
                    profile.setTeam(JungleRaidTeam.RED);
                    break;
                default:
                    return;
            }
        }

        if (getWinner().isPresent()) {
            for (JungleRaidProfile profile : gameState.getProfiles()) {
                profile.setTeam(null);
            }

            ChatUtil.sendError(getPlayersInLobby(), "All players are on one team, the game will not start.");
            return;
        }

        state = JungleRaidState.INITIALIZE;
        gameState.getPlayers().forEach(this::addPlayer);

        startTime = System.currentTimeMillis();
    }

    private void tryBeginCombat() {
        boolean cooldownPassed = System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(1);
        if (isFlagEnabled(JungleRaidFlag.NO_CHILL) || cooldownPassed) {
            state = JungleRaidState.IN_PROGRESS;
            ChatUtil.sendNotice(getPlayersInArena(), ChatColor.DARK_RED + "LET THE SLAUGHTER BEGIN!");
        }
    }

    private void handleWin(Player player) {
        highScoresComponent.update(player, ScoreTypes.JUNGLE_RAID_WINS, 1);

        // Give some final points for victory
        adjustPoints(player, JungleRaidPointEvent.GAME_WON);

        rewardPlayer(player, true);
    }

    private void processWin(Win win) {
        state = JungleRaidState.DONE;

        String rawWinMessage;
        switch (win.getWinType()) {
            case SOLO:
                rawWinMessage = win.getName() + " has won the jungle raid!";
                break;
            case TEAM:
                rawWinMessage = win.getName() + " team has won the jungle raid!";
                break;
            case DRAW:
                rawWinMessage = "The jungle raid was a draw!";
                break;
            default:
                return;
        }

        gameState.getPlayers().forEach(this::handleWin);

        Bukkit.broadcastMessage(ChatColor.GOLD + rawWinMessage);
    }

    @Override
    public void run() {
        if (state == JungleRaidState.RESTORING) {
            return;
        }

        enforceBounds();

        if (state == JungleRaidState.LOBBY) {
            smartStart();
            return;
        }

        if (state == JungleRaidState.INITIALIZE) {
            tryBeginCombat();
            return;
        }

        try {
            Optional<Win> optWin = getWinner();
            if (optWin.isPresent()) {
                processWin(optWin.get());
                end();
                return;
            }

            // Handle titian mode night vision
            for (Player player : getPlayersInArena()) {
                if (!player.isValid()) continue;

                if (isFlagEnabled(JungleRaidFlag.TITAN_MODE)) {
                    if (flagData.titan == null) {
                        flagData.titan = CollectionUtil.getElement(gameState.getPlayers()).getUniqueId();
                    }

                    if (player.getUniqueId().equals(flagData.titan)) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 20, 1, true));
                    }
                }
            }

            // Sudden death
            boolean suddenD = isSuddenDeath();
            if (suddenD) flagData.amt = 100;

            // Distributor
            if (isFlagEnabled(JungleRaidFlag.END_OF_DAYS) || isFlagEnabled(JungleRaidFlag.GRENADES) || isFlagEnabled(JungleRaidFlag.POTION_PLUMMET) || suddenD) {

                BlockVector bvMax = region.getMaximumPoint();
                BlockVector bvMin = region.getMinimumPoint();

                for (int i = 0; i < ChanceUtil.getRangedRandom(flagData.amt / 3, flagData.amt); i++) {

                    Vector v = LocationUtil.pickLocation(bvMin.getX(), bvMax.getX(),
                            bvMin.getZ(), bvMax.getZ()).add(0, bvMax.getY(), 0);
                    Location testLoc = new Location(world, v.getX(), v.getY(), v.getZ());

                    if (testLoc.getBlock().getTypeId() != BlockID.AIR) continue;

                    if (isFlagEnabled(JungleRaidFlag.END_OF_DAYS) || suddenD) {
                        TNTPrimed e = world.spawn(testLoc, TNTPrimed.class);
                        e.setVelocity(new org.bukkit.util.Vector(
                                random.nextDouble() * 2.0 - 1,
                                random.nextDouble() * 2 * -1,
                                random.nextDouble() * 2.0 - 1));
                        if (ChanceUtil.getChance(4)) e.setIsIncendiary(true);
                    }
                    if (isFlagEnabled(JungleRaidFlag.POTION_PLUMMET)) {
                        PotionType type = CollectionUtil.getElement(PotionType.values());
                        if (type == null || type == PotionType.WATER) {
                            i--;
                            continue;
                        }
                        for (int ii = ChanceUtil.getRandom(5); ii > 0; --ii) {
                            ThrownPotion potion = SplashPotionUtil.throwMaxedSplashPotion(testLoc, type);
                            potion.setVelocity(new org.bukkit.util.Vector(
                                    random.nextDouble() * 2.0 - 1,
                                    0,
                                    random.nextDouble() * 2.0 - 1));
                        }
                    }
                    if (isFlagEnabled(JungleRaidFlag.GRENADES)) {
                        testLoc.getWorld().dropItem(testLoc, new ItemStack(ItemID.SNOWBALL, ChanceUtil.getRandom(3)));
                    }
                }
                if (flagData.amt < 150 && ChanceUtil.getChance(isFlagEnabled(JungleRaidFlag.SUPER) ? 9 : 25)) ++flagData.amt;
            }

            // Random Rockets
            if (isFlagEnabled(JungleRaidFlag.RANDOM_ROCKETS)) {
                for (final Player player : getPlayersInArena()) {
                    if (!ChanceUtil.getChance(30)) continue;
                    for (int i = 0; i < 5; i++) {
                        server.getScheduler().runTaskLater(inst, () -> {
                            Location targetLocation = player.getLocation();
                            Firework firework = targetLocation.getWorld().spawn(targetLocation, Firework.class);
                            FireworkMeta meta = firework.getFireworkMeta();
                            FireworkEffect.Builder builder = FireworkEffect.builder();
                            builder.flicker(ChanceUtil.getChance(2));
                            builder.trail(ChanceUtil.getChance(2));
                            builder.withColor(Collections.singletonList(Color.RED));
                            builder.withFade(Collections.singletonList(Color.YELLOW));
                            builder.with(FireworkEffect.Type.BURST);
                            meta.addEffect(builder.build());
                            meta.setPower(ChanceUtil.getRangedRandom(2, 5));
                            firework.setFireworkMeta(meta);
                        }, i * 4);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(ChatColor.RED + "[WARNING] Jungle Raid logic failed to process.");
        }
    }

    public void stopRestore() {

        if (restorationTask.size() < 1) return;
        Bukkit.broadcastMessage(ChatColor.RED + "Jungle Arena restoration cancelled.");
        for (BukkitTask task : Collections.synchronizedList(restorationTask)) {

            task.cancel();
        }
        restorationTask.clear();
    }

    private void restore() {
        state = JungleRaidState.RESTORING;

        BukkitConfiguration worldEditConfig = null;
        try {
            worldEditConfig = getWorldEdit().getLocalConfiguration();
        } catch (UnknownPluginException e) {
            e.printStackTrace();
        }
        if ((worldEditConfig != null ? worldEditConfig.snapshotRepo : null) == null) {
            log.warning("No snapshots configured, restoration cancelled.");
            return;
        }

        try {
            // Discover chunks
            world.getEntitiesByClasses(Item.class, TNTPrimed.class).stream()
                    .filter(entity -> region.contains(BukkitUtil.toVector(entity.getLocation())))
                    .forEach(Entity::remove);

            final Snapshot snap = worldEditConfig.snapshotRepo.getDefaultSnapshot(config.worldName);

            if (snap == null) {
                log.warning("No snapshot could be found, restoration cancelled.");
                return;
            }

            final List<Chunk> chunkList = new ArrayList<>();

            Vector min;
            Vector max;

            try {
                min = region.getMinimumPoint();
                max = region.getMaximumPoint();
            } catch (NullPointerException ex) {
                return;
            }

            final int minX = min.getBlockX();
            final int minZ = min.getBlockZ();
            final int minY = min.getBlockY();
            final int maxX = max.getBlockX();
            final int maxZ = max.getBlockZ();
            final int maxY = max.getBlockY();

            Chunk c;
            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    c = world.getBlockAt(x, minY, z).getChunk();
                    if (!chunkList.contains(c)) chunkList.add(c);
                }
            }

            log.info("Snapshot '" + snap.getName() + "' loaded; now restoring Jungle Arena...");
            // Tell players restoration is beginning
            for (Player player : server.getOnlinePlayers()) {

                ChatUtil.sendWarning(player, "Restoring Jungle Arena...");
            }

            // Setup task to progressively restore
            final EditSession fakeEditor = WorldEdit.getInstance()
                    .getEditSessionFactory().getEditSession(new BukkitWorld(world), -1);

            ChunkStore chunkStore;

            try {
                chunkStore = snap.getChunkStore();
            } catch (DataException | IOException e) {
                log.warning("Failed to load snapshot: " + e.getMessage());
                return;
            }

            for (final Chunk chunk : chunkList) {
                BukkitTask aTask = server.getScheduler().runTaskLater(inst, () -> {
                    boolean isLastRestore = chunkList.indexOf(chunk) == chunkList.size() - 1;

                    try {
                        Block minBlock = chunk.getBlock(0, minY, 0);
                        Block maxBlock = chunk.getBlock(15, maxY, 15);
                        Vector minPt = new Vector(minBlock.getX(), minBlock.getY(), minBlock.getZ());
                        Vector maxPt = new Vector(maxBlock.getX(), maxBlock.getY(), maxBlock.getZ());

                        Region r = new CuboidRegion(minPt, maxPt);

                        // Restore snapshot
                        if (!chunk.isLoaded()) chunk.load();
                        SnapshotRestore restore = new SnapshotRestore(chunkStore, fakeEditor, r);

                        try {
                            restore.restore();
                            ChunkBook.relight(chunk);
                        } catch (MaxChangedBlocksException e) {
                            log.warning("Congratulations! You got an error which makes no sense!");
                            e.printStackTrace();
                            return;
                        } catch (UnsupportedFeatureException e) {
                            log.warning("Couldn't relight the chunk!");
                        }

                        if (restore.hadTotalFailure()) {
                            String error = restore.getLastErrorMessage();
                            if (error != null) {
                                log.warning("Errors prevented any blocks from being restored.");
                                log.warning("Last error: " + error);
                            } else {
                                log.warning("No chunks could be loaded. (Bad archive?)");
                            }
                        } else {
                            if (restore.getMissingChunks().size() > 0 || restore.getErrorChunks().size() > 0) {
                                log.info(String.format("Restored, %d missing chunks and %d other errors.",
                                        restore.getMissingChunks().size(),
                                        restore.getErrorChunks().size()));
                            }

                            if (isLastRestore) {
                                state = JungleRaidState.LOBBY;

                                for (Player player : lobbyExitLocation.getNearbyEntitiesByType(Player.class, 20)) {
                                    ChatUtil.sendNotice(player, "Jungle Raid restored successfully.");
                                }
                            }
                        }
                    } finally {
                        if (isLastRestore) {
                            try {
                                chunkStore.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }, 5 * chunkList.indexOf(chunk));
                restorationTask.add(aTask);
            }

            // Setup a task to clear out any restoration task
            server.getScheduler().runTaskLater(inst, restorationTask::clear, (5 * chunkList.size()) + 20);
        } catch (MissingWorldException e) {
            log.warning("The world: " + config.worldName + " could not be found, restoration cancelled.");
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("start.World")
        public String worldName = "City";
        @Setting("region")
        public String region = "carpe-diem-district-jungle-raid";

    }

    private static EDBEExtractor<Player, Player, Projectile> extractor = new EDBEExtractor<>(
            Player.class,
            Player.class,
            Projectile.class
    );

    private class JungleRaidListener implements Listener {

        private final String[] guildCmdList = new String[]{
                "ninja", "rogue", "unninja", "derogue"
        };
        private final String[] cmdWhiteList = new String[]{
                "ar", "jr", "stopweather", "me", "say", "pm", "msg", "message", "whisper", "tell",
                "reply", "r", "mute", "unmute", "debug", "dropclear", "dc", "auth", "toggleeditwand"
        };

        @EventHandler(ignoreCancelled = true)
        public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            if (anythingContains(player)) {
                String command = event.getMessage();
                boolean allowed = false;
                for (String cmd : cmdWhiteList) {
                    if (command.toLowerCase().startsWith("/" + cmd)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed && isFlagEnabled(JungleRaidFlag.ALLOW_GUILDS)) {
                    for (String cmd : guildCmdList) {
                        if (command.toLowerCase().startsWith("/" + cmd)) {
                            allowed = true;
                            break;
                        }
                    }
                }
                if (!allowed) {
                    ChatUtil.sendError(player, "Command blocked.");
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onItemDrop(PlayerDropItemEvent event) {
            if (anythingContains(event.getPlayer()) && state != JungleRaidState.IN_PROGRESS) event.setCancelled(true);
        }

        private boolean attemptToDoLobbyAction(Player player) {
            if (state != JungleRaidState.LOBBY) {
                if (state == JungleRaidState.RESTORING) {
                    ChatUtil.sendError(player, "The jungle raid arena is restoring, please wait.");
                } else {
                    ChatUtil.sendError(player, "A jungle raid is in progress, please wait.");
                }

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
            Player player = event.getPlayer();
            if (attemptToDoLobbyAction(player)) {
                return;
            }

            Block block = event.getClickedBlock();
            Location blockLoc = block.getLocation();

            if (blockLoc.equals(classSelectionModeSign)) {
                classModeSign();
            }
        }

        private void handleLobbyOnClick(PlayerInteractEvent event) {
            Block block = event.getClickedBlock();

            if (block.getState().getData() instanceof Door) {
                handleLobbyDoorClick(event);
            } else if (block.getType() == Material.WALL_SIGN) {
                handleLobbySigns(event);
            }
        }

        private void handleArenaOnClick(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            ItemStack stack = player.getItemInHand();

            if (stack.getType() == Material.COMPASS) {
                if (state == JungleRaidState.IN_PROGRESS && isFlagEnabled(JungleRaidFlag.ENHANCED_COMPASS)) {
                    Set<String> resultSet = new HashSet<>();
                    for (Player aPlayer : gameState.getPlayers()) {
                        // Check validity
                        if (!player.isValid() || player.equals(aPlayer)) continue;

                        // Check team
                        if (isFriendlyFire(player, aPlayer)) continue;

                        ChatColor color = player.hasLineOfSight(aPlayer) ? ChatColor.DARK_RED : ChatColor.RED;

                        resultSet.add(color + aPlayer.getName() + " - " + player.getLocation().distance(aPlayer.getLocation()));
                    }

                    if (resultSet.isEmpty()) {
                        ChatUtil.sendNotice(player, "No players found.");
                    }

                    ChatUtil.sendNotice(player, "Player - Distance");
                    for (String string : resultSet) {
                        ChatUtil.sendNotice(player, string);
                    }
                } else if (state == JungleRaidState.INITIALIZE) {
                    player.teleport(getRandomLocation());
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onClick(PlayerInteractEvent event) {
            Player player = event.getPlayer();

            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && lobbyContains(clickedBlock.getLocation())) {
                handleLobbyOnClick(event);
            } else if (arenaContains(player)) {
                handleArenaOnClick(event);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamageEvent(EntityDamageEvent event) {

            Entity e = event.getEntity();

            if (!(e instanceof Player)) return;

            Player player = (Player) e;

            if (arenaContains(player)) {
                if (!gameState.containsPlayer(player)) {
                    player.teleport(player.getWorld().getSpawnLocation());
                    event.setCancelled(true);
                    return;
                }

                switch (event.getCause()) {
                    case FALL:
                        if (LocationUtil.getBelowID(e.getLocation(), BlockID.LEAVES)
                                || (isFlagEnabled(JungleRaidFlag.SUPER) && isFlagEnabled(JungleRaidFlag.TRAMPOLINE))) {
                            server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                            server.getPluginManager().callEvent(new FallBlockerEvent(player));
                            if (ChanceUtil.getChance(2) || isFlagEnabled(JungleRaidFlag.TRAMPOLINE)) {
                                org.bukkit.util.Vector v = player.getLocation().getDirection();
                                v.setY(0);
                                v.setX(v.getX() > 0 ? -.5 : .5);
                                v.setZ(v.getZ() > 0 ? -.5 : .5);
                                player.setVelocity(new org.bukkit.util.Vector(0, .1, 0).multiply(event.getDamage()).add(v));
                            }
                            event.setCancelled(true);
                        }
                        break;
                    case BLOCK_EXPLOSION:
                        if (!(event instanceof EntityDamageByEntityEvent)) {
                            event.setDamage(Math.min(event.getDamage(), 2));
                        }
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

            CombatantPair<Player, Player, Projectile> result = extractor.extractFrom(event);

            if (result == null) return;

            Player attackingPlayer = result.getAttacker();
            Player defendingPlayer = result.getDefender();

            if (!arenaContains(attackingPlayer)) {
                return;
            }

            if (isFriendlyFire(attackingPlayer, defendingPlayer)) {
                event.setCancelled(true);
                ChatUtil.sendWarning(attackingPlayer, "Don't hit your team mates!");
            } else {
                Projectile projectile = result.getProjectile();
                if (projectile != null) {
                    double distSq = attackingPlayer.getLocation().distanceSquared(
                            defendingPlayer.getLocation()
                    );

                    if (getClassForPlayer(attackingPlayer).orElse(JungleRaidClass.BALANCED) == JungleRaidClass.SNIPER) {
                        double targetDistSq = Math.pow(70, 2);
                        double ratio = Math.min(distSq, targetDistSq) / targetDistSq;

                        // Handle damage modification
                        event.setDamage(event.getDamage() * ratio);
                    }

                    double epicLongShotDist = Math.pow(150, 2);
                    double longShotDist = Math.pow(50, 2);
                    if (distSq > epicLongShotDist) {
                        adjustPoints(attackingPlayer, JungleRaidPointEvent.EPIC_LONG_SHOT);
                    } else if (distSq > longShotDist) {
                        adjustPoints(attackingPlayer, JungleRaidPointEvent.LONG_SHOT);
                    }
                }

                if (isFlagEnabled(JungleRaidFlag.TITAN_MODE) && attackingPlayer.getUniqueId().equals(flagData.titan)) {
                    if (event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                        event.setDamage(event.getDamage() * 2);
                    }
                }

                if (isFlagEnabled(JungleRaidFlag.DEATH_TOUCH)) {
                    event.setDamage(Math.pow(defendingPlayer.getMaxHealth(), 3));
                    ChatUtil.sendNotice(attackingPlayer, "You've killed " + defendingPlayer.getName() + "!");
                } else {
                    ChatUtil.sendNotice(attackingPlayer, "You've hit " + defendingPlayer.getName() + "!");
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent event) {

            final Player player = event.getEntity();
            if (gameState.containsPlayer(player)) {
                Optional<Color> optTeamColor = getTeamColorForPlayer(player);

                // Enable disabled Checks
                boolean isTitanEnabled = isFlagEnabled(JungleRaidFlag.TITAN_MODE);
                boolean isTitan = player.getUniqueId().equals(flagData.titan);

                if (isTitanEnabled && isTitan) {
                    // Player aPlayer = Bukkit.getPlayerExact(titan);
                    // antiCheat.unexempt(aPlayer, CheckType.FAST_BREAK);
                    // antiCheat.unexempt(aPlayer, CheckType.NO_SWING);
                    // antiCheat.unexempt(aPlayer, CheckType.AUTOTOOL);
                }

                // Normal Jungle Raid fireworks and stuff
                Color killerColor = Color.WHITE;
                Player killer = player.getKiller();
                if (killer != null) {
                    Optional<Color> optKillerColor = getTeamColorForPlayer(killer);
                    if (optKillerColor.isPresent() && killer.isValid()) {
                        adjustPoints(killer, JungleRaidPointEvent.PLAYER_KILL);
                        if (isTitanEnabled) {
                            if (isTitan) {
                                flagData.titan = killer.getUniqueId();

                                adjustPoints(killer, JungleRaidPointEvent.TITAN_KILLED);
                                try {
                                    // Player aPlayer = Bukkit.getPlayerExact(titan);
                                    // antiCheat.exempt(aPlayer, CheckType.FAST_BREAK);
                                    // antiCheat.exempt(aPlayer, CheckType.NO_SWING);
                                    // antiCheat.exempt(aPlayer, CheckType.AUTOTOOL);
                                } catch (Exception ex) {
                                    ChatUtil.sendNotice(getPlayersInArena(), ChatColor.RED, "[ERROR] Cannot find titan.");
                                }
                            } else if (killer.getUniqueId().equals(flagData.titan)) {
                                killerColor = Color.BLACK;
                            }
                        }
                    }
                }

                final List<Color> colors = Collections.singletonList(optTeamColor.orElse(Color.WHITE));
                final List<Color> fades = Collections.singletonList(killerColor);

                final Location playerLoc = player.getLocation().clone();

                for (int i = 0; i < 12; i++) {
                    server.getScheduler().runTaskLater(inst, () -> {
                        Firework firework = world.spawn(playerLoc, Firework.class);
                        FireworkMeta meta = firework.getFireworkMeta();
                        FireworkEffect.Builder builder = FireworkEffect.builder();
                        builder.flicker(ChanceUtil.getChance(2));
                        builder.trail(ChanceUtil.getChance(2));
                        builder.withColor(colors);
                        builder.withFade(fades);
                        meta.addEffect(builder.build());
                        meta.setPower(ChanceUtil.getRangedRandom(2, 5));
                        firework.setFireworkMeta(meta);
                    }, i * 4);
                }

                if (killer != null) {
                    if (killerColor == Color.BLACK) {
                        event.setDeathMessage(player.getName() + " has been taken out by the titan");
                    } else {
                        event.setDeathMessage(player.getName() + " has been taken out by " + killer.getName());
                    }
                } else {
                    event.setDeathMessage(player.getName() + " is out");
                }
                event.getDrops().clear();
                event.setDroppedExp(0);

                died(player);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockDamage(BlockDamageEvent event) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (isFlagEnabled(JungleRaidFlag.TITAN_MODE) && arenaContains(block.getLocation())) {
                if (player.getUniqueId().equals(flagData.titan) && block.getTypeId() != BlockID.BEDROCK) {
                    event.setInstaBreak(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {

            Player player = event.getPlayer();

            if (arenaContains(player)) {
                if (isFlagEnabled(JungleRaidFlag.NO_BLOCK_BREAK)) {
                    ChatUtil.sendError(player, "You cannot break blocks by hand this game.");
                    event.setCancelled(true);
                } else if (isFlagEnabled(JungleRaidFlag.NO_MINING)) {
                    if (BlockType.isNaturalTerrainBlock(event.getBlock().getTypeId())) {
                        ChatUtil.sendError(player, "You cannot mine this game.");
                        event.setCancelled(true);
                    }
                }
            }
        }

        @EventHandler
        public void onFireSpread(BlockIgniteEvent event) {

            Location l = event.getBlock().getLocation();

            if (arenaContains(l) && (state == JungleRaidState.DONE || isFlagEnabled(JungleRaidFlag.NO_FIRE_SPREAD))) {
                switch (event.getCause()) {
                    case SPREAD:
                        event.setCancelled(true);
                        break;
                }
            }
        }

        private boolean handleExplosion(List<Block> blockList) {
            for (Block block : blockList) {
                if (arenaContains(block.getLocation())) {
                    return true;
                }
            }
            return false;
        }

        @EventHandler
        public void onEntityExplode(EntityExplodeEvent event) {
            if (handleExplosion(event.blockList())) {
                event.setYield(0);
            }
        }

        @EventHandler
        public void onBlockExplode(BlockExplodeEvent event) {
            if (handleExplosion(event.blockList())) {
                event.setYield(0);
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
            if (anythingContains(event.getLocation())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onEggDrop(EggDropEvent event) {
            if (arenaContains(event.getLocation())) event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            Player player = event.getPlayer();

            if (lobbyContains(event.getTo()) && !gameState.containsPlayer(player)) {
                event.setTo(lobbyExitLocation);
            }
        }

        @EventHandler
        public void onProjectileLand(ProjectileHitEvent event) {

            Projectile p = event.getEntity();
            if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
            if (arenaContains(p) && state == JungleRaidState.IN_PROGRESS) {
                int explosionSize = 2;
                Player shooter = (Player) p.getShooter();

                if (p instanceof Arrow) {
                    if (isFlagEnabled(JungleRaidFlag.TORMENT_ARROWS)) {
                        for (Entity e : p.getNearbyEntities(16, 16, 16)) {
                            if (e.equals(shooter)) continue;
                            if (e instanceof LivingEntity) {
                                ((LivingEntity) e).damage(1, p);
                                if (ChanceUtil.getChance(5)) {
                                    EntityUtil.heal(shooter, 1);
                                }
                            }
                        }
                    }

                    if (isFlagEnabled(JungleRaidFlag.EXPLOSIVE_ARROWS)) {
                        if (isFlagEnabled(JungleRaidFlag.SUPER)) explosionSize = 4;
                    } else return;
                }
                if (p instanceof Snowball) {
                    if (isFlagEnabled(JungleRaidFlag.GRENADES)) {
                        if (isFlagEnabled(JungleRaidFlag.SUPER)) explosionSize = 10;
                        else explosionSize = 6;
                    } else return;
                }

                ExplosionStateFactory.createPvPExplosion(
                        shooter,
                        p.getLocation(),
                        explosionSize,
                        false,
                        true
                );
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockChangePreLog(BlockChangePreLogEvent event) {
            if (arenaContains(event.getLocation())) event.setCancelled(true);
        }

        @EventHandler
        public void onPlayerStatePop(PlayerStatePopEvent event) {
            if (event.getKind() != PlayerStateKind.JUNGLE_RAID) {
                return;
            }

            Player player = event.getPlayer();

            gameState.removePlayer(player);
            prayerComponent.uninfluencePlayer(player);

            player.teleport(lobbyExitLocation);
        }
    }

    private WorldEditPlugin getWorldEdit() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldEdit");

        // WorldEdit may not be loaded
        if (!(plugin instanceof WorldEditPlugin)) {
            throw new UnknownPluginException("WorldEdit");
        }

        return (WorldEditPlugin) plugin;
    }

    private WorldGuardPlugin getWorldGuard() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }

        return (WorldGuardPlugin) plugin;
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