/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.RitualTomb;

import com.destroystokyo.paper.Title;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.sacrifice.SacrificeInformation;
import gg.packetloss.grindstone.sacrifice.SacrificeResult;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import gg.packetloss.grindstone.util.dropttable.BoundDropSpawner;
import gg.packetloss.grindstone.util.dropttable.MassBossDropTable;
import gg.packetloss.grindstone.util.dropttable.MassBossKillInfo;
import gg.packetloss.grindstone.util.dropttable.MassBossPlayerKillInfo;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.warps.WarpQualifiedName;
import gg.packetloss.grindstone.warps.WarpsComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@ComponentInformation(friendlyName = "Ritual Tomb", desc = "The demons of Hallow")
@Depend(components = {ManagedWorldComponent.class, PlayerStateComponent.class,
        SpectatorComponent.class, WarpsComponent.class},
    plugins = {"WorldGuard"})
public class RitualTomb extends AreaComponent<RitualTombConfig> {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    protected PlayerStateComponent playerState;
    @InjectComponent
    private SpectatorComponent spectator;
    @InjectComponent
    private WarpsComponent warps;

    private static final WarpQualifiedName ENTRANCE_WARP = new WarpQualifiedName("ritual-site");

    protected ProtectedRegion floorRegion;
    protected Location ritualFireLoc;

    protected MassBossDropTable dropTable = new MassBossDropTable();
    protected BossBar healthBar = Bukkit.createBossBar("Ritual of Hallow - I", BarColor.PURPLE, BarStyle.SEGMENTED_6);

    private int demonsTotal;
    private int demonsCurrent;
    private boolean demonsCanKill;

    private BigDecimal ritualValue = BigDecimal.ZERO;
    private Map<UUID, BigDecimal> individualRitualValue = new HashMap<>();
    private Map<UUID, Integer> individualDemonKills = new HashMap<>();

    @Override
    public void setUp() {
        spectator.registerSpectatorKind(PlayerStateKind.RITUAL_TOMB_SPECTATOR);

        world = managedWorld.get(ManagedWorldGetQuery.CITY);
        RegionManager regionManager = WorldGuardBridge.getManagerFor(world);
        region = regionManager.getRegion("vineam-district-ritual-tomb");
        floorRegion = regionManager.getRegion("vineam-district-ritual-tomb-floor");
        tick = 20 * 5;
        listener = new RitualTombListener(this);
        config = new RitualTombConfig();

        ritualFireLoc = new Location(world, 250, 102, 530);

        setDefaults();

        Bukkit.getScheduler().runTaskTimer(CommandBook.inst(), this::updateBossBarProgress, 0, 5);

        CommandBook.registerEvents(new FlightBlockingListener(this::contains));

        setupDropTable();

        spectator.registerSpectatedRegion(PlayerStateKind.RITUAL_TOMB_SPECTATOR, region);
        spectator.registerSpectatorSkull(
            PlayerStateKind.RITUAL_TOMB_SPECTATOR,
            new Location(world, 245, 104, 533),
            () -> !isEmpty()
        );
    }

    @Override
    public void disable() {
        resetRitual();
    }

    private static ItemPointTranslator diamondLoot = new ItemPointTranslator();

    static {
        diamondLoot.addMapping(CustomItemCenter.build(CustomItems.PHANTOM_DIAMOND), 750);
        diamondLoot.addMapping(new ItemStack(Material.DIAMOND_BLOCK), 9);
        diamondLoot.addMapping(new ItemStack(Material.DIAMOND), 1);
    }

    private void setupDropTable() {
        dropTable.registerCustomPlayerDrop((info, consumer) -> {
            Player player = info.getPlayer();

            // Sacrificed Loot
            BigDecimal individualValue = individualRitualValue.getOrDefault(player.getUniqueId(), BigDecimal.ZERO);
            if (individualValue.compareTo(BigDecimal.ZERO) > 0) {
                // Roll two multiplied sacrifices with limited quantity
                SacrificeComponent.getCalculatedLoot(new SacrificeInformation(
                    player, 5, individualValue.multiply(BigDecimal.valueOf(50))
                )).forEach(consumer);
                SacrificeComponent.getCalculatedLoot(new SacrificeInformation(
                    player, 5, individualValue.multiply(BigDecimal.valueOf(10))
                )).forEach(consumer);

                SacrificeResult finalPersonal = SacrificeComponent.getCalculatedLoot(new SacrificeInformation(
                    player, 10, individualValue
                ));
                finalPersonal.forEach(consumer);

                // Update individual value with the remainder in value as points
                individualValue = finalPersonal.getRemainingValue();
            }

            int numPlayers = info.getKillInfo().getPlayers().size();
            SacrificeComponent.getCalculatedLoot(new SacrificeInformation(
                Bukkit.getConsoleSender(),
                10,
                ritualValue.divide(BigDecimal.valueOf(numPlayers), RoundingMode.HALF_UP)
            )).forEach(consumer);

            // Diamond Loot
            int individualKills = individualDemonKills.getOrDefault(player.getUniqueId(), 0);
            diamondLoot.streamValue(
                // (int) ((individualKills + individualValue) / 30)
                BigDecimal.valueOf(individualKills).add(individualValue).divide(
                    BigDecimal.valueOf(30), RoundingMode.HALF_UP
                ).intValue(),
                consumer);
        });

        // Chance modified drops
        NumericPipeline.Builder<MassBossPlayerKillInfo> modifiedChance = NumericPipeline.builder();
        modifiedChance.accept((info, chance) -> chance / getRitualLevel());

        // Red Items
        dropTable.registerPlayerDrop(modifiedChance.build(15000), () -> CustomItemCenter.build(CustomItems.RED_FEATHER));
        dropTable.registerPlayerDrop(modifiedChance.build(15000), () -> CustomItemCenter.build(CustomItems.RED_SWORD));
        dropTable.registerPlayerDrop(modifiedChance.build(15000), () -> CustomItemCenter.build(CustomItems.RED_BOW));
    }

    @Override
    public void run() {
        updateBossBar();

        if (!isRitualActive()) {
            return;
        }

        if (isEmpty()) {
            resetRitual();
            return;
        }

        updateDemonsInternal();
    }

    public void cleanupDemons() {
        getContained(Vex.class).forEach(Entity::remove);
    }

    public void setFloor(boolean filled) {
        RegionWalker.walk(floorRegion, (x, y, z) -> {
            if (x == 285 && z == 543) {
                return;
            }

            Block block = world.getBlockAt(x, y, z);
            if (filled && block.getType().isAir()) {
                block.setType(Material.CRACKED_STONE_BRICKS);
            } else if (!filled && block.getType() == Material.CRACKED_STONE_BRICKS) {
                block.setType(Material.AIR);
            }
        });
    }

    private void setDefaults() {
        ritualValue = BigDecimal.ZERO;
        individualRitualValue.clear();
        individualDemonKills.clear();
        demonsTotal = demonsCurrent = config.ritualDemonsStartingAmount;
        demonsCanKill = false;
    }

    public void resetRitual() {
        setDefaults();
        cleanupDemons();
        setFloor(true);
    }

    public boolean isRitualActive() {
        return ritualValue.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean areDemonsLethal() {
        return demonsCanKill;
    }

    public int getRitualLevel() {
        return ritualValue.divide(BigDecimal.valueOf(config.ritualLevelInterval), RoundingMode.FLOOR).intValue() + 1;
    }

    private void updateBossBar() {
        if (isRitualActive()) {
            BossBarUtil.syncWithPlayers(healthBar, getAudiblePlayers());
        } else {
            healthBar.removeAll();
        }
    }
    private void updateBossBarProgress() {
        if (isRitualActive()) {
            healthBar.setProgress((double) demonsCurrent / demonsTotal);
        }
    }

    public int getNumberOfDemonsActive() {
        double modifier = (config.ritualLevelDemonMultiplier * (getRitualLevel() - 1)) + 1;
        return Math.min(config.ritualDemonsCountMax, (int) (config.ritualDemonsCountMin * modifier));
    }

    private Collection<Vex> fillDemons() {
        Collection<Vex> demons = getContained(Vex.class);
        for (int i = demons.size(), target = getNumberOfDemonsActive(); i < target; ++i) {
            demons.add(world.spawn(getRandomRitualTombPoint(), Vex.class));
        }
        return demons;
    }

    private void announceDemonsLethal(Player target) {
        String targetName = target.getDisplayName().toUpperCase();
        for (Player player : getAudiblePlayers()) {
            player.sendTitle(Title.builder()
                .title(Text.of(ChatColor.DARK_RED, "YOUR DEATH IS NEAR").build())
                .subtitle(Text.of(ChatColor.DARK_RED, targetName + " SHALL BE FIRST").build())
                .fadeIn(10)
                .stay(20 * 3)
                .fadeOut(10)
                .build());
        }
    }

    private Player findHighPriorityTarget() {
        List<Player> players = getContainedParticipants();
        players.sort(Comparator.comparing((e) -> e.getHealth() / e.getMaxHealth()));

        Player target = players.get(players.size() - 1);

        boolean demonsCouldKill = demonsCanKill;
        demonsCanKill = target.getHealth() < target.getMaxHealth() / 2;

        if (demonsCanKill && !demonsCouldKill) {
            announceDemonsLethal(target);
        }

        return target;
    }

    private void seekDemonsOn(Collection<Vex> demons, Player player) {
        demons.forEach(d -> d.setTarget(player));
    }

    private void updateDemonsInternal() {
        seekDemonsOn(fillDemons(), findHighPriorityTarget());
    }

    protected void updateDemons() {
        if (!isRitualActive()) {
            return;
        }

        // This is important so we ensure that any demons (Vex) don't follow players out of the arena
        // if they exit by warping somewhere nearby
        if (isEmpty()) {
            cleanupDemons();
            return;
        }

        updateDemonsInternal();
    }

    public void increaseRitualValue(Player player, BigDecimal amount) {
        int oldLevel = getRitualLevel();
        ritualValue = ritualValue.add(amount);
        individualRitualValue.merge(player.getUniqueId(), amount, BigDecimal::add);
        int newLevel = getRitualLevel();

        if (newLevel == oldLevel) {
            return;
        }

        // Update demon counts
        int oldDemons = demonsTotal;
        demonsTotal *= config.ritualLevelDemonMultiplier * (newLevel - oldLevel);
        demonsCurrent += demonsTotal - oldDemons;

        // Maybe remove the floor
        if (oldLevel < config.ritualLevelFloorRemoval && newLevel >= config.ritualLevelFloorRemoval) {
            setFloor(false);
        }

        // Update the ritual name
        healthBar.setTitle("Ritual of Hallow - " + RomanNumeralUtil.toRoman(getRitualLevel()));
    }

    public void demonKilled(Player killer) {
        individualDemonKills.merge(killer.getUniqueId(), 1, Integer::sum);
        if (--demonsCurrent == 0 || ItemUtil.isHoldingItemInFamily(killer, ItemFamily.PWNG)) {
            List<Player> players = getContainedParticipants();

            players.forEach(this::teleportToRitualSite);
            Location dropLoc = ritualFireLoc.clone().add(0.5, .75, 0.5);
            new BoundDropSpawner(() -> dropLoc).provide(dropTable, new MassBossKillInfo(players));

            resetRitual();
        }
    }

    private int getFloorLevel() {
        return floorRegion.getMaximumPoint().getY();
    }

    public Location getRandomRitualTombPoint() {
        Location midPoint = RegionUtil.getCenter(world, floorRegion);
        return LocationUtil.pickLocation(
            world,
            getFloorLevel() + 1,
            new RegionChecker(region) {
                private boolean canStandOnBlock(BlockVector3 vector) {
                    Block upper = world.getBlockAt(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
                    Block lower = world.getBlockAt(vector.getBlockX(), vector.getBlockY() - 1, vector.getBlockZ());
                    return !upper.getType().isSolid() && lower.getType().isSolid();
                }

                private boolean isNotTooCloseToMid(BlockVector3 vector) {
                    Location vectorAsLoc = new Location(world, vector.getX(), vector.getY(), vector.getZ());
                    return !LocationUtil.isWithin2DDistance(midPoint, vectorAsLoc, 4);
                }

                @Override
                public Boolean evaluate(BlockVector3 vector) {
                    return canStandOnBlock(vector) && isNotTooCloseToMid(vector);
                }
            }
        ).add(0.5, 0, 0.5);
    }

    public void teleportToRitualTomb(Player player, boolean asSpectator) {
        Location spawnPoint = getRandomRitualTombPoint();
        if (asSpectator) {
            spawnPoint.add(0, 3, 0);
        }

        Location pointOfInterest = RegionUtil.getCenterAt(world, getFloorLevel(), floorRegion);
        spawnPoint.setDirection(VectorUtil.createDirectionalVector(spawnPoint, pointOfInterest));

        player.teleport(spawnPoint, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    private final Set<UUID> activeDialogSet = new HashSet<>();

    public void tryTeleportToRitualTomb(Player player) {
        ChatUtil.sendWarning(player, "You dare touch the sacred flame!");

        if (isRitualActive()) {
            UUID playerID = player.getUniqueId();
            if (activeDialogSet.contains(playerID)) {
                return;
            }

            activeDialogSet.add(playerID);
            ChatUtil.sendStaggered(player, List.of(
                Text.of(ChatColor.RED, "We're busy at the moment..."),
                Text.of(ChatColor.RED, "Come back and try again later, then you'll pay!!!"),
                Text.of(ChatColor.RED, "Oof! Ughh, no actually don't do that!"),
                Text.of(ChatColor.RED, "Don't, and I can't stress this enough, don't, touch the flame!"),
                Text.of(ChatColor.RED, "But We have cookies!"),
                Text.of(ChatColor.RED, "Don't tell em about the cookies Carl!"),
                Text.of(ChatColor.RED, "But we have..."),
                Text.of(ChatColor.RED, "Don't listen to em, we have knives!!! Run!!! Hide!!!")
            )).thenFinally(() -> activeDialogSet.remove(playerID));
            return;
        }

        teleportToRitualTomb(player, false);
    }

    public Location getRitualSiteLoc() {
        return warps.getWarp(ENTRANCE_WARP).orElse(world.getSpawnLocation());
    }

    public void teleportToRitualSite(Player player) {
        player.teleport(getRitualSiteLoc());
        ChatUtil.sendNotice(player, "Was it all just a bad dream...?");
    }
}
