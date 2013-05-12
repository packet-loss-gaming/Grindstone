package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EffectUtil;
import com.skelril.aurora.util.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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

    private boolean canSpeced(String name) {

        if (fearSpec.containsKey(name)) {

            return System.currentTimeMillis() - fearSpec.get(name) >= 3800;
        }
        return true;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {

        Player owner = event.getDamager() instanceof Player ? (Player) event.getDamager() : null;
        LivingEntity target = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;

        if (owner != null && target != null && ItemUtil.hasFearSword(owner) && canSpeced(owner.getName())) {

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
                    EffectUtil.Fear.wrath(owner, target, event.getDamage(), 4);
                    break;
            }
            fearSpec.put(owner.getName(), System.currentTimeMillis());
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity damaged = event.getEntity();
        Player player = damaged.getKiller();

        if (player != null) {

            World w = player.getWorld();
            Location pLocation = player.getLocation();

            if (ItemUtil.hasMasterBow(player) && !(damaged instanceof Player) && event.getDrops().size() > 0) {

                for (ItemStack is : event.getDrops()) {
                    if (is != null) w.dropItemNaturally(pLocation, is);
                }
                event.getDrops().clear();
                ChatUtil.sendNotice(player, "The Master Bow releases a bright flash.");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        String name = event.getPlayer().getName();
        if (fearSpec.containsKey(name)) fearSpec.remove(name);
    }
}
