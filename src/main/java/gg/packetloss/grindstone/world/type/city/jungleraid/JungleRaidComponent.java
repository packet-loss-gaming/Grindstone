/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.jungleraid;

import com.destroystokyo.paper.Title;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitConfiguration;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.snapshot.SnapshotRestore;
import com.sk89q.worldedit.world.storage.ChunkStore;
import com.sk89q.worldedit.world.storage.MissingWorldException;
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
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseBlockDamagePreventionEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLightningStrikeSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.events.egg.EggDropEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePrePopEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.state.GuildState;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.flag.BooleanFlagState;
import gg.packetloss.grindstone.util.item.ItemModifierUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.signwall.SignWall;
import gg.packetloss.grindstone.util.signwall.SignWallClickHandler;
import gg.packetloss.grindstone.util.signwall.enumname.EnumNameDataBackend;
import gg.packetloss.grindstone.util.signwall.enumname.EnumNamePainter;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagClickHandler;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagDataBackend;
import gg.packetloss.grindstone.util.signwall.flag.BooleanFlagPainter;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.grindstone.world.type.city.minigame.Win;
import gg.packetloss.grindstone.world.type.city.minigame.WinType;
import org.bukkit.*;
import org.bukkit.attribute.AttributeModifier;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.Door;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.ProjectileWatchingComponent.getSpawningItem;
import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;
import static gg.packetloss.grindstone.util.item.ItemUtil.NO_ARMOR;

@ComponentInformation(friendlyName = "Jungle Raid", desc = "Warfare at it's best!")
@Depend(
    components = {GuildComponent.class, PrayerComponent.class, WalletComponent.class},
    plugins = {"WorldEdit", "WorldGuard"}
)
public class JungleRaidComponent extends BukkitComponent implements Runnable {
    private ProtectedRegion region;
    private ProtectedRegion lobbyRegion;
    private World world;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private LocalConfiguration config;
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
    GuildComponent guilds;
    @InjectComponent
    PrayerComponent prayerComponent;
    @InjectComponent
    HighScoresComponent highScoresComponent;
    @InjectComponent
    PlayerStateComponent playerStateComponent;
    @InjectComponent
    SpectatorComponent spectatorComponent;
    @InjectComponent
    WalletComponent walletComponent;
    @InjectComponent
    ChatBridgeComponent chatBridgeComponent;

    public JungleRaidState getState() {
        return state;
    }

    public long getStartTime() {
        return startTime;
    }

    private static final String LEGENDARY_JUNGLE_BOW = ChatColor.DARK_GREEN + "Legendary Jungle Bow";

    private void applyClassEquipment(Player player) {
        player.getInventory().clear();

        if (classSelectionMode == JungleRaidClassSelectionMode.SURVIVAL) {
            return;
        }

        JungleRaidClass combatClass = getClassForPlayer(player).orElseThrow();

        List<ItemStack> gear = new ArrayList<>();
        switch (combatClass) {
            case KNIGHT: {
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
                superBowMeta.setDisplayName(LEGENDARY_JUNGLE_BOW);
                superBowMeta.setLore(List.of(ChatColor.GOLD + "Imbued with unique powers for long ranged combat."));
                superBow.setItemMeta(superBowMeta);

                gear.add(superBow);

                ItemStack woodSword = new ItemStack(Material.WOODEN_SWORD);
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
            case BANDIT: {
                ItemStack standardSword = CustomItemCenter.build(CustomItems.IRON_SHORT_SWORD);
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
            gear.add(ItemModifierUtil.cloneWithSpecifiedModifiers(
                    new ItemStack(Material.IRON_AXE),
                    Lists.newArrayList(
                        ItemModifierUtil.ITEM_ATTACK_DAMAGE.get(
                            1,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlot.HAND
                        ),
                        ItemModifierUtil.ITEM_ATTACK_SPEED.get(
                            .9,
                            AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlot.HAND
                        )
                    )
            ));
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

        if (state == JungleRaidState.INITIALIZE_ARENA && classSelectionMode == JungleRaidClassSelectionMode.SCAVENGER) {
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

        ItemStack[] leatherArmour = ItemUtil.clone(ItemUtil.LEATHER_ARMOR);

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

        BigDecimal finalAmt = new BigDecimal(amt);
        walletComponent.addToBalance(player, finalAmt).thenAcceptAsynchronously(
            (newBalance) -> {
                ChatUtil.sendNotice(player, "You received: ", walletComponent.format(finalAmt), '.');
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
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
        return Bukkit.getOnlinePlayers().stream()
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
        return Bukkit.getOnlinePlayers().stream()
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

        spectatorComponent.registerSpectatorKind(PlayerStateKind.JUNGLE_RAID_SPECTATOR);
        setupRegionInfo();

        CommandBook.registerEvents(new JungleRaidListener());

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 10);
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

        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        region = manager.getRegion(config.region);
        lobbyRegion = manager.getRegion(config.region + "-lobby");

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

        spectatorComponent.registerSpectatedRegion(PlayerStateKind.JUNGLE_RAID_SPECTATOR, region);
        spectatorComponent.registerSpectatorSkull(
                PlayerStateKind.JUNGLE_RAID_SPECTATOR,
                new Location(world, -750, 82, -337),
                () -> gameState.hasParticipants()
        );
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
        if (!gameState.hasParticipants()) {
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

        guilds.getState(player).ifPresent(GuildState::disablePowers);
    }

    private void resetPlayerProperties(Player player) {
        resetPlayerVitals(player);

        player.getActivePotionEffects().clear();

        prayerComponent.clearPrayers(player);
        maybeDisableGuild(player);
    }

    private void tryRandomAssignClass(Player player) {
        if (classSelectionMode != JungleRaidClassSelectionMode.RANDOM) {
            return;
        }

        JungleRaidClass playerClass = CollectionUtil.getElement(JungleRaidClass.values());
        gameState.get(player).setCombatClass(playerClass);

        ChatUtil.sendNotice(player, "You've been assigned the class: " + playerClass.name());
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
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        while (true) {
            Location randomDest = LocationUtil.pickLocation(
                    world,
                    min.getX(), max.getX(),
                    16, 80,
                    min.getZ(), max.getZ()
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

    private Location getRandomOrePointAtLevel(int minY, int maxY) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        while (true) {
            Location randomDest = new Location(
                    world,
                    ChanceUtil.getRangedRandom(min.getX(), max.getX()),
                    ChanceUtil.getRangedRandom(minY, maxY),
                    ChanceUtil.getRangedRandom(min.getZ(), max.getZ())
            );


            if (randomDest.getBlock().getType() == Material.STONE) {
                return randomDest;
            }
        }
    }

    private static List<Material> SURVIVAL_ORES = List.of(
            Material.COAL_ORE, Material.IRON_ORE,
            Material.GOLD_ORE, Material.DIAMOND_ORE
    );

    private void populateOres() {
        final int minY = region.getMinimumPoint().getBlockY();
        final int maxY = region.getMaximumPoint().getBlockY();

        for (int i = 0; i < config.oreBandOreCount; ++i) {
            Block block = getRandomOrePointAtLevel(minY, maxY).getBlock();

            Material oreType = CollectionUtil.getElement(SURVIVAL_ORES);
            for (int ii = ChanceUtil.getRandom(10); ii > 0; --ii) {
                block.setType(oreType);

                block = block.getRelative(CollectionUtil.getElement(BlockFace.values()));
                if (block.getType() != Material.STONE || !arenaContains(block.getLocation())) {
                    break;
                }
            }
        }
    }

    private static List<EntityType> SURVIVAL_ANIMALS = List.of(
            EntityType.PIG, EntityType.COW, EntityType.SHEEP
    );

    private void populateAnimals() {
        for (int i = config.survivalAnimalCount; i > 0; --i) {
            Location loc = getRandomLocation();
            loc.getWorld().spawnEntity(loc, CollectionUtil.getElement(SURVIVAL_ANIMALS));
        }
    }

    private void populateSurvivalResource() {
        populateOres();
        populateAnimals();
    }

    public void smartStart() {
        Collection<Player> containedPlayers = getPlayersInLobby();
        if (containedPlayers.size() <= 1) {
            return;
        }

        for (Player player : containedPlayers) {
            Block block = player.getLocation().add(0, -1, 0).getBlock();
            if (!EnvironmentUtil.isConcrete(block)) {
                return;
            }

            JungleRaidProfile profile = gameState.get(player);
            switch (block.getType()) {
                case WHITE_CONCRETE:
                    profile.setTeam(JungleRaidTeam.FREE_FOR_ALL);
                    break;
                case BLUE_CONCRETE:
                    profile.setTeam(JungleRaidTeam.BLUE);
                    break;
                case RED_CONCRETE:
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

        state = JungleRaidState.INITIALIZE_ARENA;

        if (classSelectionMode == JungleRaidClassSelectionMode.SURVIVAL) {
            populateSurvivalResource();
        }

        gameState.getPlayers().forEach(this::addPlayer);

        startTime = System.currentTimeMillis();
    }

    private void tryBeginCombat() {
        boolean cooldownPassed = System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(1);
        if (isFlagEnabled(JungleRaidFlag.NO_CHILL) || cooldownPassed) {
            state = JungleRaidState.INITIALIZE_FIGHT;

            TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

            taskBuilder.setInterval(20);
            taskBuilder.setNumberOfRuns(3);

            taskBuilder.setAction((seconds) -> {
                ChatColor color;
                switch (seconds) {
                    case 3:
                        color = ChatColor.YELLOW;
                        break;
                    case 2:
                        color = ChatColor.RED;
                        break;
                    default:
                        color = ChatColor.DARK_RED;
                        break;
                }

                for (Player player : getPlayersInArena()) {
                    player.sendTitle(Title.builder().title(Text.of(color, seconds).build()).build());
                }

                return true;
            });
            taskBuilder.setFinishAction(() -> {
                if (state != JungleRaidState.INITIALIZE_FIGHT) {
                    return;
                }

                state = JungleRaidState.IN_PROGRESS;

                Collection<Player> players = getPlayersInArena();

                for (Player player : players) {
                    player.sendTitle(Title.builder().title(Text.of(
                            ChatColor.DARK_RED, "LET THE SLAUGHTER BEGIN!"
                    ).build()).build());
                }
            });

            taskBuilder.build();
        }
    }

    private void handleWin(Player player) {
        highScoresComponent.update(player, ScoreTypes.JUNGLE_RAID_WINS, BigInteger.ONE);

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
        chatBridgeComponent.broadcast(rawWinMessage);
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

        if (state == JungleRaidState.INITIALIZE_ARENA) {
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

                BlockVector3 bvMax = region.getMaximumPoint();
                BlockVector3 bvMin = region.getMinimumPoint();

                for (int i = 0; i < ChanceUtil.getRangedRandom(flagData.amt / 3, flagData.amt); i++) {

                    Location testLoc = LocationUtil.pickLocation(world, bvMax.getY(), bvMin, bvMax);

                    if (!EnvironmentUtil.isAirBlock(testLoc.getBlock())) {
                        continue;
                    }

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
                        testLoc.getWorld().dropItem(testLoc, new ItemStack(Material.SNOWBALL, ChanceUtil.getRandom(3)));
                    }
                }
                if (flagData.amt < 150 && ChanceUtil.getChance(isFlagEnabled(JungleRaidFlag.SUPER) ? 9 : 25)) ++flagData.amt;
            }

            // Random Rockets
            if (isFlagEnabled(JungleRaidFlag.RANDOM_ROCKETS)) {
                for (final Player player : getPlayersInArena()) {
                    if (!ChanceUtil.getChance(30)) continue;
                    for (int i = 0; i < 5; i++) {
                        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
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

        BukkitConfiguration worldEditConfig = WorldEditBridge.getLocalConfiguration();
        if ((worldEditConfig != null ? worldEditConfig.snapshotRepo : null) == null) {
            CommandBook.logger().warning("No snapshots configured, restoration cancelled.");
            return;
        }

        try {
            // Discover chunks
            world.getEntitiesByClasses(Item.class, TNTPrimed.class, Creature.class).stream()
                    .filter(entity -> region.contains(toBlockVec3(entity.getLocation())))
                    .forEach(Entity::remove);

            final Snapshot snap = worldEditConfig.snapshotRepo.getDefaultSnapshot(config.worldName);

            if (snap == null) {
                CommandBook.logger().warning("No snapshot could be found, restoration cancelled.");
                return;
            }

            final List<BlockVector2> chunkList = new ArrayList<>();

            BlockVector3 min;
            BlockVector3 max;

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

            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    BlockVector2 chunkCoords = BlockVector2.at(x >> 4, z >> 4);
                    if (!chunkList.contains(chunkCoords)) chunkList.add(chunkCoords);
                }
            }

            CommandBook.logger().info("Snapshot '" + snap.getName() + "' loaded; now restoring Jungle Arena...");
            // Tell players restoration is beginning
            for (Player player : Bukkit.getOnlinePlayers()) {

                ChatUtil.sendWarning(player, "Restoring Jungle Arena...");
            }

            // Setup task to progressively restore
            final EditSession fakeEditor = WorldEdit.getInstance()
                    .getEditSessionFactory().getEditSession(new BukkitWorld(world), -1);

            ChunkStore chunkStore;

            try {
                chunkStore = snap.getChunkStore();
            } catch (DataException | IOException e) {
                CommandBook.logger().warning("Failed to load snapshot: " + e.getMessage());
                return;
            }

            for (final BlockVector2 chunkCoords : chunkList) {
                BukkitTask aTask = Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                    boolean isLastRestore = chunkList.indexOf(chunkCoords) == chunkList.size() - 1;

                    try {
                        Chunk chunk = world.getChunkAt(chunkCoords.getX(), chunkCoords.getZ());
                        Block minBlock = chunk.getBlock(0, minY, 0);
                        Block maxBlock = chunk.getBlock(15, maxY, 15);
                        BlockVector3 minPt = BlockVector3.at(minBlock.getX(), minBlock.getY(), minBlock.getZ());
                        BlockVector3 maxPt = BlockVector3.at(maxBlock.getX(), maxBlock.getY(), maxBlock.getZ());

                        Region r = new CuboidRegion(minPt, maxPt);

                        // Restore snapshot
                        SnapshotRestore restore = new SnapshotRestore(chunkStore, fakeEditor, r);

                        try {
                            restore.restore();
                            fakeEditor.flushSession();
                        } catch (MaxChangedBlocksException e) {
                            CommandBook.logger().warning("Congratulations! You got an error which makes no sense!");
                            e.printStackTrace();
                            return;
                        }

                        if (restore.hadTotalFailure()) {
                            String error = restore.getLastErrorMessage();
                            if (error != null) {
                                CommandBook.logger().warning("Errors prevented any blocks from being restored.");
                                CommandBook.logger().warning("Last error: " + error);
                            } else {
                                CommandBook.logger().warning("No chunks could be loaded. (Bad archive?)");
                            }
                        } else {
                            if (restore.getMissingChunks().size() > 0 || restore.getErrorChunks().size() > 0) {
                                CommandBook.logger().info(
                                    String.format("Restored, %d missing chunks and %d other errors.",
                                    restore.getMissingChunks().size(),
                                    restore.getErrorChunks().size())
                                );
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
                }, 5 * chunkList.indexOf(chunkCoords));
                restorationTask.add(aTask);
            }

            // Setup a task to clear out any restoration task
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), restorationTask::clear, (5 * chunkList.size()) + 20);
        } catch (MissingWorldException e) {
            CommandBook.logger().warning("The world: " + config.worldName + " could not be found, restoration cancelled.");
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("start.World")
        public String worldName = "City";
        @Setting("region")
        public String region = "carpe-diem-district-jungle-raid";
        @Setting("survival.animal-count")
        private int survivalAnimalCount = 60;
        @Setting("survival.ore-band-ore-count")
        private int oreBandOreCount = 40;
        @Setting("cmd-whitelist")
        private List<String> commandWhitelist = List.of(
                "stopweather", "daylight", "guild", "deguild", "me", "say", "pm", "msg", "message", "whisper",
                "tell", "reply", "r", "mute", "unmute", "debug", "dropclear", "dc"
        );
    }

    private static EDBEExtractor<Player, Player, Projectile> extractor = new EDBEExtractor<>(
            Player.class,
            Player.class,
            Projectile.class
    );

    private class JungleRaidListener implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            if (gameState.containsPlayer(player)) {
                new CommandBlocker(config.commandWhitelist).handle(event);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onGuildPowersEnable(GuildPowersEnableEvent event) {
            if (gameState.containsPlayer(event.getPlayer()) && !isFlagEnabled(JungleRaidFlag.ALLOW_GUILDS)) {
                event.setCancelled(true);
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
            } else if (EnvironmentUtil.isSign(block)) {
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
                } else if (state == JungleRaidState.INITIALIZE_ARENA) {
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
                        if (LocationUtil.hasBelow(e.getLocation(), (type) -> type == Material.JUNGLE_LEAVES)
                                || (isFlagEnabled(JungleRaidFlag.SUPER) && isFlagEnabled(JungleRaidFlag.TRAMPOLINE))) {
                            CommandBook.callEvent(new ThrowPlayerEvent(player));
                            CommandBook.callEvent(new FallBlockerEvent(player));
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

            if (state != JungleRaidState.IN_PROGRESS) {
                ChatUtil.sendError(attackingPlayer, "The game has not started yet!");
                event.setCancelled(true);
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

                    getSpawningItem(projectile).ifPresent((launcher) -> {
                        if (ItemUtil.matchesFilter(launcher, LEGENDARY_JUNGLE_BOW)) {
                            double unitDistSq = Math.pow(25, 2);
                            for (int i = (int) (distSq / unitDistSq); i > 0; --i) {
                                DeathUtil.throwSlashPotion(defendingPlayer.getLocation());
                            }
                        }
                    });

                    double epicLongShotDist = Math.pow(75, 2);
                    double longShotDist = Math.pow(25, 2);
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
                    Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
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

                // Allow some drops to be kept if in Scavenger mode and the player dies inside the arena
                if (arenaContains(player) && classSelectionMode == JungleRaidClassSelectionMode.SCAVENGER) {
                    event.getDrops().removeIf((item) -> ItemUtil.isLeatherArmorPiece(item.getType()));
                } else {
                    event.getDrops().clear();
                }
                event.setDroppedExp(0);

                died(player);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockDamage(BlockDamageEvent event) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (isFlagEnabled(JungleRaidFlag.TITAN_MODE) && arenaContains(block.getLocation())) {
                if (player.getUniqueId().equals(flagData.titan) && block.getType() != Material.BEDROCK) {
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
                    if (EnvironmentUtil.isNaturalTerrainBlock(event.getBlock())) {
                        ChatUtil.sendError(player, "You cannot mine this game.");
                        event.setCancelled(true);
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
        public void onApocalypseBlockDamage(ApocalypseBlockDamagePreventionEvent event) {
            if (arenaContains(event.getPlayer())) {
                event.setCancelled(true);
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
        public void onPlayerStatePush(PlayerStatePushEvent event) {
            if (event.getKind() != PlayerStateKind.JUNGLE_RAID_SPECTATOR) {
                return;
            }

            Player player = event.getPlayer();
            player.teleport(getRandomLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }

        @EventHandler
        public void onPlayerStatePrePop(PlayerStatePrePopEvent event) {
            if (event.getKind() != PlayerStateKind.JUNGLE_RAID) {
                return;
            }

            Player player = event.getPlayer();

            gameState.removePlayer(player);
            prayerComponent.clearPrayers(player);
            player.getActivePotionEffects().clear();

            player.teleport(lobbyExitLocation);
        }
    }
}