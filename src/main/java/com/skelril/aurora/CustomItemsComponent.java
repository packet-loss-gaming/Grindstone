package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.events.entity.ProjectileTickEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.skelril.aurora.events.custom.item.SpecialAttackEvent.Specs;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Custom Items Component", desc = "Custom Items")
public class CustomItemsComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private ConcurrentHashMap<String, Long> fearSpec = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> unleashedSpec = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> batSpec = new ConcurrentHashMap<>();

    private boolean canFearSpec(String name) {

        return !fearSpec.containsKey(name) || System.currentTimeMillis() - fearSpec.get(name) >= 3800;
    }

    private boolean canUnleashedSpec(String name) {

        return !unleashedSpec.containsKey(name) || System.currentTimeMillis() - unleashedSpec.get(name) >= 3800;
    }

    private boolean canBatSpec(String name) {

        return !batSpec.containsKey(name) || System.currentTimeMillis() - batSpec.get(name) >= 15000;
    }

    private Specs callSpec(Player owner, Object target, Specs spec) {

        SpecialAttackEvent event;
        if (target instanceof LivingEntity) {
            event = new SpecialAttackEvent(owner, (LivingEntity) target, spec);
        } else if (target instanceof Location) {
            event = new SpecialAttackEvent(owner, (Location) target, spec);
        } else {
            return null;
        }

        server.getPluginManager().callEvent(event);

        return event.isCancelled() ? null : event.getSpec();
    }

    private Specs callSpec(Player owner, Object target, int start, int end) {

        Specs[] available;
        Specs used;
        SpecialAttackEvent event;

        available = Arrays.copyOfRange(Specs.values(), start, end);
        used = available[ChanceUtil.getRandom(available.length) - 1];

        if (target instanceof LivingEntity) {
            event = new SpecialAttackEvent(owner, (LivingEntity) target, used);
        } else if (target instanceof Location) {
            event = new SpecialAttackEvent(owner, (Location) target, used);
        } else {
            return null;
        }

        server.getPluginManager().callEvent(event);

        return event.isCancelled() ? null : event.getSpec();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {

        Entity damager = event.getDamager();
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() != null) {
            damager = ((Projectile) damager).getShooter();
        }

        Player owner = damager instanceof Player ? (Player) damager : null;
        LivingEntity target = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;

        if (owner != null && target != null) {

            Specs used;
            if (canFearSpec(owner.getName())) {
                if (ItemUtil.hasFearSword(owner)) {
                    used = callSpec(owner, target, 0, 5);
                    if (used == null) return;
                    fearSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (used) {
                        case CONFUSE:
                            EffectUtil.Fear.confuse(owner, target);
                            break;
                        case BLAZE:
                            EffectUtil.Fear.fearBlaze(owner, target);
                            break;
                        case CURSE:
                            EffectUtil.Fear.curse(owner, target);
                            break;
                        case WEAKEN:
                            EffectUtil.Fear.weaken(owner, target);
                            break;
                        case SOUL_SMITE:
                            EffectUtil.Fear.soulSmite(owner, target);
                            break;
                    }
                } else if (ItemUtil.hasFearBow(owner)) {
                    used = callSpec(owner, target, 5, 9);
                    if (used == null) return;
                    fearSpec.put(owner.getName(), System.currentTimeMillis());

                    switch (used) {
                        case DISARM:
                            if (EffectUtil.Fear.disarm(owner, target)) {
                                break;
                            }
                        case RANGE_CURSE:
                            EffectUtil.Fear.curse(owner, target);
                            break;
                        case MAGIC_CHAIN:
                            EffectUtil.Fear.magicChain(owner, target);
                            break;
                        case FEAR_STRIKE:
                            event.setDamage(EffectUtil.Fear.fearStrike(owner, target, event.getDamage()));
                            break;
                    }
                }
            }

            if (canUnleashedSpec(owner.getName())) {
                if (ItemUtil.hasUnleashedSword(owner)) {
                    used = callSpec(owner, target, 9, 15);
                    if (used == null) return;
                    unleashedSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (used) {
                        case BLIND:
                            EffectUtil.Unleashed.blind(owner, target);
                            break;
                        case HEALING_LIGHT:
                            EffectUtil.Unleashed.healingLight(owner, target);
                            break;
                        case SPEED:
                            EffectUtil.Unleashed.speed(owner, target);
                            break;
                        case REGEN:
                            EffectUtil.Unleashed.regen(owner, target);
                            break;
                        case DOOM_BLADE:
                            EffectUtil.Unleashed.doomBlade(owner, target);
                            break;
                        case LIFE_LEECH:
                            EffectUtil.Unleashed.lifeLeech(owner, target);
                            break;
                    }
                } else if (ItemUtil.hasUnleashedBow(owner)) {
                    //used = callSpec(owner, target, 16, 17);
                    //if (used == null) return;
                    unleashedSpec.put(owner.getName(), System.currentTimeMillis());
                    ChatUtil.sendError(owner, "This weapon is currently a WIP.");
                }
            }
        }
    }

    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {

        Entity shooter = event.getEntity().getShooter();

        if (shooter != null && shooter instanceof Player) {

            Player owner = (Player) shooter;
            Location targetLoc = event.getEntity().getLocation();

            if (canBatSpec(owner.getName())) {
                Specs used;
                if (ItemUtil.hasBatBow(owner)) {
                    used = callSpec(owner, targetLoc, Specs.BAT_ATTACK);
                    if (used == null) return;
                    batSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (used) {
                        case BAT_ATTACK:
                            EffectUtil.Strange.goneBatty(owner, targetLoc);
                            break;
                    }
                }
            }

            if (ItemUtil.hasFearBow(owner)) {

                if (!canFearSpec(owner.getName())) {
                    if (!targetLoc.getWorld().isThundering() && targetLoc.getBlock().getLightFromSky() > 0) {
                        targetLoc.getWorld().strikeLightning(targetLoc);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArrowTick(ProjectileTickEvent event) {

        Entity shooter = event.getEntity().getShooter();

        if (shooter != null && shooter instanceof Player) {

            if (ChanceUtil.getChance(5) && ItemUtil.hasBatBow((Player) shooter)) {

                final Location location = event.getEntity().getLocation();
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        final Bat bat = (Bat) location.getWorld().spawnEntity(location, EntityType.BAT);
                        server.getScheduler().runTaskLater(inst, new Runnable() {

                            @Override
                            public void run() {

                                if (bat.isValid()) {
                                    bat.remove();
                                    for (int i = 0; i < 20; i++) {
                                        bat.getWorld().playEffect(bat.getLocation(), Effect.SMOKE, 0);
                                    }
                                }
                            }
                        }, 20 * 3);
                    }
                }, 3);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onXPPickUp(PlayerExpChangeEvent event) {

        Player player = event.getPlayer();

        if (ItemUtil.hasAncientArmour(player)) {
            ItemStack[] armour = player.getInventory().getArmorContents();
            ItemStack is = armour[ChanceUtil.getRandom(armour.length) - 1];
            int exp = event.getAmount();
            if (exp > is.getDurability()) {
                exp -= is.getDurability();
                is.setDurability((short) 0);
            } else {
                is.setDurability((short) (is.getDurability() - exp));
                exp = 0;
            }
            player.getInventory().setArmorContents(armour);
            event.setAmount(exp);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onItemBreak(PlayerItemBreakEvent event) {

        ItemStack broken = event.getBrokenItem();
        ItemStack newItem = null;

        if (ItemUtil.isFearSword(broken) || ItemUtil.isUnleashedSword(broken)) {
            newItem = ItemUtil.Master.makeSword();
        } else if (ItemUtil.isFearBow(broken) || ItemUtil.isUnleashedBow(broken)) {
            newItem = ItemUtil.Master.makeBow();
        }

        if (newItem == null) return;

        final Player player = event.getPlayer();
        final ItemStack finalNewItem = newItem;
        server.getScheduler().runTaskLater(inst, new Runnable() {

            @Override
            public void run() {

                if (player.isDead()) {
                    player.getWorld().dropItem(player.getLocation(), finalNewItem);
                } else {
                    player.setItemInHand(finalNewItem);
                }
            }
        }, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity damaged = event.getEntity();
        Player player = damaged.getKiller();

        if (player != null) {

            World w = player.getWorld();
            Location pLocation = player.getLocation();

            List<ItemStack> drops = event.getDrops();

            if (drops.size() > 0) {
                if (ItemUtil.hasUnleashedBow(player) || ItemUtil.hasMasterBow(player) && !(damaged instanceof Player)) {

                    for (ItemStack is : ItemUtil.clone(drops.toArray(new ItemStack[drops.size()]))) {
                        if (is != null) w.dropItemNaturally(pLocation, is);
                    }
                    drops.clear();
                    ChatUtil.sendNotice(player, "Your Bow releases a bright flash.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        String name = event.getPlayer().getName();
        if (fearSpec.containsKey(name)) fearSpec.remove(name);
        if (unleashedSpec.containsKey(name)) unleashedSpec.remove(name);
        if (batSpec.containsKey(name)) batSpec.remove(name);
    }
}
