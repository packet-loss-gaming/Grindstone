package com.skelril.aurora.pet;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Pet", desc = "Pets!")
public class PetComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private HashMap<String, Pet> namePet = new HashMap<>();

    @Override
    public void enable() {

        //inst.registerEvents(this);
        //server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20 * 2);

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        EntityType entityType = null;

        if (inst.hasPermission(player, "aurora.pet.blaze")) {
            entityType = EntityType.BLAZE;
        } else if (inst.hasPermission(player, "aurora.pet.skeleton")) {
            entityType = EntityType.SKELETON;
        } else if (inst.hasPermission(player, "aurora.pet.cavespider")) {
            entityType = EntityType.CAVE_SPIDER;
        } else if (inst.hasPermission(player, "aurora.pet.irongolem")) {
            entityType = EntityType.IRON_GOLEM;
        }

        if (entityType != null) {
            Entity e = player.getWorld().spawnEntity(player.getLocation(), entityType);
            Pet p = new Pet(player.getName(), e.getType(), (LivingEntity) e);
            namePet.put(player.getName(), p);
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (namePet.containsKey(player.getName())) {
            namePet.get(player.getName()).getPet().remove();
            namePet.remove(player.getName());
        }
    }

    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

        for (Map.Entry<String, Pet> entry : namePet.entrySet()) {
            try {
                Player player = Bukkit.getPlayerExact(entry.getKey());

                if (!event.getEntity().equals(player)) continue;

                Entity target;
                if (event.getDamager() instanceof Arrow) {
                    target = ((Arrow) event.getDamager()).getShooter();
                } else {
                    target = event.getDamager();
                }

                if (target != null && target instanceof LivingEntity) entry.getValue().setTarget((LivingEntity) target);
            } catch (Exception ignored) {

            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        for (Pet pet : namePet.values()) {
            if (pet.getPet() != null && pet.getPet().equals(event.getEntity())) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {

        for (Pet petEntry : namePet.values()) {

            Entity entity = event.getEntity();
            LivingEntity pet = petEntry.getPet();

            if (pet != null && pet.equals(entity)) {

                LivingEntity target = petEntry.getTarget();

                if (target != null
                        && !target.isDead() && target.isValid()
                        && target.getWorld() != null
                        && pet.getWorld() != null
                        && target.getWorld().equals(pet.getWorld())
                        && pet.getLocation().distanceSquared(target.getLocation()) < (22 * 22)
                        && target.getType().isAlive()
                        && !(target instanceof Player)
                        && !event.getTarget().equals(target)) {
                    event.setTarget(target);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void run() {

        for (Map.Entry<String, Pet> entry : namePet.entrySet()) {
            try {
                Player player = Bukkit.getPlayerExact(entry.getKey());
                Pet petEntry = entry.getValue();

                if (player != null && petEntry != null) {

                    LivingEntity pet = petEntry.getPet();

                    if (pet == null || pet.isDead() || !pet.isValid()) {

                        Entity newPet = player.getWorld().spawnEntity(player.getLocation(), petEntry.getType());
                        petEntry.setPet((LivingEntity) newPet);
                        continue;
                    }

                    if (!pet.getWorld().equals(player.getWorld())) {
                        pet.remove();
                        Entity newPet = player.getWorld().spawnEntity(player.getLocation(), petEntry.getType());
                        petEntry.setPet((LivingEntity) newPet);
                    } else if (pet.getLocation().distanceSquared(player.getLocation()) > (14 * 14)) {
                        pet.teleport(LocationUtil.findRandomLoc(player.getLocation(), 7, true));
                    } else if (pet.getLocation().distanceSquared(player.getLocation()) < (4 * 4)) {
                        pet.teleport(LocationUtil.findRandomLoc(player.getLocation(), 7, true));
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
