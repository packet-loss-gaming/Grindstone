/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.bosses.detail.WBossDetail;
import gg.packetloss.grindstone.bosses.manager.wilderness.*;
import gg.packetloss.grindstone.city.engine.combat.PvMComponent;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseBlockDamagePreventionEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLocalSpawnEvent;
import gg.packetloss.grindstone.events.entity.item.DropClearPulseEvent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldGetQuery;
import gg.packetloss.grindstone.managedworld.ManagedWorldIsQuery;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.EnvironmentUtil.isFrozenBiome;
import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier;


@ComponentInformation(friendlyName = "Wilderness Core", desc = "Operate the wilderness.")
@Depend(components = {AdminComponent.class, SessionComponent.class,
        HighScoresComponent.class, ManagedWorldComponent.class})
public class WildernessCoreComponent extends BukkitComponent implements Listener, Runnable {
    private static WildernessCoreComponent componentInst;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private HighScoresComponent highScoresComponent;
    @InjectComponent
    private ManagedWorldComponent managedWorld;

    private long nextDropTime = 0;
    private LocalConfiguration config;

    // Boss Handlers
    private Fangz fangz = new Fangz();
    private FearKnight fearKnight = new FearKnight();
    private LostRogue rogue = new LostRogue();
    private StormBringer stormBringer = new StormBringer();
    private GraveDigger graveDigger = new GraveDigger();

    public WildernessCoreComponent() {
        componentInst = this;
    }

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        // Start tree growth task
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 0, 20 * 2);

        registerCommands(Commands.class);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    @Override
    public void run() {
        World wildernessWorld = managedWorld.get(ManagedWorldGetQuery.WILDERNESS);
        if (wildernessWorld.getTime() % 7 == 0) {
            for (Entity item : wildernessWorld.getEntitiesByClasses(Item.class)) {
                ItemStack stack = ((Item) item).getItemStack();

                if (stack.getAmount() > 1) continue;

                if (item.getTicksLived() > 20 * 60 && EnvironmentUtil.isSapling(stack.getType())) {
                    item.getLocation().getBlock().setType(stack.getType(), true);
                    item.remove();
                }
            }
        }
    }

    public static boolean isWildernessWorld(World world) {
        return componentInst.managedWorld.is(ManagedWorldIsQuery.ANY_WILDERNESS, world);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("entity.drop-count-max-level")
        public double dropCountMaxLevel = 20;
        @Setting("entity.drop-value-modifier")
        public double dropValueModifier = 1.25;
        @Setting("mini-bosses.lost-rogue")
        public int lostRogueChance = 1000;
        @Setting("mini-bosses.fear-knight")
        public int fearKnightChance = 100;
        @Setting("mini-bosses.fangz")
        public int fangzChance = 100;
        @Setting("mini-bosses.stormbringer")
        public int stormBringerChance = 100;
        @Setting("mini-bosses.gravedigger")
        public int graveDiggerChance = 100;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Monster)) return;

        Location location = event.getLocation();
        int level = getLevel(location);
        if (isWildernessWorld(location.getWorld()) && level > 1) {
            double max = entity.getMaxHealth();

            // We want the health to be boosted only after level 1
            // but not skip an increment
            entity.setMaxHealth(max * 5 * (level - 1));
            entity.setHealth(max * 5 * (level - 1));

            if (entity instanceof Zombie) {
                if (ChanceUtil.getChance(config.lostRogueChance)) {
                    rogue.bind(entity, new WBossDetail(level));
                } else if (ChanceUtil.getChance(config.fearKnightChance)) {
                    fearKnight.bind(entity, new WBossDetail(level));
                }
            } else if (entity instanceof Spider) {
                if (ChanceUtil.getChance(config.fangzChance)) {
                    fangz.bind(entity, new WBossDetail(level));
                }
            } else if (entity instanceof Skeleton) {
                if (ChanceUtil.getChance(config.stormBringerChance)) {
                    stormBringer.bind(entity, new WBossDetail(level));
                } else if (ChanceUtil.getChance(config.graveDiggerChance)) {
                    graveDigger.bind(entity, new WBossDetail(level));
                }
            }
        }
    }

    private static Set<EntityDamageEvent.DamageCause> ignoredDamage = new HashSet<>();

    static {
        ignoredDamage.add(EntityDamageEvent.DamageCause.WITHER);
        ignoredDamage.add(EntityDamageEvent.DamageCause.FIRE_TICK);
        ignoredDamage.add(EntityDamageEvent.DamageCause.POISON);
        ignoredDamage.add(EntityDamageEvent.DamageCause.CONTACT);
        ignoredDamage.add(EntityDamageEvent.DamageCause.DROWNING);
        ignoredDamage.add(EntityDamageEvent.DamageCause.FALL);
        ignoredDamage.add(EntityDamageEvent.DamageCause.STARVATION);
        ignoredDamage.add(EntityDamageEvent.DamageCause.SUFFOCATION);
        ignoredDamage.add(EntityDamageEvent.DamageCause.VOID);
        ignoredDamage.add(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
        ignoredDamage.add(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        final Entity entity = event.getEntity();

        Location location = entity.getLocation();
        if (!isWildernessWorld(location.getWorld())) return;

        if (event instanceof EntityDamageByEntityEvent) {
            if (onPlayerDamage((EntityDamageByEntityEvent) event)) {
                return;
            }
        }

        int level = getLevel(location);

        // Make Skeletons & Zombies burn faster
        if (entity instanceof Zombie || entity instanceof Skeleton) {
            if (event.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)
              && location.getBlock().getLightFromSky() == 15) {
                event.setDamage(event.getDamage() * 5 * level);
                return;
            }
        }

        if (!(entity instanceof Player) || ignoredDamage.contains(event.getCause())) return;

        event.setDamage(DamageModifier.BASE, event.getDamage() + ChanceUtil.getRandom(ChanceUtil.getRandom(level)) - 1);
        if (((Player) entity).isFlying() && event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
            DamageUtil.multiplyFinalDamage(event, 2);
        }
    }

    private static EDBEExtractor<Player, LivingEntity, Projectile> extractor = new EDBEExtractor<>(
      Player.class,
      LivingEntity.class,
      Projectile.class
    );

    public boolean onPlayerDamage(EntityDamageByEntityEvent event) {

        CombatantPair<Player, LivingEntity, Projectile> result = extractor.extractFrom(event);

        if (result == null) return false;

        final Player attacker = result.getAttacker();
        LivingEntity defender = result.getDefender();

        // The defender is a player, we don't want to print the HP, but at the same time
        // we don't want to allow the processing of onEntityDamage to continue,
        // so we return true instead of false to end the cycle.
        if (defender instanceof Player) {
            return true;
        }

        PvMComponent.printHealth(attacker, defender);
        return true;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {

        Entity entity = event.getEntity();

        if (!(entity instanceof Monster) || event.getDroppedExp() < 1) return;

        Location location = entity.getLocation();
        int level = getLevel(location);

        boolean isWilderness = isWildernessWorld(location.getWorld());
        if (!isWilderness) {
            return;
        }

        if (level > 1) {
            boolean isFrozenBiome = isFrozenBiome(location.getBlock().getBiome());

            List<ItemStack> drops = new ArrayList<>();

            int dropCountModifier = (int) Math.min(config.dropCountMaxLevel, level - 1);
            double dropValueModifier = Math.max(1, level * config.dropValueModifier);

            // Handle unique drops
            for (int i = 0; i < dropCountModifier; ++i) {
                if (ChanceUtil.getChance(100000 / dropValueModifier)) {
                    drops.add(CustomItemCenter.build(CustomItems.RED_FEATHER));
                }

                if (ChanceUtil.getChance(2000 / dropValueModifier)) {
                    drops.add(CustomItemCenter.build(CustomItems.POTION_OF_RESTITUTION));
                }

                if (ChanceUtil.getChance(2000 / dropValueModifier)) {
                    drops.add(CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION));
                }

                if (ChanceUtil.getChance((isFrozenBiome ? 1000 : 2000) / dropValueModifier)) {
                    drops.add(CustomItemCenter.build(CustomItems.ODE_TO_THE_FROZEN_KING));
                }
            }

            // Handle sacrificial pit generated drops
            drops.addAll(
              SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), dropCountModifier, dropValueModifier * 512)
            );

            // Handle double drop modifier
            if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_WILD_DROPS)) {
                drops.addAll(drops.stream().map(ItemStack::clone).collect(Collectors.toList()));
            }

            event.getDrops().addAll(drops);
            event.setDroppedExp(event.getDroppedExp() * level);
        }

        Player killer = ((Monster) entity).getKiller();
        if (killer != null) {
            highScoresComponent.update(killer, ScoreTypes.WILDERNESS_MOB_KILLS, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (!isWildernessWorld(player.getWorld()) || adminComponent.isAdmin(player)) return;

        List<ItemStack> drops = event.getDrops();
        MarketItemLookupInstance lookupInstance = MarketComponent.getLookupInstanceFromStacksImmediately(drops);
        drops.sort((o1, o2) -> {
            double o1SellPrice = lookupInstance.checkMaximumValue(o1).orElse(0d);
            double o2SellPrice = lookupInstance.checkMaximumValue(o2).orElse(0d);
            return (int) (o2SellPrice - o1SellPrice);
        });

        Iterator<ItemStack> it = drops.iterator();
        List<ItemStack> grave = new ArrayList<>();
        for (int kept = 9; kept > 0 && it.hasNext(); --kept) {
            ItemStack next = it.next();
            grave.add(next);
            it.remove();

            kept--;
        }

        Location location = player.getLocation();
        Block block = location.getBlock();
        if (WorldGuardBridge.canBuildAt(player, block)) {
            try {
                graveSupplier:
                {
                    checkGrave:
                    {
                        for (BlockFace face : BlockFace.values()) {
                            if (face.getModY() != 0) continue;
                            Block aBlock = block.getRelative(face);
                            if (aBlock.getType() == Material.CHEST) {
                                block = aBlock;
                                break checkGrave;
                            }
                        }
                        block.setType(Material.CHEST, true);
                    }
                    Chest chest = (Chest) block.getState();
                    it = grave.iterator();
                    Inventory blockInv = chest.getInventory();
                    for (int i = 0; it.hasNext(); ++i) {
                        while (blockInv.getItem(i) != null) {
                            ++i;
                            if (i >= blockInv.getSize()) {
                                ChatUtil.sendError(player, "Some items could not be added to your grave!");
                                break graveSupplier;
                            }
                        }
                        blockInv.setItem(i, it.next());
                        it.remove();
                    }
                    ChatUtil.sendNotice(player, "A grave has been created where you died.");
                }
            } catch (Exception ex) {
                log.warning("Location could not be found to create a grave for: " + player.getName());
                ex.printStackTrace();
            }
        }
        event.getDrops().addAll(grave);
        highScoresComponent.update(player, ScoreTypes.WILDERNESS_DEATHS, 1);
    }

    public int getLevel(Location location) {

        // Not in Wilderness
        if (!isWildernessWorld(location.getWorld())) {
            return 0;
        }

        // In Wilderness
        return Math.max(0, Math.max(Math.abs(location.getBlockX()), Math.abs(location.getBlockZ())) / 1000) + 1;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        final BlockState block = event.getBlock().getState();

        if (!isWildernessWorld(player.getWorld())) return;

        if (isEffectedOre(block.getType())) {

            ItemStack stack = player.getItemInHand();

            addPool(block, ItemUtil.fortuneLevel(stack), stack.containsEnchantment(Enchantment.SILK_TOUCH));
            highScoresComponent.update(player, ScoreTypes.WILDERNESS_ORES_MINED, 1);
        }  /* else if (block.getTypeId() == Material.SAPLING) {
            event.setCancelled(true);
            ChatUtil.sendError(player, "You cannot break that here.");
        }
        */

        event.setExpToDrop(event.getExpToDrop() * getLevel(block.getLocation()));

        /*
        if (isTree(block)) {

            Block treeBase = block.getBlock();
            while (treeBase.getTypeId() != Material.GRASS && treeBase.getTypeId() != Material.DIRT) {
                treeBase = treeBase.getRelative(BlockFace.DOWN);
                if (treeBase.getY() < 3) break;
            }

            treeBase = treeBase.getRelative(BlockFace.UP);

            treeBase.breakNaturally();
            treeBase.setTypeIdAndData(Material.SAPLING, block.getData().getData(), true);
        }
        */
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onApocalypseBlockDamage(ApocalypseBlockDamagePreventionEvent event) {
        if (isWildernessWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {

        Entity entity = event.getEntity();
        if (!(entity instanceof Creeper || entity instanceof Fireball)) return;

        Location loc = entity.getLocation();
        if (!isWildernessWorld(loc.getWorld())) return;

        float min = event.getRadius();
        event.setRadius(Math.min(entity instanceof Fireball ? 4 : 9, Math.max(min, (min + getLevel(loc)) / 2)));

    }

    private void handleExplosion(List<Block> blockList) {
        Iterator<Block> it = blockList.iterator();
        while (it.hasNext()) {
            Block next = it.next();
            if (isEffectedOre(next.getType())) {
                addPool(next.getState(), 0, false);
                continue;
            }

            if (next.getType() == Material.CHEST) {
                it.remove();
                continue;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (!isWildernessWorld(event.getLocation().getWorld())) return;

        event.setYield(.1F);

        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (!isWildernessWorld(event.getBlock().getWorld())) return;

        event.setYield(.1F);

        handleExplosion(event.blockList());
    }

    @EventHandler
    public void onDropClearPulse(DropClearPulseEvent event) {
        if (isWildernessWorld(event.getWorld())) {
            nextDropTime = System.currentTimeMillis() + (event.getSecondsLeft() * 1000);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLocalSpawn(ApocalypseLocalSpawnEvent event) {

        Location location = event.getLocation();

        if (!isWildernessWorld(location.getWorld())) return;

        int level = getLevel(location);
        if (!ChanceUtil.getChance(level * level)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (isWildernessWorld(player.getWorld()) && !adminComponent.isAdmin(player)) {
            Material typeId = event.getBlock().getType();
            if (isEffectedOre(typeId)) {
                event.setCancelled(true);
                ChatUtil.sendError(player, "You find yourself unable to place that ore.");
            } /* else if (typeId == Material.SAPLING) {
                event.setCancelled(true);
                ChatUtil.sendError(player, "You cannot plant that here.");
            }
            */
        }
    }

    public class Commands {

        @Command(aliases = {"wlevel", "wl"},
                usage = "[target level]", desc = "Get current wilderness level",
                flags = "", min = 0, max = 1)
        public void lostPotionOfRestitutionCmd(CommandContext args, CommandSender sender) throws CommandException {

            int level;
            if (args.argsLength() > 0) {
                level = args.getInteger(0);
            } else {
                level = Math.max(1, getLevel(PlayerUtil.checkPlayer(sender).getLocation()));
            }
            ChatUtil.sendNotice(sender, "Current Level: " + (level == 0 ? ChatColor.RED + "Not available" : level)
                    + ChatColor.YELLOW + ", Target Level: " + level + ".\n");

            DecimalFormat df = new DecimalFormat("#.#");

            ChatUtil.sendNotice(sender, "Damage Modifier: +" + (level - 1));
            ChatUtil.sendNotice(sender, "Ore Pool Modifier: " + df.format(getOreMod(level)) + "x");
            ChatUtil.sendNotice(sender, "Mob Health Modifier: " + (level > 1 ? 5 * (level - 1) : 1) + "x");
        }

        @Command(aliases = {"wboss"}, desc = "Boss Commands")
        @NestedCommand({BossCommands.class})
        @CommandPermissions("aurora.wilderness.boss")
        public void bossCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class BossCommands {

        @Command(aliases = {"lostrogue"},
                usage = "[level]", desc = "Spawn a lost rogue",
                flags = "", min = 0, max = 1)
        public void lostRogue(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            int level = args.argsLength() < 1 ? getLevel(player.getLocation()) : args.getInteger(0);
            if (level < 1) {
                throw new CommandException("You are not in a Wilderness world, please specify a level.");
            }
            Zombie zombie = player.getLocation().getWorld().spawn(player.getLocation(), Zombie.class);
            WBossDetail detail = new WBossDetail(level);
            rogue.bind(zombie, detail);
        }

        @Command(aliases = {"fangz"},
                usage = "[level]", desc = "Spawn a fangz",
                flags = "", min = 0, max = 1)
        public void fangz(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            int level = args.argsLength() < 1 ? getLevel(player.getLocation()) : args.getInteger(0);
            if (level < 1) {
                throw new CommandException("You are not in a Wilderness world, please specify a level.");
            }
            Spider spider = player.getLocation().getWorld().spawn(player.getLocation(), Spider.class);
            WBossDetail detail = new WBossDetail(level);
            fangz.bind(spider, detail);
        }

        @Command(aliases = {"fearknight"},
                usage = "[level]", desc = "Spawn a Fear Knight",
                flags = "", min = 0, max = 1)
        public void fearKnight(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            int level = args.argsLength() < 1 ? getLevel(player.getLocation()) : args.getInteger(0);
            if (level < 1) {
                throw new CommandException("You are not in a Wilderness world, please specify a level.");
            }
            Zombie zombie = player.getLocation().getWorld().spawn(player.getLocation(), Zombie.class);
            WBossDetail detail = new WBossDetail(level);
            fearKnight.bind(zombie, detail);
        }

        @Command(aliases = {"stormbringer"},
                usage = "[level]", desc = "Spawn a Storm Bringer",
                flags = "", min = 0, max = 1)
        public void stormBringer(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            int level = args.argsLength() < 1 ? getLevel(player.getLocation()) : args.getInteger(0);
            if (level < 1) {
                throw new CommandException("You are not in a Wilderness world, please specify a level.");
            }
            Skeleton skeleton = player.getLocation().getWorld().spawn(player.getLocation(), Skeleton.class);
            skeleton.getEquipment().setItemInHand(new ItemStack(Material.BOW));
            WBossDetail detail = new WBossDetail(level);
            stormBringer.bind(skeleton, detail);
        }

        @Command(aliases = {"gravedigger"},
                usage = "[level]", desc = "Spawn a Grave Digger",
                flags = "", min = 0, max = 1)
        public void graveDigger(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            int level = args.argsLength() < 1 ? getLevel(player.getLocation()) : args.getInteger(0);
            if (level < 1) {
                throw new CommandException("You are not in a Wilderness world, please specify a level.");
            }
            Skeleton skeleton = player.getLocation().getWorld().spawn(player.getLocation(), Skeleton.class);
            skeleton.getEquipment().setItemInHand(new ItemStack(Material.BOW));
            WBossDetail detail = new WBossDetail(level);
            graveDigger.bind(skeleton, detail);
        }
    }

    private int getOreMod(int level) {
        double modifier = Math.max(1, (level * 3));
        if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_WILD_ORES)) {
            modifier *= 2;
        }
        return (int) modifier;
    }

    private void addPool(final BlockState block, final int fortuneLevel, final boolean hasSilkTouch) {

        final Location location = block.getLocation();
        final World world = location.getWorld();

        ItemStack generalDrop = EnvironmentUtil.getOreDrop(block.getType(), hasSilkTouch);
        final int fortune = EnvironmentUtil.isOre(generalDrop.getType()) ? 0 : fortuneLevel;
        final int times = ChanceUtil.getRandom(getOreMod(getLevel(location)));
        final float vol = ((float) 1 / times);
        IntegratedRunnable dropper = new IntegratedRunnable() {
            @Override
            public boolean run(int timesL) {

                if (nextDropTime != 0 && System.currentTimeMillis() < nextDropTime) return false;

                for (int i = 0; i < ItemUtil.fortuneModifier(block.getType(), fortune); i++) {
                    world.dropItem(location, EnvironmentUtil.getOreDrop(block.getType(), hasSilkTouch));
                }
                world.playSound(location, Sound.ENTITY_BLAZE_BURN, Math.min(1, (((float) timesL / times) * .6F) + vol), 0);
                return true;
            }

            @Override
            public void end() {

                world.playSound(location, Sound.ENTITY_BLAZE_DEATH, .2F, 0);
            }
        };

        TimedRunnable runnable = new TimedRunnable(dropper, times);

        BukkitTask task = server.getScheduler().runTaskTimer(inst, runnable, 20, 20);

        runnable.setTask(task);
    }

    /*
    private static final int[] ores = {
            Material.GOLD_ORE
    };
    */

    private boolean isEffectedOre(Material material) {

        /*
        for (int ore : ores) {
            if (ore == typeId) return true;
        }
        */
        return EnvironmentUtil.isOre(material);
    }
}