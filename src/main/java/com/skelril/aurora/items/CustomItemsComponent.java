/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.anticheat.AntiCheatCompatibilityComponent;
import com.skelril.aurora.city.engine.pvp.PvPComponent;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.events.custom.item.HymnSingEvent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.events.entity.ProjectileTickEvent;
import com.skelril.aurora.items.specialattack.SpecialAttack;
import com.skelril.aurora.items.specialattack.attacks.hybrid.fear.Curse;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.EvilFocus;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.LifeLeech;
import com.skelril.aurora.items.specialattack.attacks.hybrid.unleashed.Speed;
import com.skelril.aurora.items.specialattack.attacks.melee.fear.*;
import com.skelril.aurora.items.specialattack.attacks.melee.unleashed.DoomBlade;
import com.skelril.aurora.items.specialattack.attacks.melee.unleashed.HealingLight;
import com.skelril.aurora.items.specialattack.attacks.melee.unleashed.Regen;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.Disarm;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.FearBomb;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.FearStrike;
import com.skelril.aurora.items.specialattack.attacks.ranged.fear.MagicChain;
import com.skelril.aurora.items.specialattack.attacks.ranged.misc.MobAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.unleashed.Famine;
import com.skelril.aurora.items.specialattack.attacks.ranged.unleashed.GlowingFog;
import com.skelril.aurora.prayer.Prayer;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerFX.HulkFX;
import com.skelril.aurora.prayer.PrayerFX.NecrosisFX;
import com.skelril.aurora.util.*;
import com.skelril.aurora.util.extractor.entity.CombatantPair;
import com.skelril.aurora.util.extractor.entity.EDBEExtractor;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.InventoryUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import com.skelril.aurora.util.item.custom.CustomItems;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.skelril.aurora.events.custom.item.HymnSingEvent.Hymn;

//import net.gravitydevelopment.anticheat.check.CheckType;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Custom Items Component", desc = "Custom Items")
@Depend(components = {SessionComponent.class, AdminComponent.class,
        AntiCheatCompatibilityComponent.class, PvPComponent.class,
        PrayerComponent.class
})
public class CustomItemsComponent extends BukkitComponent implements Listener {

    private static final CommandBook inst = CommandBook.inst();
    private static final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private AntiCheatCompatibilityComponent antiCheat;
    @InjectComponent
    private PrayerComponent prayers;

    private List<String> players = new ArrayList<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    public CustomItemSession getSession(Player player) {

        return sessions.getSession(CustomItemSession.class, player);
    }

    private SpecialAttackEvent callSpec(Player owner, SpecType context, SpecialAttack spec) {

        SpecialAttackEvent event = new SpecialAttackEvent(owner, context, spec);

        server.getPluginManager().callEvent(event);

        return event;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {

        Entity healed = event.getEntity();

        if (healed instanceof Player) {

            Player player = (Player) healed;

            if (ItemUtil.isItem(player.getInventory().getHelmet(), CustomItems.ANCIENT_CROWN)) {
                event.setAmount(event.getAmount() * 2.5);
            }
        }
    }

    private static Set<EntityDamageEvent.DamageCause> ignoredCauses = new HashSet<>();

    static {
        ignoredCauses.add(EntityDamageEvent.DamageCause.POISON);
        ignoredCauses.add(EntityDamageEvent.DamageCause.WITHER);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player && !ignoredCauses.contains(event.getCause())) {

            Player player = (Player) entity;
            CustomItemSession session = getSession(player);
            ItemStack[] contents = player.getInventory().getContents();

            if (session.canSpec(SpecType.RED_FEATHER) && ItemUtil.hasItem(player, CustomItems.RED_FEATHER)) {

                final int redQD = ItemUtil.countItemsOfType(contents, ItemID.REDSTONE_DUST);
                final int redQB = 9 * ItemUtil.countItemsOfType(contents, BlockID.REDSTONE_BLOCK);

                int redQ = redQD + redQB;

                if (redQ > 0) {

                    contents = ItemUtil.removeItemOfType(contents, ItemID.REDSTONE_DUST);
                    contents = ItemUtil.removeItemOfType(contents, BlockID.REDSTONE_BLOCK);

                    player.getInventory().setContents(contents);

                    final double dmg = event.getDamage();
                    final int k = (dmg > 80 ? 16 : dmg > 40 ? 8 : dmg > 20 ? 4 : 2);

                    final double blockable = redQ * k;
                    final double blocked = blockable - (blockable - dmg);

                    redQ = (int) ((blockable - blocked) / k);

                    World w = player.getWorld();

                    while (redQ / 9 > 0) {
                        ItemStack is = new ItemStack(BlockID.REDSTONE_BLOCK);
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(is);
                        } else {
                            w.dropItem(player.getLocation(), is);
                        }
                        redQ -= 9;
                    }

                    while (redQ > 0) {
                        int r = Math.min(64, redQ);
                        ItemStack is = new ItemStack(ItemID.REDSTONE_DUST, r);
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(is);
                        } else {
                            w.dropItem(player.getLocation(), is);
                        }
                        redQ -= r;
                    }

                    //noinspection deprecation
                    player.updateInventory();

                    event.setDamage(Math.max(0, dmg - blocked));
                    player.setFireTicks(0);

                    // Update the session
                    session.updateSpec(SpecType.RED_FEATHER, (long) (blocked * 75));
                }
            }
        }
    }

    private static EDBEExtractor<LivingEntity, Player, Projectile> necrosisExtractor = new EDBEExtractor<>(
            LivingEntity.class,
            Player.class,
            Projectile.class
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void necrosis(EntityDamageByEntityEvent event) {

        CombatantPair<LivingEntity, Player, Projectile> result = necrosisExtractor.extractFrom(event);

        if (result == null) return;

        Player defender = result.getDefender();
        if ((ItemUtil.hasNectricArmour(defender) || ItemUtil.hasNecrosArmour(defender)) && ChanceUtil.getChance(4)) {
            LivingEntity attacker = result.getAttacker();
            if (attacker instanceof Player) {
                NecrosisFX necrosis = new NecrosisFX();
                necrosis.setBeneficiary(defender);
                Prayer prayer = PrayerComponent.constructPrayer((Player) attacker, necrosis, 5000);
                prayers.influencePlayer((Player) attacker, prayer);
                defender.chat("Taste necrosis!");
            } else {
                EffectUtil.Necros.deathStrike(defender, Math.max(5, event.getDamage()));
            }
        }
    }

    private static Queue<EntityDamageByEntityEvent> attackQueue = new LinkedList<>();

    private static EDBEExtractor<Player, LivingEntity, Projectile> specExtractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Projectile.class
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    public void specAttack(EntityDamageByEntityEvent event) {

        // Handle cancellation here so that we don't end up with a memory leak from the queue
        if (attackQueue.poll() != null || event.isCancelled()) return;

        CombatantPair<Player, LivingEntity, Projectile> result = specExtractor.extractFrom(event);

        if (result == null) return;

        ItemStack launcher = null;
        if (result.hasProjectile()) {
            Projectile projectile = result.getProjectile();
            if (projectile.hasMetadata("launcher")) {
                Object test = projectile.getMetadata("launcher").get(0).value();
                if (test instanceof ItemStack) {
                    launcher = (ItemStack) test;
                }
            }

            if (launcher == null) return;
        }

        Player owner = result.getAttacker();
        LivingEntity target = result.getDefender();

        if (target != null && owner != target) {

            CustomItemSession session = getSession(owner);

            SpecType specType = null;
            SpecialAttack spec = null;

            if (launcher != null) {

                specType = SpecType.RANGED;

                if (ItemUtil.isHoldingItem(owner, CustomItems.FEAR_BOW)) {
                    switch (ChanceUtil.getRandom(5)) {
                        case 1:
                            Disarm disarmSpec = new Disarm(owner, target);
                            if (disarmSpec.getItemStack() != null) {
                                spec = disarmSpec;
                                break;
                            }
                        case 2:
                            spec = new Curse(owner, target);
                            break;
                        case 3:
                            spec = new MagicChain(owner, target);
                            break;
                        case 4:
                            spec = new FearStrike(owner, target);
                            break;
                        case 5:
                            spec = new FearBomb(owner, target);
                            break;
                    }
                } else if (ItemUtil.isHoldingItem(owner, CustomItems.UNLEASHED_BOW)) {
                    switch (ChanceUtil.getRandom(5)) {
                        case 1:
                            spec = new Famine(owner, target);
                            break;
                        case 2:
                            spec = new LifeLeech(owner, target);
                            break;
                        case 3:
                            spec = new EvilFocus(owner, target);
                            break;
                        case 4:
                            spec = new Speed(owner, target);
                            break;
                        case 5:
                            spec = new GlowingFog(owner, target);
                            break;
                    }
                }
            } else {

                specType = SpecType.MELEE;

                if (ItemUtil.isHoldingItem(owner, CustomItems.FEAR_SWORD)) {
                    switch (ChanceUtil.getRandom(6)) {
                        case 1:
                            spec = new Confuse(owner, target);
                            break;
                        case 2:
                            spec = new FearBlaze(owner, target);
                            break;
                        case 3:
                            spec = new Curse(owner, target);
                            break;
                        case 4:
                            spec = new Weaken(owner, target);
                            break;
                        case 5:
                            spec = new Decimate(owner, target);
                            break;
                        case 6:
                            spec = new SoulSmite(owner, target);
                            break;
                    }
                } else if (ItemUtil.isHoldingItem(owner, CustomItems.UNLEASHED_SWORD)) {
                    switch (ChanceUtil.getRandom(6)) {
                        case 1:
                            spec = new EvilFocus(owner, target);
                            break;
                        case 2:
                            spec = new HealingLight(owner, target);
                            break;
                        case 3:
                            spec = new Speed(owner, target);
                            break;
                        case 4:
                            spec = new Regen(owner, target);
                            break;
                        case 5:
                            spec = new DoomBlade(owner, target);
                            break;
                        case 6:
                            spec = new LifeLeech(owner, target);
                            break;
                    }
                }
            }

            if (spec != null && session.canSpec(specType)) {

                SpecialAttackEvent specEvent = callSpec(owner, specType, spec);

                if (!specEvent.isCancelled()) {
                    session.updateSpec(specType, specEvent.getSpec().getCoolDown());
                    specEvent.getSpec().activate();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void damageModifier(EntityDamageByEntityEvent event) {

        if (DamageUtil.remove(event.getDamager(), event.getEntity())) {
            attackQueue.add(event);
            return;
        }

        CombatantPair<Player, LivingEntity, Projectile> result = specExtractor.extractFrom(event);

        if (result == null) return;

        ItemStack launcher = null;
        if (result.hasProjectile()) {
            Projectile projectile = result.getProjectile();
            if (projectile.hasMetadata("launcher")) {
                Object test = projectile.getMetadata("launcher").get(0).value();
                if (test instanceof ItemStack) {
                    launcher = (ItemStack) test;
                }
            }

            if (launcher == null) return;
        }

        Player owner = result.getAttacker();

        double modifier = 1;

        ItemStack targetItem = launcher;

        if (targetItem == null) {
            targetItem = owner.getItemInHand();
        }

        Map<String, String> map = ItemUtil.getItemTags(targetItem);

        if (map != null) {
            String modifierString = map.get(ChatColor.RED + "Damage Modifier");
            if (modifierString != null) {
                try {
                    modifier = Double.parseDouble(modifierString);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        event.setDamage(event.getDamage() * modifier);
    }

    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();
        Entity shooter = null;

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Entity) {
            shooter = (Entity) source;
        }

        if (shooter != null && shooter instanceof Player && projectile.hasMetadata("launcher")) {

            Object test = projectile.getMetadata("launcher").get(0).value();

            if (!(test instanceof ItemStack)) return;

            ItemStack launcher = (ItemStack) test;

            final Player owner = (Player) shooter;
            final Location targetLoc = projectile.getLocation();

            final boolean unleashedBow = ItemUtil.isItem(launcher, CustomItems.UNLEASHED_BOW);
            final boolean masterBow = ItemUtil.isItem(launcher, CustomItems.MASTER_BOW);

            if ((unleashedBow || masterBow) && !projectile.hasMetadata("splashed")) {

                projectile.setMetadata("splashed", new FixedMetadataValue(inst, true));

                IntegratedRunnable vacuum = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {

                        EnvironmentUtil.generateRadialEffect(targetLoc, Effect.ENDER_SIGNAL);

                        targetLoc.getWorld().getEntitiesByClasses(Item.class).stream().filter(e -> e.isValid()
                                && e.getLocation().distanceSquared(targetLoc) <= 16).forEach(e -> {
                            e.teleport(owner);
                        });
                        return true;
                    }

                    @Override
                    public void end() {

                        EnvironmentUtil.generateRadialEffect(targetLoc, Effect.ENDER_SIGNAL);

                        for (Entity e : targetLoc.getWorld().getEntitiesByClasses(Monster.class, Player.class)) {
                            if (!e.isValid() || e.equals(owner)) continue;
                            if (e.getLocation().distanceSquared(targetLoc) <= 16) {
                                if (e instanceof Item) {
                                    e.teleport(owner);
                                    continue;
                                }
                                if (e instanceof Player) {
                                    if (masterBow || !PvPComponent.allowsPvP(owner, (Player) e)) continue;
                                }
                                e.setFireTicks(20 * (unleashedBow ? 4 : 2));
                            }
                        }
                    }
                };
                TimedRunnable runnable = new TimedRunnable(vacuum, 3);
                runnable.setTask(server.getScheduler().runTaskTimer(inst, runnable, 1, 10));
            }

            CustomItemSession session = getSession(owner);

            if (session.canSpec(SpecType.ANIMAL_BOW)) {
                Class<? extends LivingEntity> type = null;
                if (ItemUtil.isItem(launcher, CustomItems.BAT_BOW)) {
                    type = Bat.class;
                } else if (ItemUtil.isItem(launcher, CustomItems.CHICKEN_BOW)) {
                    type = Chicken.class;
                }

                if (type != null) {
                    SpecialAttackEvent specEvent = callSpec(owner, SpecType.RANGED, new MobAttack(owner, targetLoc, type));
                    if (!specEvent.isCancelled()) {
                        session.updateSpec(SpecType.ANIMAL_BOW, specEvent.getSpec().getCoolDown());
                        specEvent.getSpec().activate();
                    }
                }
            }

            if (!session.canSpec(SpecType.RANGED)) {

                if (ItemUtil.isItem(launcher, CustomItems.FEAR_BOW)) {
                    if (!targetLoc.getWorld().isThundering() && targetLoc.getBlock().getLightFromSky() > 0) {

                        server.getPluginManager().callEvent(new RapidHitEvent(owner));

                        // Simulate a lightning strike
                        targetLoc.getWorld().strikeLightningEffect(targetLoc);
                        for (Entity e : projectile.getNearbyEntities(2, 4, 2)) {
                            if (!e.isValid() || !(e instanceof LivingEntity)) continue;
                            // Pig Zombie
                            if (e instanceof Pig) {
                                e.getWorld().spawn(e.getLocation(), PigZombie.class);
                                e.remove();
                                continue;
                            }
                            // Creeper
                            if (e instanceof Creeper) {
                                ((Creeper) e).setPowered(true);
                            }
                            // Player
                            if (e instanceof Player) {
                                if (!PvPComponent.allowsPvP(owner, (Player) e)) continue;
                            }

                            ((LivingEntity) e).damage(5, owner);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArrowTick(ProjectileTickEvent event) {

        Projectile projectile = event.getEntity();
        Entity shooter = null;

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Entity) {
            shooter = (Entity) source;
        }

        if (shooter != null && shooter instanceof Player && projectile.hasMetadata("launcher")) {

            Object test = projectile.getMetadata("launcher").get(0).value();

            if (!(test instanceof ItemStack)) return;

            ItemStack launcher = (ItemStack) test;

            final Location location = projectile.getLocation();
            if (ItemUtil.isItem(launcher, CustomItems.BAT_BOW)) {

                if (!ChanceUtil.getChance(5)) return;
                server.getScheduler().runTaskLater(inst, () -> {
                    final Bat bat = location.getWorld().spawn(location, Bat.class);
                    bat.setRemoveWhenFarAway(true);
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (bat.isValid()) {
                            bat.remove();
                            for (int i = 0; i < 20; i++) {
                                bat.getWorld().playEffect(bat.getLocation(), Effect.SMOKE, 0);
                            }
                        }
                    }, 20 * 3);
                }, 3);
            } else if (ItemUtil.isItem(launcher, CustomItems.CHICKEN_BOW)) {

                if (!ChanceUtil.getChance(5)) return;
                server.getScheduler().runTaskLater(inst, () -> {
                    final Chicken chicken = location.getWorld().spawn(location, Chicken.class);
                    chicken.setRemoveWhenFarAway(true);
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (chicken.isValid()) {
                            chicken.remove();
                            for (int i = 0; i < 20; i++) {
                                chicken.getWorld().playEffect(chicken.getLocation(), Effect.SMOKE, 0);
                            }
                        }
                    }, 20 * 3);
                }, 3);
            }
        }
    }


    private static ItemCondenser summationCondenser = new ItemCondenser();

    static {
        // Coal
        summationCondenser.addSupport(new ItemStack(ItemID.COAL, 9), new ItemStack(BlockID.COAL_BLOCK, 1));

        // Iron
        summationCondenser.addSupport(new ItemStack(ItemID.IRON_BAR, 9), new ItemStack(BlockID.IRON_BLOCK, 1));

        // Gold
        summationCondenser.addSupport(new ItemStack(ItemID.GOLD_NUGGET, 9), new ItemStack(ItemID.GOLD_BAR, 1));
        summationCondenser.addSupport(new ItemStack(ItemID.GOLD_BAR, 9), new ItemStack(BlockID.GOLD_BLOCK, 1));

        // Redstone
        summationCondenser.addSupport(new ItemStack(ItemID.REDSTONE_DUST, 9), new ItemStack(BlockID.REDSTONE_BLOCK, 1));

        // Lapis
        summationCondenser.addSupport(new ItemStack(ItemID.INK_SACK, 9, (byte) 4), new ItemStack(BlockID.LAPIS_LAZULI_BLOCK, 1));

        // Diamond
        summationCondenser.addSupport(new ItemStack(ItemID.DIAMOND, 9), new ItemStack(BlockID.DIAMOND_BLOCK, 1));

        // Emerald
        summationCondenser.addSupport(new ItemStack(ItemID.EMERALD, 9), new ItemStack(BlockID.EMERALD_BLOCK, 1));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            // Hymns
            boolean isPhantomH = ItemUtil.isItem(itemStack, CustomItems.PHANTOM_HYMN);
            boolean isChickenH = ItemUtil.isItem(itemStack, CustomItems.CHICKEN_HYMN);
            boolean isSummatiH = ItemUtil.isItem(itemStack, CustomItems.HYMN_OF_SUMMATION);
            // Scrolls
            boolean isScrollOfSummation = ItemUtil.isItem(itemStack, CustomItems.SCROLL_OF_SUMMATION);
            if (handleRightClick(player, event.getClickedBlock().getLocation(), itemStack)) {
                event.setCancelled(true);
            } else if (isPhantomH || isChickenH || isSummatiH) {
                ChatUtil.sendNotice(player, "You sing the hymn...");
                Event e;
                if (isSummatiH) {
                    e = new HymnSingEvent(player, Hymn.SUMMATION);
                } else if (isPhantomH) {
                    e = new HymnSingEvent(player, Hymn.PHANTOM);
                } else {
                    e = new HymnSingEvent(player, Hymn.CHICKEN);
                }
                //noinspection AccessStaticViaInstance
                inst.callEvent(e);
            } else if (isScrollOfSummation) {
                ItemStack[] result = summationCondenser.operate(player.getInventory().getContents());
                if (result != null) {
                    player.getInventory().setContents(result);
                    ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION), 1, false);
                    ChatUtil.sendNotice(player, ChatColor.GOLD, "The scroll glows brightly before turning to dust...");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        Player player = event.getPlayer();
        Hymn hymn = event.getHymn();

        switch (hymn) {
            case CHICKEN:
                player.getNearbyEntities(4, 4, 4).stream()
                        .filter(e -> (e instanceof Item || e instanceof Chicken)).limit(30).forEach(e -> {
                    Location l = e.getLocation();
                    if (e instanceof Item) {
                        for (int i = 0; i < 3; i++) {
                            Chicken chicken = l.getWorld().spawn(l, Chicken.class);
                            chicken.setRemoveWhenFarAway(true);
                        }
                        e.remove();
                        ChatUtil.sendNotice(player, "The item transforms into chickens!");
                    } else if (((Chicken) e).getRemoveWhenFarAway()) {
                        if (ChanceUtil.getChance(3)) {
                            l.getWorld().dropItem(l, new ItemStack(ItemID.COOKED_CHICKEN));
                        }
                        e.remove();
                    }
                });
                break;
            case SUMMATION:
                ItemStack[] result = summationCondenser.operate(player.getInventory().getContents());
                if (result != null) {
                    player.getInventory().setContents(result);
                    //noinspection deprecation
                    player.updateInventory();
                    ChatUtil.sendNotice(player, ChatColor.GOLD, "The hymn glows brightly...");
                }
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        if (event.getRightClicked() instanceof Cow && ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET)) {
            server.getScheduler().runTaskLater(inst, () -> {
                if (!ItemUtil.swapItem(player.getInventory(), CustomItems.MAGIC_BUCKET, CustomItems.MAD_MILK)) {
                    ChatUtil.sendError(player, "Your inventory is too full!");
                    return;
                }
                if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
                    if (player.getAllowFlight()) {
                        ChatUtil.sendNotice(player, "The power of the bucket fades.");
                    }
                    player.setAllowFlight(false);
                }
            }, 1);
            event.setCancelled(true);
            return;
        }

        if (handleRightClick(player, event.getRightClicked().getLocation(), itemStack)) {
            event.setCancelled(true);
        }
        //noinspection deprecation
        server.getScheduler().runTaskLater(inst, player::updateInventory, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        if (handleRightClick(player, event.getBlockClicked().getLocation(), itemStack)) {
            event.setCancelled(true);
        }
        //noinspection deprecation
        server.getScheduler().runTaskLater(inst, player::updateInventory, 1);
    }

    public boolean handleRightClick(final Player player, Location location, ItemStack itemStack) {

        if (admin.isAdmin(player)) return false;

        final long currentTime = System.currentTimeMillis();

        if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET)) {
            player.setAllowFlight(!player.getAllowFlight());
            if (player.getAllowFlight()) {
                player.setFlySpeed(.4F);
                // antiCheat.exempt(player, CheckType.FLY);
                ChatUtil.sendNotice(player, "The bucket glows brightly.");
            } else {
                player.setFlySpeed(.1F);
                // antiCheat.unexempt(player, CheckType.FLY);
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            return true;
        } else if (ItemUtil.isItem(itemStack, CustomItems.PIXIE_DUST)) {

            if (player.getAllowFlight()) return false;

            if (players.contains(player.getName())) {

                ChatUtil.sendError(player, "You need to wait to regain your faith, and trust.");
                return false;
            }

            player.setAllowFlight(true);
            player.setFlySpeed(.6F);
            // antiCheat.exempt(player, CheckType.FLY);

            ChatUtil.sendNotice(player, "You use the Pixie Dust to gain flight.");

            IntegratedRunnable integratedRunnable = new IntegratedRunnable() {
                @Override
                public boolean run(int times) {

                    // Just get out of here you stupid players who don't exist!
                    if (!player.isValid()) return true;

                    if (player.getAllowFlight()) {
                        int c = ItemUtil.countItemsOfName(player.getInventory().getContents(), CustomItems.PIXIE_DUST.toString()) - 1;

                        if (c >= 0) {
                            ItemStack[] pInventory = player.getInventory().getContents();
                            pInventory = ItemUtil.removeItemOfName(pInventory, CustomItems.PIXIE_DUST.toString());
                            player.getInventory().setContents(pInventory);

                            int amount = Math.min(c, 64);
                            while (amount > 0) {
                                player.getInventory().addItem(CustomItemCenter.build(CustomItems.PIXIE_DUST, amount));
                                c -= amount;
                                amount = Math.min(c, 64);
                            }

                            //noinspection deprecation
                            player.updateInventory();

                            if (System.currentTimeMillis() >= currentTime + 13000) {
                                ChatUtil.sendNotice(player, "You use some more Pixie Dust to keep flying.");
                            }
                            return false;
                        }
                        ChatUtil.sendWarning(player, "The effects of the Pixie Dust are about to wear off!");
                    }
                    return true;
                }

                @Override
                public void end() {

                    if (player.isValid()) {
                        if (player.getAllowFlight()) {
                            ChatUtil.sendNotice(player, "You are no longer influenced by the Pixie Dust.");
                            // antiCheat.unexempt(player, CheckType.FLY);
                        }
                        player.setFallDistance(0);
                        player.setAllowFlight(false);
                        player.setFlySpeed(.1F);
                    }
                }
            };

            TimedRunnable runnable = new TimedRunnable(integratedRunnable, 1);
            BukkitTask task = server.getScheduler().runTaskTimer(inst, runnable, 0, 20 * 15);
            runnable.setTask(task);
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {

        Player player = event.getPlayer();

        if (event.isSneaking() && player.getAllowFlight() && player.isOnGround() && !admin.isAdmin(player)) {

            if (!ItemUtil.hasItem(player, CustomItems.PIXIE_DUST)) return;

            player.setAllowFlight(false);
            // antiCheat.unexempt(player, CheckType.FLY);
            ChatUtil.sendNotice(player, "You are no longer influenced by the Pixie Dust.");

            final String playerName = player.getName();

            players.add(playerName);

            server.getScheduler().runTaskLater(inst, () -> players.remove(playerName), 20 * 30);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET)) {
            server.getScheduler().runTaskLater(inst, () -> {
                if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
                    if (player.getAllowFlight()) {
                        ChatUtil.sendNotice(player, "The power of the bucket fades.");
                    }
                    player.setAllowFlight(false);
                    // antiCheat.unexempt(player, CheckType.FLY);
                }
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        InventoryType type = event.getInventory().getType();
        InventoryAction action = event.getAction();

        if (type.equals(InventoryType.ANVIL)) {
            if (action.equals(InventoryAction.NOTHING)) return;
            if (InventoryUtil.getMoveClicks().contains(event.getClick())) {
                event.setResult(Event.Result.DENY);
                ChatUtil.sendError(player, "You cannot move that here.");
                return;
            }

            int rawSlot = event.getRawSlot();

            if (rawSlot < 2) {
                if (InventoryUtil.getPlaceActions().contains(action) && ItemUtil.isNamed(cursorItem)) {
                    boolean isCustomItem = ItemUtil.isAuthenticCustomItem(cursorItem.getItemMeta().getDisplayName());

                    if (!isCustomItem) return;

                    event.setResult(Event.Result.DENY);
                    ChatUtil.sendError(player, "You cannot place that here.");
                }
            } else if (rawSlot == 2) {
                if (InventoryUtil.getPickUpActions().contains(action) && ItemUtil.isNamed(currentItem)) {
                    boolean isCustomItem = ItemUtil.isAuthenticCustomItem(currentItem.getItemMeta().getDisplayName());

                    if (!isCustomItem) return;

                    event.setResult(Event.Result.DENY);
                    ChatUtil.sendError(player, "You cannot name this item that name.");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getInventory().getType().equals(InventoryType.ANVIL)) {

            for (int i : event.getRawSlots()) {
                if (i + 1 <= event.getInventory().getSize()) {
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        if (!player.getAllowFlight()) return;

        ItemStack[] chestContents = event.getInventory().getContents();
        if (!ItemUtil.findItemOfName(chestContents, CustomItems.MAGIC_BUCKET.toString())) return;

        if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
            if (player.getAllowFlight()) {
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            player.setAllowFlight(false);
            // antiCheat.unexempt(player, CheckType.FLY);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        ItemStack[] drops = event.getDrops().toArray(new ItemStack[event.getDrops().size()]);

        if (ItemUtil.findItemOfName(drops, CustomItems.MAGIC_BUCKET.toString())) {
            if (player.getAllowFlight()) {
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            player.setAllowFlight(false);
            // antiCheat.unexempt(player, CheckType.FLY);
        }

        getSession(player).addDeathPoint(player.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if (ItemUtil.isItem(stack, CustomItems.GOD_FISH)) {
            player.chat("The fish flow within me!");
            new HulkFX().add(player);
        } else if (ItemUtil.isItem(stack, CustomItems.POTION_OF_RESTITUTION)) {
            Location lastLoc = getSession(player).getRecentDeathPoint();
            if (lastLoc != null) {
                if (!player.teleport(lastLoc)) {
                    ChatUtil.sendError(player, "Location Information: X: "
                            + lastLoc.getBlockX() + ", Y: "
                            + lastLoc.getBlockY() + ", Z: "
                            + lastLoc.getBlockZ() + " in "
                            + lastLoc.getWorld().getName() + '.'
                    );
                }
            } else {
                ChatUtil.sendError(player, "No previous death points are known the the potion.");
            }
        } else if (ItemUtil.isItem(stack, CustomItems.MAD_MILK)) {
            server.getScheduler().runTaskLater(inst, () -> player.getInventory().setItemInHand(CustomItemCenter.build(CustomItems.MAGIC_BUCKET)), 1);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onXPPickUp(PlayerExpChangeEvent event) {

        Player player = event.getPlayer();

        boolean hasCrown = ItemUtil.isItem(player.getInventory().getHelmet(), CustomItems.ANCIENT_CROWN);

        int exp = event.getAmount();
        if (hasCrown) {
            exp *= 2;
        }

        if (ItemUtil.hasAncientArmour(player) || ItemUtil.hasNecrosArmour(player)) {
            ItemStack[] armour = player.getInventory().getArmorContents();
            ItemStack is = CollectionUtil.getElement(armour);
            if (exp > is.getDurability()) {
                exp -= is.getDurability();
                is.setDurability((short) 0);
            } else {
                is.setDurability((short) (is.getDurability() - exp));
                exp = 0;
            }
            player.getInventory().setArmorContents(armour);
            event.setAmount(exp);
        } else if (hasCrown) {
            event.setAmount(exp);
        }
    }

    private static ItemCondenser goldCondenser = new ItemCondenser();

    static {
        goldCondenser.addSupport(new ItemStack(ItemID.GOLD_NUGGET, 9), new ItemStack(ItemID.GOLD_BAR, 1));
        goldCondenser.addSupport(new ItemStack(ItemID.GOLD_BAR, 9), new ItemStack(BlockID.GOLD_BLOCK, 1));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItem().getItemStack();

        if (itemStack.getTypeId() == ItemID.GOLD_BAR || itemStack.getTypeId() == ItemID.GOLD_NUGGET) {

            if (!(ItemUtil.isItem(player.getInventory().getHelmet(), CustomItems.ANCIENT_CROWN)
                    || ItemUtil.hasItem(player, CustomItems.IMBUED_CRYSTAL))) {
                return;
            }
            server.getScheduler().runTaskLater(inst, () -> {
                ItemStack[] result = goldCondenser.operate(player.getInventory().getContents());
                if (result != null) {
                    player.getInventory().setContents(result);
                    //noinspection deprecation
                    player.updateInventory();
                }
            }, 1);
        }
    }

    public static class CustomItemSession extends PersistentSession {

        private static final long MAX_AGE = TimeUnit.DAYS.toMillis(3);

        private HashMap<SpecType, Long> specMap = new HashMap<>();
        private LinkedList<Location> recentDeathLocations = new LinkedList<>();

        protected CustomItemSession() {
            super(MAX_AGE);
        }

        public void updateSpec(SpecType type) {

            specMap.put(type, System.currentTimeMillis());
        }

        public void updateSpec(SpecType type, long additionalDelay) {

            specMap.put(type, System.currentTimeMillis() + additionalDelay);
        }

        public boolean canSpec(SpecType type) {

            return !specMap.containsKey(type) || System.currentTimeMillis() - specMap.get(type) >= type.getDelay();
        }

        public void addDeathPoint(Location deathPoint) {

            recentDeathLocations.add(0, deathPoint.clone());
            while (recentDeathLocations.size() > 5) {
                recentDeathLocations.pollLast();
            }
        }

        public Location getRecentDeathPoint() {

            return recentDeathLocations.poll();
        }
    }

    public enum SpecType {

        RED_FEATHER(1000),
        RANGED(3800),
        MELEE(3800),
        ANIMAL_BOW(15000);

        private final long delay;

        private SpecType(long delay) {

            this.delay = delay;
        }

        public long getDelay() {

            return delay;
        }
    }
}
