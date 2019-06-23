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
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.anticheat.AntiCheatCompatibilityComponent;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLightningStrikeSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.events.egg.EggDropEvent;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.player.PlayerState;
import gg.packetloss.hackbook.ChunkBook;
import gg.packetloss.hackbook.ModifierBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.Door;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    protected final String workingDir;

    private ProtectedRegion region;
    private ProtectedRegion lobbyRegion;
    private World world;

    private final Random random = new Random();
    private LocalConfiguration config;
    private static Economy economy = null;
    private static final double BASE_AMT = 1.2;
    private List<BukkitTask> restorationTask = new ArrayList<>();

    private List<Player> participants = new ArrayList<>();

    private Map<Player, Set<Player>> teamMapping = new HashMap<>();
    private Set<Player> freeForAllPlayers = new HashSet<>();
    private Set<Player> blueTeamPlayers = new HashSet<>();
    private Set<Player> redTeamPlayers = new HashSet<>();
    private Map<Player, JungleRaidClass> classMap = new HashMap<>();

    private ConcurrentHashMap<UUID, PlayerState> playerState = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, PlayerState> goneState = new ConcurrentHashMap<>();

    private JungleRaidState state = JungleRaidState.LOBBY;
    private long startTime;

    private Location lobbySpawnLocation;
    private Location lobbyExitLocation;
    private Location leftFlagActivationSign;
    private Location rightFlagActivationSign;
    private List<Location> scrollingFlagSigns = new ArrayList<>();

    private Location leftClassActivationSign;
    private Location rightClassActivationSign;
    private List<Location> scrollingClassSigns = new ArrayList<>();

    private int signScrollFlagStart;
    private int signScrollClassStart;

    private FlagEffectData flagData = new FlagEffectData();
    private boolean[] flagState = new boolean[JungleRaidFlag.values().length];

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    PrayerComponent prayerComponent;
    @InjectComponent
    AntiCheatCompatibilityComponent antiCheat;

    public JungleRaidComponent() {
        this.workingDir = inst.getDataFolder().getPath() + "/minigames/jr/";
    }

    public JungleRaidState getState() {
        return state;
    }

    public long getStartTime() {
        return startTime;
    }

    private void giveBaseEquipment(Player player, JungleRaidClass jrClass) {
        player.getInventory().clear();

        List<ItemStack> gear = new ArrayList<>();
        switch (jrClass) {
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

                superBow.setDurability((short) (superBow.getType().getMaxDurability() - jrClass.getArrowAmount()));

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

        int tntAmt = jrClass.getTNTAmount();
        int tntStacks = tntAmt / 64;
        int tntRemainder = tntAmt % 64;
        for (int i = 0; i < tntStacks; ++i) {
            gear.add(new ItemStack(Material.TNT, 64));
        }
        if (tntRemainder > 0) {
            gear.add(new ItemStack(Material.TNT, tntRemainder));
        }

        if (jrClass.hasFlintAndSteel()) {
            gear.add(new ItemStack(Material.FLINT_AND_STEEL));
        }
        if (jrClass.hasShears()) {
            gear.add(new ItemStack(Material.SHEARS));
        }
        if (jrClass.hasAxe()) {
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

        int arrowAmt = jrClass.getArrowAmount();
        int arrowStacks = arrowAmt / 64;
        int arrowRemainder = arrowAmt % 64;
        for (int i = 0; i < arrowStacks; ++i) {
            gear.add(new ItemStack(Material.ARROW, 64));
        }
        if (arrowRemainder > 0) {
            gear.add(new ItemStack(Material.ARROW, arrowRemainder));
        }

        for (ItemStack stack : gear) {
            player.getInventory().addItem(stack);
        }
    }

    private void giveTeamEquipment(Player player, Color teamColor) {
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

    public void restore(Player player) {
        prayerComponent.uninfluencePlayer(player);
    }

    private void handleTeamLeave(Player player) {
        Set<Player> teamPlayers = teamMapping.remove(player);
        if (teamPlayers != null) {
            teamPlayers.remove(player);
        }
    }

    protected void restore(Player player, PlayerState state) {

        // Clear Player
        player.getInventory().clear();
        player.getInventory().setArmorContents(NO_ARMOR);

        // Teleport Player
        Location returnLoc = state.getLocation();
        if (returnLoc != null) {
            player.teleport(returnLoc);
        }

        // Restore the contents
        player.getInventory().setArmorContents(state.getArmourContents());
        player.getInventory().setContents(state.getInventoryContents());
        player.setHealth(Math.min(player.getMaxHealth(), state.getHealth()));
        player.setFoodLevel(state.getHunger());
        player.setSaturation(state.getSaturation());
        player.setExhaustion(state.getExhaustion());
        player.setLevel(state.getLevel());
        player.setExp(state.getExperience());
        player.updateInventory();

        if (player.getVehicle() != null) {
            player.getVehicle().eject();
        }
    }

    public void addToLobby(Player player) {
        GeneralPlayerUtil.takeFlightSafely(player);

        PlayerState state = GeneralPlayerUtil.makeComplexState(player);
        state.setOwnerName(player.getName());
        state.setLocation(player.getLocation());
        playerState.put(player.getUniqueId(), state);

        player.getInventory().clear();
        player.getInventory().setArmorContents(NO_ARMOR);

        giveBaseEquipment(player, getClassForPlayer(player));

        participants.add(player);

        writeData();
    }

    private void restorePresentPlayer(Player player) {
        restore(player, playerState.get(player.getUniqueId()));
        playerState.remove(player.getUniqueId());
        writeData();
    }

    public void left(Player player) {
        PlayerState state = playerState.get(player.getUniqueId());
        goneState.put(player.getUniqueId(), state);
        playerState.remove(player.getUniqueId());

        handleTeamLeave(player);

        writeData();
    }

    public void removeFromLobby(Player player) {
        if (!playerState.containsKey(player.getUniqueId())) return;

        restorePresentPlayer(player);
        handleTeamLeave(player);
    }

    public void removeFromTeam(Player player, boolean forced) {
        if (!playerState.containsKey(player.getUniqueId())) return;

        restorePresentPlayer(player);
        handleTeamLeave(player);

        if (economy != null && forced && state != JungleRaidState.LOBBY) {
            payPlayer(player, state == JungleRaidState.DONE ? 10 : 1);
        }
    }

    public void removeGoneFromTeam(Player player, boolean forced) {
        if (!goneState.containsKey(player.getUniqueId())) return;

        restore(player, goneState.get(player.getUniqueId()));
        goneState.remove(player.getUniqueId());
        writeData();

        if (economy != null && forced) {
            payPlayer(player, 1);
        }
    }

    // Persistence System
    public void writeData() {

        File workingDirectory = new File(workingDir);

        IOUtil.toBinaryFile(workingDirectory, "active", playerState);
        IOUtil.toBinaryFile(workingDirectory, "gone", goneState);
    }

    public void reloadData() {

        File activeFile = new File(workingDir + "active.dat");
        File goneFile = new File(workingDir + "gone.dat");

        if (activeFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(activeFile);

            if (playerStateFileO instanceof ConcurrentHashMap) {
                //noinspection unchecked
                playerState = (ConcurrentHashMap<UUID, PlayerState>) playerStateFileO;
            } else {
                log.warning("Invalid identity record file encountered: " + activeFile.getName() + "!");
                log.warning("Attempting to use backup file...");

                activeFile = new File(workingDir + "old-" + activeFile.getName());

                if (activeFile.exists()) {

                    playerStateFileO = IOUtil.readBinaryFile(activeFile);

                    if (playerStateFileO instanceof ConcurrentHashMap) {
                        //noinspection unchecked
                        playerState = (ConcurrentHashMap<UUID, PlayerState>) playerStateFileO;
                        log.info("Backup file loaded successfully!");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }

        if (goneFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(goneFile);

            if (playerStateFileO instanceof ConcurrentHashMap) {
                //noinspection unchecked
                goneState = (ConcurrentHashMap<UUID, PlayerState>) playerStateFileO;
            } else {
                log.warning("Invalid identity record file encountered: " + goneFile.getName() + "!");
                log.warning("Attempting to use backup file...");

                goneFile = new File(workingDir + "old-" + goneFile.getName());

                if (goneFile.exists()) {

                    playerStateFileO = IOUtil.readBinaryFile(goneFile);

                    if (playerStateFileO instanceof ConcurrentHashMap) {
                        //noinspection unchecked
                        goneState = (ConcurrentHashMap<UUID, PlayerState>) playerStateFileO;
                        log.info("Backup file loaded successfully!");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }

        for (Map.Entry<UUID, PlayerState> entry : playerState.entrySet()) {
            goneState.put(entry.getKey(), entry.getValue());
        }
        playerState.clear();

        restore();

        log.info("Loaded: " + goneState.size() + " saved identities for: Jungle Raid.");
    }

    public Optional<Color> getTeamColor(Player player) {
        Set<Player> playerTeam = teamMapping.get(player);
        if (playerTeam == redTeamPlayers) {
            return Optional.of(Color.RED);
        } else if (playerTeam == blueTeamPlayers) {
            return Optional.of(Color.BLUE);
        } else if (playerTeam == freeForAllPlayers) {
            return Optional.of(Color.WHITE);
        }
        return Optional.empty();
    }

    private void payPlayer(Player player, double modifier) {

        double amt = BASE_AMT * modifier;

        economy.depositPlayer(player.getName(), amt);
        ChatUtil.sendNotice(player, "You received: " + economy.format(amt) + '.');
    }

    public boolean isFriendlyFire(Player attacker, Player defender) {
        Set<Player> attackerTeam = teamMapping.get(attacker);
        Set<Player> defenderTeam = teamMapping.get(defender);

        /* We want identity comparison to prevent expensive list comparisons */
        return attackerTeam == defenderTeam && attackerTeam != freeForAllPlayers && attackerTeam != null;
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
        ProtectedRegion r = region;
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
        server.getScheduler().runTaskLater(inst, () -> {
            setupRegionInfo();
            reloadData();
        }, 1);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(new JungleRaidListener());

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    public void end() {
        Iterator<Map.Entry<UUID, PlayerState>> it = playerState.entrySet().iterator();

        while (it.hasNext()) {
            PlayerState state = it.next().getValue();
            removeFromTeam(Bukkit.getPlayer(state.getOwnerName()), true);
            it.remove();
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
        leftFlagActivationSign = new Location(world, -766, 82, -364);
        rightFlagActivationSign = new Location(world, -760, 82, -364);

        for (int x = leftFlagActivationSign.getBlockX() + 1; x < rightFlagActivationSign.getBlockX(); ++x) {
            scrollingFlagSigns.add(new Location(world, x, 82, -364));
        }

        for (JungleRaidFlag flag : JungleRaidFlag.values()) {
            flagState[flag.index] = flag.enabledByDefault;
        }

        flagSignPopulate();

        leftClassActivationSign = new Location(world, -745, 82, -364);
        rightClassActivationSign = new Location(world, -739, 82, -364);

        for (int x = leftClassActivationSign.getBlockX() + 1; x < rightClassActivationSign.getBlockX(); ++x) {
            scrollingClassSigns.add(new Location(world, x, 82, -364));
        }

        classSignPopulate();
    }

    private void updateFlagSign(int index) {
        String title = JungleRaidFlag.values()[signScrollFlagStart + index].toString();
        if (title.length() > 15) {
            title = title.substring(0, 15);
        }
        title = WordUtils.capitalizeFully(title.replace("_", " "));

        Sign sign = (Sign) scrollingFlagSigns.get(index).getBlock().getState();
        sign.setLine(1, title);
        sign.setLine(2, flagState[signScrollFlagStart + index] ? ChatColor.DARK_GREEN + "Enabled" : ChatColor.RED + "Disabled");
        sign.update(true);
    }

    private void flagSignPopulate() {
        for (int i = 0; i < scrollingFlagSigns.size(); ++i) {
            updateFlagSign(i);
        }

        boolean isLeftScrollable = signScrollFlagStart == 0;
        Sign leftSign = (Sign) leftFlagActivationSign.getBlock().getState();
        leftSign.setLine(1, isLeftScrollable ? "" : ChatColor.BLUE + "<<");
        leftSign.update(true);

        boolean isRightScrollable = signScrollFlagStart + scrollingFlagSigns.size() == JungleRaidFlag.values().length;
        Sign rightSign = (Sign) rightFlagActivationSign.getBlock().getState();
        rightSign.setLine(1, isRightScrollable ? "" : ChatColor.BLUE + ">>");
        rightSign.update(true);
    }

    public void leftFlagListSign() {
        signScrollFlagStart = Math.max(0, signScrollFlagStart - scrollingFlagSigns.size());
        flagSignPopulate();
    }

    public void rightFlagListSign() {
        signScrollFlagStart = Math.min(JungleRaidFlag.values().length - scrollingFlagSigns.size(), signScrollFlagStart + scrollingFlagSigns.size());
        flagSignPopulate();
    }

    public void tryToggleFlagSignAt(Location loc) {
        for (int i = 0; i < scrollingFlagSigns.size(); ++i) {
            if (loc.equals(scrollingFlagSigns.get(i))) {
                flagState[signScrollFlagStart + i] = !flagState[signScrollFlagStart + i];
                updateFlagSign(i);
                break;
            }
        }
    }

    private void updateClassSign(int index) {
        String title = JungleRaidClass.values()[signScrollClassStart + index].toString();
        if (title.length() > 15) {
            title = title.substring(0, 15);
        }
        title = WordUtils.capitalizeFully(title.replace("_", " "));

        Sign sign = (Sign) scrollingClassSigns.get(index).getBlock().getState();
        sign.setLine(1, title);
        sign.update(true);
    }

    private void classSignPopulate() {
        for (int i = 0; i < scrollingClassSigns.size(); ++i) {
            updateClassSign(i);
        }

        boolean isLeftScrollable = signScrollClassStart == 0;
        Sign leftSign = (Sign) leftClassActivationSign.getBlock().getState();
        leftSign.setLine(1, isLeftScrollable ? "" : ChatColor.BLUE + "<<");
        leftSign.update(true);

        boolean isRightScrollable = signScrollClassStart + scrollingClassSigns.size() == JungleRaidClass.values().length;
        Sign rightSign = (Sign) rightClassActivationSign.getBlock().getState();
        rightSign.setLine(1, isRightScrollable ? "" : ChatColor.BLUE + ">>");
        rightSign.update(true);
    }

    public void leftClassListSign() {
        signScrollClassStart = Math.max(0, signScrollClassStart - scrollingClassSigns.size());
        classSignPopulate();
    }

    public void rightClassListSign() {
        signScrollClassStart = Math.min(JungleRaidClass.values().length - scrollingClassSigns.size(), signScrollClassStart + scrollingClassSigns.size());
        classSignPopulate();
    }

    public void tryUseClassSignAt(Location loc, Player player) {
        for (int i = 0; i < scrollingClassSigns.size(); ++i) {
            if (loc.equals(scrollingClassSigns.get(i))) {
                JungleRaidClass targetClass = JungleRaidClass.values()[signScrollClassStart + i];
                giveBaseEquipment(player, targetClass);
                classMap.put(player, targetClass);
                break;
            }
        }
    }

    public void setFlag(JungleRaidFlag flag, boolean enabled) {
        flagState[flag.index] = enabled;
    }

    public boolean isSuddenDeath() {
        return !isFlagEnabled(JungleRaidFlag.NO_TIME_LIMIT) && System.currentTimeMillis() - getStartTime() >= TimeUnit.MINUTES.toMillis(15);
    }

    public boolean isFlagEnabled(JungleRaidFlag flag) {
        return flagState[flag.index];
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


    public Optional<Win> getWinner() {
        return getWinner(freeForAllPlayers, blueTeamPlayers, redTeamPlayers);
    }

    private Optional<Win> getWinner(Collection<Player> ffa, Collection<Player> blue, Collection<Player> red) {
        if (ffa.size() == 1 && blue.isEmpty() && red.isEmpty()) {
            return Optional.of(new Win(ffa.iterator().next().getName(), WinType.SOLO));
        } else if (ffa.isEmpty() && !blue.isEmpty() && red.isEmpty()) {
            return Optional.of(new Win("Blue", WinType.TEAM));
        } else if (ffa.isEmpty() && blue.isEmpty() && !red.isEmpty()) {
            return Optional.of(new Win("Red", WinType.TEAM));
        } else if (ffa.isEmpty() && blue.isEmpty() && red.isEmpty()) {
            return Optional.of(new Win(null, WinType.DRAW));
        }

        return Optional.empty();
    }

    private void resetPlayerProperties(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setExhaustion(0);

        player.getActivePotionEffects().clear();
    }

    private void maybeDisableGuild(Player player) {
        boolean allowGuilds = isFlagEnabled(JungleRaidFlag.ALLOW_GUILDS);
        if (allowGuilds) {
            return;
        }

        adminComponent.deguildPlayer(player);
    }

    private void addPlayer(Player player, Supplier<Location> startingPos, Color teamColor, JungleRaidClass jrClass) {
        giveBaseEquipment(player, jrClass);
        giveTeamEquipment(player, teamColor);

        resetPlayerProperties(player);
        maybeDisableGuild(player);

        prayerComponent.uninfluencePlayer(player);

        player.teleport(startingPos.get());
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

    public void addFFAPlayer(Player player, JungleRaidClass jrClass) {
        addPlayer(player, this::getRandomLocation, Color.WHITE, jrClass);
        freeForAllPlayers.add(player);
        teamMapping.put(player, freeForAllPlayers);
    }

    public void addBluePlayer(Player player, JungleRaidClass jrClass) {
        addPlayer(player, this::getRandomLocation, Color.BLUE, jrClass);
        blueTeamPlayers.add(player);
        teamMapping.put(player, blueTeamPlayers);
    }

    public void addRedPlayer(Player player, JungleRaidClass jrClass) {
        addPlayer(player, this::getRandomLocation, Color.RED, jrClass);
        redTeamPlayers.add(player);
        teamMapping.put(player, redTeamPlayers);
    }

    private void enforceBounds() {
        for (PlayerState entry : playerState.values()) {
            Player player = Bukkit.getPlayerExact(entry.getOwnerName());
            if (player == null || !player.isValid()) continue;
            if (anythingContains(player)) continue;

            player.setHealth(0);
        }
    }

    private JungleRaidClass getClassForPlayer(Player player) {
        return classMap.getOrDefault(player, JungleRaidClass.BALANCED);
    }

    public void smartStart() {
        List<Player> ffaList = new ArrayList<>();
        List<Player> redList = new ArrayList<>();
        List<Player> blueList = new ArrayList<>();

        Collection<Player> containedPlayers = getPlayersInLobby();
        if (containedPlayers.size() <= 1) {
            return;
        }

        for (Player player : containedPlayers) {
            Block block = player.getLocation().add(0, -1, 0).getBlock();
            if (block.getType() != Material.CONCRETE) {
                return;
            }

            switch (block.getData()) {
                case 0:
                    ffaList.add(player);
                    break;
                case 11:
                    blueList.add(player);
                    break;
                case 14:
                    redList.add(player);
                    break;
                default:
                    return;
            }
        }

        if (getWinner(ffaList, blueList, redList).isPresent()) {
            ChatUtil.sendError(getPlayersInLobby(), "All players are on one team, the game will not start.");
            return;
        }

        ffaList.forEach(p -> addFFAPlayer(p, getClassForPlayer(p)));
        redList.forEach(p -> addRedPlayer(p, getClassForPlayer(p)));
        blueList.forEach(p -> addBluePlayer(p, getClassForPlayer(p)));

        state = JungleRaidState.INITIALIZE;
        startTime = System.currentTimeMillis();
    }

    private void tryBeginCombat() {
        if (System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(1)) {
            state = JungleRaidState.IN_PROGRESS;
            ChatUtil.sendNotice(getPlayersInArena(), ChatColor.DARK_RED + "LET THE SLAUGHTER BEGIN!");
        }
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
                        flagData.titan = CollectionUtil.getElement(participants).getUniqueId();
                    }

                    if (player.getUniqueId().equals(flagData.titan)) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 20, 1, true));
                    }
                }
            }

            // Sudden death
            boolean suddenD = !isFlagEnabled(JungleRaidFlag.NO_TIME_LIMIT)
                    && System.currentTimeMillis() - startTime >= TimeUnit.MINUTES.toMillis(15);
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
                            ThrownPotion potion = world.spawn(testLoc, ThrownPotion.class);

                            ItemStack brewedPotion = new ItemStack(Material.SPLASH_POTION);
                            PotionMeta potionMeta = (PotionMeta) brewedPotion.getItemMeta();
                            // Prioritize upgradablility over extendability
                            boolean isExtended = type.isExtendable() && !type.isUpgradeable();
                            boolean isUpgraded = type.isUpgradeable();
                            potionMeta.setBasePotionData(new PotionData(type, isExtended, isUpgraded));
                            brewedPotion.setItemMeta(potionMeta);

                            potion.setItem(brewedPotion);
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

            if (lobbyContains(player)) {
                removeFromLobby(player);
                player.teleport(lobbyExitLocation);
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

            if (blockLoc.equals(leftFlagActivationSign)) {
                leftFlagListSign();
            } else if (blockLoc.equals(rightFlagActivationSign)) {
                rightFlagListSign();
            } else if (blockLoc.equals(leftClassActivationSign)) {
                leftClassListSign();
            } else if (blockLoc.equals(rightClassActivationSign)) {
                rightClassListSign();
            } else {
                tryToggleFlagSignAt(blockLoc);
                tryUseClassSignAt(blockLoc, event.getPlayer());
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
                    for (PlayerState state : playerState.values()) {

                        Player aPlayer = Bukkit.getPlayerExact(state.getOwnerName());

                        // Check validity
                        if (aPlayer == null || !player.isValid() || player.equals(aPlayer)) continue;

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
                if (!playerState.containsKey(player.getUniqueId())) {
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
                if (getClassForPlayer(attackingPlayer) == JungleRaidClass.SNIPER) {
                    Projectile projectile = result.getProjectile();
                    if (projectile != null) {
                        double distSq = attackingPlayer.getLocation().distanceSquared(
                                defendingPlayer.getLocation()
                        );
                        double targetDistSq = Math.pow(70, 2);
                        double ratio = Math.min(distSq, targetDistSq) / targetDistSq;

                        // Handle damage modification
                        event.setDamage(event.getDamage() * ratio);
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
            if (playerState.containsKey(player.getUniqueId())) {
                Optional<Color> optTeamColor  = getTeamColor(player);

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
                    Optional<Color> optKillerColor = getTeamColor(killer);
                    if (optKillerColor.isPresent()) {
                        if (isTitanEnabled && killer.isValid()) {
                            if (isTitan) {
                                flagData.titan = killer.getUniqueId();
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

                final List<Color> colors = Collections.singletonList(optTeamColor.get());
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

                left(player);
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {

            final Player p = event.getPlayer();

            // Technically forced, but because this
            // happens from disconnect/quit button
            // we don't want it to count as forced
            server.getScheduler().runTaskLater(inst, () -> removeGoneFromTeam(p, false), 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerRespawn(PlayerRespawnEvent event) {

            final Player p = event.getPlayer();

            server.getScheduler().runTaskLater(inst, () -> removeGoneFromTeam(p, true), 1);
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

            if (playerState.containsKey(player.getUniqueId()))  {
                left(player);
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

            if (lobbyContains(event.getTo()) && !playerState.containsKey(player.getUniqueId())) {
                event.setTo(lobbyExitLocation);
            }
        }

        @EventHandler
        public void onProjectileLand(ProjectileHitEvent event) {

            Projectile p = event.getEntity();
            if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
            if (arenaContains(p) && state == JungleRaidState.IN_PROGRESS) {
                int explosionSize = 2;
                LivingEntity shooter = (LivingEntity) p.getShooter();

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

                p.getWorld().createExplosion(p.getLocation(), explosionSize);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockChangePreLog(BlockChangePreLogEvent event) {
            if (arenaContains(event.getLocation())) event.setCancelled(true);
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