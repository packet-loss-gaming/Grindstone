/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.minigame.games;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.patterns.SingleBlockPattern;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.anticheat.AntiCheatCompatibilityComponent;
import com.skelril.aurora.city.engine.minigame.MinigameComponent;
import com.skelril.aurora.city.engine.minigame.PlayerGameState;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.events.apocalypse.ApocalypseLocalSpawnEvent;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.checker.RegionChecker;
import com.skelril.aurora.util.extractor.entity.CombatantPair;
import com.skelril.aurora.util.extractor.entity.EDBEExtractor;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
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
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Sky Wars", desc = "Sky warfare at it's best!")
@Depend(components = {AdminComponent.class, PrayerComponent.class}, plugins = {"WorldEdit", "WorldGuard"})
public class SkyWarsComponent extends MinigameComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private ProtectedRegion region;
    private World world;
    private LocalConfiguration config;
    private short attempts = 0;

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    AntiCheatCompatibilityComponent antiCheat;
    @InjectComponent
    SessionComponent sessions;

    public SkyWarsComponent() {
        super("Sky War", "sw", 10);
    }

    @Override
    public void initialize(Set<Character> flags) {
        super.initialize(flags);

        ChatUtil.sendNotice(getContainedPlayers(), "Get ready...");
    }

    @Override
    public void start() {
        super.start();

        Collection<Player> players = getContainedPlayers();

        for (Player player : players) {
            launchPlayer(player, 1);
            sessions.getSession(SkyWarSession.class, player).stopPushBack();
        }

        editStartingPad(0, 0);

        ChatUtil.sendNotice(players, "Fight!");
    }

    private void launchPlayer(Player player, double mod) {
        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
        player.setVelocity(new Vector(0, 3.5, 0).multiply(mod));
    }

    // Player Management
    @Override
    public boolean addToTeam(Player player, int teamNumber, Set<Character> flags) {

        if (adminComponent.isAdmin(player) && !adminComponent.isSysop(player)) return false;

        super.addToTeam(player, teamNumber, flags);

        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();

        List<ItemStack> gear = new ArrayList<>();

        gear.add(makeSkyFeather(-1, 3, 2, 2));

        player.getInventory().addItem(gear.toArray(new ItemStack[gear.size()]));

        ItemStack[] leatherArmour = ItemUtil.leatherArmour;
        Color color = Color.WHITE;
        switch (teamNumber) {
            case 1:
                color = Color.BLUE;
                break;
            case 2:
                color = Color.RED;
                break;
            case 3:
                color = Color.GREEN;
                break;
            case 4:
                color = Color.ORANGE;
                break;
            case 5:
                color = Color.MAROON;
                break;
            case 6:
                color = Color.PURPLE;
                break;
            case 7:
                color = Color.GRAY;
                break;
            case 8:
                color = Color.YELLOW;
                break;
            case 9:
                color = Color.BLACK;
                break;
        }

        LeatherArmorMeta helmMeta = (LeatherArmorMeta) leatherArmour[3].getItemMeta();
        helmMeta.setDisplayName(ChatColor.WHITE + "Sky Hood");
        helmMeta.setColor(color);
        leatherArmour[3].setItemMeta(helmMeta);

        LeatherArmorMeta chestMeta = (LeatherArmorMeta) leatherArmour[2].getItemMeta();
        chestMeta.setDisplayName(ChatColor.WHITE + "Sky Plate");
        chestMeta.setColor(color);
        leatherArmour[2].setItemMeta(chestMeta);

        LeatherArmorMeta legMeta = (LeatherArmorMeta) leatherArmour[1].getItemMeta();
        legMeta.setDisplayName(ChatColor.WHITE + "Sky Leggings");
        legMeta.setColor(color);
        leatherArmour[1].setItemMeta(legMeta);

        LeatherArmorMeta bootMeta = (LeatherArmorMeta) leatherArmour[0].getItemMeta();
        bootMeta.setDisplayName(ChatColor.WHITE + "Sky Boots");
        bootMeta.setColor(color);
        leatherArmour[0].setItemMeta(bootMeta);

        playerInventory.setArmorContents(leatherArmour);

        Location battleLoc = new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z);

        if (battleLoc.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR)) {
            edit(BlockID.STAINED_GLASS, 15, battleLoc);
        }

        if (player.getVehicle() != null) {
            player.getVehicle().eject();
        }
        return player.teleport(battleLoc);
    }

    private void editStartingPad(int toType, int toData) {

        edit(toType, toData, new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z));
    }

    private void edit(int toType, int toData, Location battleLoc) {

        battleLoc = battleLoc.clone().add(0, -1, 0);

        EditSession editor = WorldEdit.getInstance()
                .getEditSessionFactory().getEditSession(new BukkitWorld(battleLoc.getWorld()), -1);
        com.sk89q.worldedit.Vector origin = new com.sk89q.worldedit.Vector(
                battleLoc.getX(), battleLoc.getY(), battleLoc.getZ()
        );
        Pattern pattern = new SingleBlockPattern(new BaseBlock(toType, toData));
        try {
            editor.makeCylinder(origin, pattern, 12, 1, true);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    private void awardPowerup(Player player, ItemStack held) {

        ItemStack powerup;

        if (ChanceUtil.getChance(12)) {
            if (ChanceUtil.getChance(2)) {
                powerup = new ItemStack(ItemID.BOOK);
                ItemMeta powerMeta = powerup.getItemMeta();
                powerMeta.setDisplayName(ChatColor.WHITE + "Book o' Omens");
                powerup.setItemMeta(powerMeta);
            } else {
                powerup = new ItemStack(ItemID.SNOWBALL, 16);
                ItemMeta powerMeta = powerup.getItemMeta();
                powerMeta.setDisplayName(ChatColor.BLUE + "Frost Orb");
                powerup.setItemMeta(powerMeta);
            }
        } else if (ChanceUtil.getChance(5)) {
            powerup = new ItemStack(ItemID.WATCH);
            ItemMeta powerMeta = powerup.getItemMeta();
            powerMeta.setDisplayName(ChatColor.GOLD + "Defroster");
            powerup.setItemMeta(powerMeta);
        } else {

            if (ItemUtil.matchesFilter(held, ChatColor.AQUA + "Sky Feather [Doom]", false)) return;

            int uses = 5;
            double radius = 3;
            double flight = 2;
            double pushBack = 4;

            if (ChanceUtil.getChance(2)) {
                radius = 5;
                pushBack = 6;
            } else {
                flight = 6;
            }

            if (ChanceUtil.getChance(50)) {
                uses = -1;
                radius = 7;
                flight = 6;
                pushBack = 6;
                for (Player aPlayer : getContainedPlayers()) {
                    if (player.equals(aPlayer)) continue;
                    ChatUtil.sendWarning(aPlayer, player.getName() + " has been given a Doom feather!");
                }

                player.getInventory().clear();
            }

            powerup = makeSkyFeather(uses, radius, flight, pushBack);
        }
        player.getInventory().addItem(powerup);
        //noinspection deprecation
        player.updateInventory();

        // Display name doesn't need checked as all power ups have one assigned
        ChatUtil.sendNotice(player, "You obtain a power-up: "
                + powerup.getItemMeta().getDisplayName() + ChatColor.YELLOW + "!");
    }

    private void decrementUses(final Player player, ItemStack itemStack, int uses, double radius, double flight, double pushBack) {

        if (uses == -1) return;

        uses--;

        final ItemStack remainder;
        if (itemStack.getAmount() > 1) {
            remainder = itemStack.clone();
            remainder.setAmount(remainder.getAmount() - 1);
        } else {
            remainder = null;
        }

        final ItemStack newSkyFeather;
        if (uses < 1) {
            newSkyFeather = null;
        } else {
            newSkyFeather = modifySkyFeather(itemStack, uses, radius, flight, pushBack);
            newSkyFeather.setAmount(1);
        }

        server.getScheduler().runTaskLater(inst, () -> {
            if (newSkyFeather == null) {
                player.getInventory().setItemInHand(null);
            }
            if (remainder != null) {
                player.getInventory().addItem(remainder);
            }
            //noinspection deprecation
            player.updateInventory();
        }, 1);
    }

    private ItemStack makeSkyFeather(int uses, double radius, double flight, double pushBack) {

        return modifySkyFeather(new ItemStack(ItemID.FEATHER), uses, radius, flight, pushBack);
    }

    private ItemStack modifySkyFeather(ItemStack skyFeather, int uses, double radius, double flight, double pushBack) {
        ItemMeta skyMeta = skyFeather.getItemMeta();

        String suffix;

        if (uses == -1) {
            if (flight == pushBack && flight > 2) {
                suffix = "Doom";
            } else {
                suffix = "Infinite";
            }
        } else {
            if (flight == pushBack) {
                suffix = "Balance";
            } else if (flight > pushBack) {
                suffix = "Flight";
            } else {
                suffix = "Push Back";
            }
        }

        skyMeta.setDisplayName(ChatColor.AQUA + "Sky Feather [" + suffix + "]");
        skyMeta.setLore(Arrays.asList(
                ChatColor.GOLD + "Uses: " + (uses != -1 ? uses : "Infinite"),
                ChatColor.GOLD + "Radius: " + radius,
                ChatColor.GOLD + "Flight: " + flight,
                ChatColor.GOLD + "Push Back: " + pushBack
        ));
        skyFeather.setItemMeta(skyMeta);
        return skyFeather;
    }

    @Override
    public void printFlags() {

        Collection<Player> players = getContainedPlayers();

        ChatUtil.sendNotice(players, ChatColor.GREEN + "The following flags are enabled: ");

        if (gameFlags.contains('q')) ChatUtil.sendNotice(players, ChatColor.GOLD, "Quick start");
        if (gameFlags.contains('r')) ChatUtil.sendNotice(players, ChatColor.GOLD, "Regen enabled");
        if (gameFlags.contains('c')) ChatUtil.sendNotice(players, ChatColor.GOLD, "Chicken++");
    }

    @Override
    public String getWinner() {

        int[] teams = new int[MAX_TEAMS];
        for (PlayerGameState entry : playerState.values()) {
            teams[entry.getTeamNumber()]++;
        }

        int aliveTeam = -1;
        for (int team = 0; team < teams.length; team++) {
            if (teams[team] > 0) {
                if (aliveTeam != -1) return null;
                aliveTeam = team;
            }
        }

        String winnerName = "Team " + aliveTeam;
        switch (aliveTeam) {
            case -1:
                return "";
            case 0:
                if (teams[0] > 1) return null;
                winnerName = Lists.newArrayList(playerState.values()).get(0).getOwnerName();
                break;
        }
        return winnerName;
    }

    @Override
    public void restore(Player player, PlayerGameState state) {

        super.restore(player, state);
        player.setFallDistance(0);
    }

    @Override
    public Collection<Player> getContainedPlayers() {

        return getContainedPlayers(0);
    }

    public Collection<Player> getContainedPlayers(int parentsUp) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();

        final ProtectedRegion finalR = r;
        return server.getOnlinePlayers().stream()
                .filter(p -> LocationUtil.isInRegion(world, finalR, p))
                .collect(Collectors.toList());
    }

    public Entity[] getContainedEntities(Class<?>... classes) {

        return getContainedEntities(0, classes);
    }

    public Entity[] getContainedEntities(int parentsUp, Class<?>... classes) {

        List<Entity> returnedList = new ArrayList<>();

        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();

        for (Entity entity : world.getEntitiesByClasses(classes)) {

            if (entity.isValid() && LocationUtil.isInRegion(r, entity)) returnedList.add(entity);
        }
        return returnedList.toArray(new Entity[returnedList.size()]);
    }

    public boolean contains(Location location) {

        return LocationUtil.isInRegion(world, region, location);
    }

    @Override
    public boolean probe() {

        world = Bukkit.getWorld(config.worldName);
        try {
            region = getWorldGuard().getGlobalRegionManager().get(world).getRegion(config.region);
        } catch (UnknownPluginException | NullPointerException e) {
            if (attempts > 10) {
                e.printStackTrace();
                return false;
            }
            attempts++;
            server.getScheduler().runTaskLater(inst, this::probe, 2);
        }

        return world != null && region != null;
    }

    @Override
    public void enable() {

        super.enable();

        config = configure(new LocalConfiguration());

        probe();
        //noinspection AccessStaticViaInstance
        inst.registerEvents(new SkyWarsListener());
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
        probe();
    }

    @Override
    public void run() {

        try {
            if (!isGameInitialised() && !queue.isEmpty()) processQueue();
            if (playerState.size() == 0 && !isGameInitialised()) return;

            // Damage players & kill missing players
            for (PlayerGameState entry : playerState.values()) {
                try {
                    Player player = Bukkit.getPlayerExact(entry.getOwnerName());
                    if (player == null || !player.isValid()) {
                        continue;
                    }
                    Location pLoc = player.getLocation();
                    if (contains(pLoc)) {

                        // Keep the player within game parameters
                        adminComponent.standardizePlayer(player);
                        player.setFoodLevel(20);
                        player.setSaturation(5F);

                        // Damage the player or teleport them if need be
                        if (EnvironmentUtil.isWater(pLoc.getBlock())) {
                            if (isGameActive()) {
                                player.damage(ChanceUtil.getRandom(3));
                            } else {
                                player.teleport(new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z));
                            }
                        }
                        continue;
                    }
                    player.setHealth(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!isGameInitialised()) return;

            RegionChecker checker = new RegionChecker(region);
            for (int i = 0; i < playerState.size(); i++) {

                if (!ChanceUtil.getChance(10) && !gameFlags.contains('c')) continue;

                Location target = LocationUtil.pickLocation(world, region.getMaximumPoint().getY() - 10, checker);
                Chicken c = world.spawn(target, Chicken.class);
                c.setRemoveWhenFarAway(true);
            }

            if (!isGameActive()) return;

            String winnerName = getWinner();

            // No winner
            if (winnerName == null) return;

            // Tie or a specific team/player?
            if (winnerName.isEmpty()) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Tie game!");
            } else {
                Bukkit.broadcastMessage(ChatColor.GOLD + winnerName + " has won!");
            }

            end();
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

    }

    private static EDBEExtractor<Player, Player, Snowball> extractor = new EDBEExtractor<>(
            Player.class,
            Player.class,
            Snowball.class
    );

    private class SkyWarsListener implements Listener {

        private final String[] cmdWhiteList = new String[]{
                "skywar", "sw", "stopweather", "me", "say", "pm", "msg", "message", "whisper", "tell",
                "reply", "r", "mute", "unmute", "debug", "dropclear", "dc", "auth", "toggleeditwand"
        };

        @EventHandler(ignoreCancelled = true)
        public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {

            Player player = event.getPlayer();
            if (getTeam(player) != -1) {
                String command = event.getMessage();
                boolean allowed = false;
                for (String cmd : cmdWhiteList) {
                    if (command.toLowerCase().startsWith("/" + cmd)) {
                        allowed = true;
                        break;
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

            if (getTeam(event.getPlayer()) != -1 && !isGameActive()) event.setCancelled(true);
        }

        @EventHandler
        public void onHealthRegain(EntityRegainHealthEvent event) {

            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (getTeam(player) != -1) {
                    if (gameFlags.contains('r')) return;
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onClick(PlayerInteractEvent event) {

            final Player player = event.getPlayer();
            ItemStack stack = player.getItemInHand();

            if (!isGameActive()) return;

            if (getTeam(player) != -1) {
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

                            if (!session.canFly()) break;

                            vel.multiply(flight);

                            server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                            player.setVelocity(vel);

                            session.stopFlight(250);

                            decrementUses(player, stack, uses, radius, flight, pushBack);
                            break;
                        case RIGHT_CLICK_AIR:

                            if (!session.canPushBack()) break;

                            vel.multiply(pushBack * 2);

                            BlockIterator it = new BlockIterator(player, 50);
                            Location k = new Location(null, 0, 0, 0);

                            Entity[] targets = getContainedEntities(Chicken.class, Player.class);

                            while (it.hasNext()) {
                                Block block = it.next();

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
                            decrementUses(player, stack, uses, radius, flight, pushBack);
                            break;
                    }
                } else if (ItemUtil.matchesFilter(stack, ChatColor.WHITE + "Book o' Omens")) {

                    if (!session.canUseOmen()) return;

                    ChatUtil.sendNotice(player, "You used the Book o' Omens!");

                    session.stopOmen();

                    for (Player aPlayer : getContainedPlayers()) {
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
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamageEvent(EntityDamageEvent event) {

            Entity e = event.getEntity();

            if (!(e instanceof Player)) return;

            Player player = (Player) e;

            if (getTeam(player) != -1) {

                if (!isGameActive()) {
                    event.setCancelled(true);

                    if (event instanceof EntityDamageByEntityEvent) {
                        Entity attacker = ((EntityDamageByEntityEvent) event).getDamager();
                        if (attacker instanceof Projectile) {
                            ProjectileSource source = ((Projectile) attacker).getShooter();
                            if (source instanceof Entity) {
                                attacker = (Entity) source;
                            }
                        }
                        if (!(attacker instanceof Player)) return;
                        ChatUtil.sendError((Player) attacker, "The game has not yet started!");
                    }
                } else if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                    event.setCancelled(true);
                }
            } else if (contains(player.getLocation())) {
                player.teleport(player.getWorld().getSpawnLocation());
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

            CombatantPair<Player, Player, Snowball> result = extractor.extractFrom(event);

            if (result == null) return;

            Player attackingPlayer = result.getAttacker();
            Player defendingPlayer = result.getDefender();

            if (getTeam(attackingPlayer) == -1 && getTeam(defendingPlayer) != -1) {
                event.setCancelled(true);
                ChatUtil.sendWarning(attackingPlayer, "Don't attack participants.");
                return;
            }

            if (getTeam(attackingPlayer) == -1) return;
            if (getTeam(defendingPlayer) == -1) {
                ChatUtil.sendWarning(attackingPlayer, "Don't attack bystanders.");
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
            if (getTeam(player) != -1) {
                Player killer = player.getKiller();
                if (killer != null) {
                    event.setDeathMessage(player.getName() + " has been taken out by " + killer.getName());
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

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {

            Player player = event.getPlayer();

            if (getTeam(player) != -1) left(event.getPlayer());
        }

        @EventHandler
        public void onZombieLocalSpawn(ApocalypseLocalSpawnEvent event) {

            if (getTeam(event.getPlayer()) != -1) event.setCancelled(true);
        }

        @EventHandler
        public void onKick(PlayerKickEvent event) {

            if (getTeam(event.getPlayer()) != -1) event.setCancelled(true);
        }
    }

    public class Commands {

        @Command(aliases = {"skywars", "sw"}, desc = "Sky wars commands")
        @NestedCommand({NestedCommands.class})
        public void skyWarCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

        @Command(aliases = {"join", "j"},
                usage = "[Player] [Team Number]", desc = "Join the Minigame",
                anyFlags = true, min = 0, max = 2)
        public void joinSkyWarCmd(CommandContext args, CommandSender sender) throws CommandException {

            joinCmd(args, sender);
        }

        @Command(aliases = {"leave", "l"},
                usage = "[Player]", desc = "Leave the Minigame",
                min = 0, max = 1)
        public void leaveSkyWarCmd(CommandContext args, CommandSender sender) throws CommandException {

            leaveCmd(args, sender);
        }

        @Command(aliases = {"reset", "r"}, desc = "Reset the Minigame.",
                flags = "p",
                min = 0, max = 0)
        public void resetSkyWarCmd(CommandContext args, CommandSender sender) throws CommandException {

            resetCmd(args, sender);
        }

        @Command(aliases = {"start", "s"},
                usage = "", desc = "Minigame start command",
                anyFlags = true, min = 0, max = 0)
        public void startSkywarCmd(CommandContext args, CommandSender sender) throws CommandException {

            startCmd(args, sender);
        }
    }

    private WorldGuardPlugin getWorldGuard() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }

        return (WorldGuardPlugin) plugin;
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