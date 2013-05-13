package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    private boolean canFearSpec(String name) {

        return !fearSpec.containsKey(name) || System.currentTimeMillis() - fearSpec.get(name) >= 3800;
    }

    private boolean canUnleashedSpec(String name) {

        return !unleashedSpec.containsKey(name) || System.currentTimeMillis() - unleashedSpec.get(name) >= 3800;
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

            if (canFearSpec(owner.getName())) {
                if (ItemUtil.hasFearSword(owner)) {
                    fearSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (ChanceUtil.getRandom(5)) {
                        case 1:
                            EffectUtil.Fear.confuse(owner, target);
                            break;
                        case 2:
                            EffectUtil.Fear.fearBlaze(owner, target);
                            break;
                        case 3:
                            EffectUtil.Fear.poison(owner, target);
                            break;
                        case 4:
                            EffectUtil.Fear.weaken(owner, target);
                            break;
                        case 5:
                            EffectUtil.Fear.wrath(owner, target, event.getDamage(), ChanceUtil.getRangedRandom(2, 6));
                            break;
                    }
                } else if (ItemUtil.hasFearBow(owner)) {

                    fearSpec.put(owner.getName(), System.currentTimeMillis());
                    int attack = ChanceUtil.getRandom(4);

                    switch (attack) {
                        case 1:
                            if (EffectUtil.Fear.disarm(owner, target)) {
                                break;
                            }
                        case 2:
                            EffectUtil.Fear.poison(owner, target);
                            break;
                        case 3:
                            EffectUtil.Fear.magicChain(owner, target);
                            break;
                        case 4:
                            event.setDamage(EffectUtil.Fear.fearStrike(owner, target, event.getDamage()));
                            break;
                    }

                    if (attack != 4 && ChanceUtil.getChance(6)) {
                        Location targetLoc = target.getLocation();
                        if (!targetLoc.getWorld().isThundering() && targetLoc.getBlock().getLightFromSky() > 0) {
                            targetLoc.getWorld().strikeLightning(targetLoc);
                        }
                    }
                }
            }

            if (canUnleashedSpec(owner.getName())) {
                if (ItemUtil.hasUnleashedSword(owner)) {
                    unleashedSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (ChanceUtil.getRandom(6)) {
                        case 1:
                            EffectUtil.Unleashed.blind(owner, target);
                            break;
                        case 2:
                            EffectUtil.Unleashed.healingLight(owner, target);
                            break;
                        case 3:
                            EffectUtil.Unleashed.speed(owner, target);
                            break;
                        case 4:
                            EffectUtil.Unleashed.regen(owner, target);
                            break;
                        case 5:
                            EffectUtil.Unleashed.doomBlade(owner, target);
                            break;
                        case 6:
                            EffectUtil.Unleashed.lifeLeech(owner, target);
                            break;
                    }
                } else if (ItemUtil.hasUnleashedBow(owner)) {
                    unleashedSpec.put(owner.getName(), System.currentTimeMillis());
                    ChatUtil.sendError(owner, "This weapon is currently a WIP.");

                }
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
    }
}
