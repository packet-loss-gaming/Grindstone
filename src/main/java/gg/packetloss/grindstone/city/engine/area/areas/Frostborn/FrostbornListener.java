/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.Frostborn;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.city.engine.area.AreaListener;
import gg.packetloss.grindstone.city.engine.combat.PvMComponent;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLocalSpawnEvent;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.logging.Logger;

import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class FrostbornListener extends AreaListener<FrostbornArea> {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private int explosionCounter = 0;
    private double rollingAvgExplosionsPerSecond = 0;

    public FrostbornListener(FrostbornArea parent) {
        super(parent);

        server.getScheduler().runTaskTimer(inst, () -> {
            rollingAvgExplosionsPerSecond = (explosionCounter + rollingAvgExplosionsPerSecond) / 2;
            explosionCounter = 0;
        }, 0, 20);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (!ItemUtil.isItem(itemStack, CustomItems.ODE_TO_THE_FROZEN_KING)) {
            return;
        }

        if (parent.contains(parent.gate_RG, player)) {
            // Teleport inside just past the gate
            player.teleport(parent.gateInner, TeleportCause.UNKNOWN);
        } else if (parent.contains(player, 1)) {
            // Teleport back outside
            player.teleport(parent.gateOuter, TeleportCause.UNKNOWN);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (!parent.contains(clickedBlock)) {
            return;
        }

        Material blockType = clickedBlock.getType();
        if (blockType == Material.SNOW || blockType == Material.SNOW_BLOCK) {
            event.getPlayer().getInventory().addItem(new ItemStack(Material.SNOWBALL, 4));
        }
    }

    @EventHandler
    public void onFallBlock(FallBlockerEvent event) {
        Player player = event.getPlayer();

        if (parent.contains(parent.entrance_RG, player)) {
            parent.movePlayer(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();

        if (parent.contains(to, 1) && !event.getCause().equals(TeleportCause.UNKNOWN)) {
            Player player = event.getPlayer();
            if (parent.admin.isAdmin(player)) return;
            event.setTo(parent.gateOuter);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (parent.contains(player)) {
            player.teleport(parent.gateInner, TeleportCause.UNKNOWN);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!parent.contains(entity)) return;

        if (entity instanceof Snowman) {
            // Increase the probability of a special attack
            ++parent.rageModifier;

            // Slow the boss on damage
            PotionEffect potionEffect = new PotionEffect(PotionEffectType.SLOW, 20 * 3, 3, true, false);
            ((Snowman) entity).addPotionEffect(potionEffect, true);

            // Notify players of the new health
            for (Player player : parent.getContained(1, Player.class)) {
                PvMComponent.printHealth(player, (LivingEntity) entity);
            }

            // If punched return fire with a special attack
            if (event instanceof EntityDamageByEntityEvent) {
                if (((EntityDamageByEntityEvent) event).getDamager() instanceof Player) {
                    parent.runSpecial(3);
                }
            }
        } else if (entity instanceof Item) {
            event.setCancelled(true);
        }
    }

    private int getChanceForIteration(int iteration) {
        if (iteration == 1) {
            return 1;
        }

        if (iteration < 5) {
            return 3;
        }

        return (int) Math.pow(3, iteration - 1);
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {
        Projectile p = event.getEntity();
        if (p instanceof Snowball && parent.contains(p)) {
            if (!parent.isBossSpawned()) {
                return;
            }

            if (p.hasMetadata("forstborn-avalanche")) {
                Location targetLoc = p.getLocation();
                targetLoc.setY(79);

                int iteration = p.getMetadata("forstborn-avalanche").get(0).asInt();
                if (ChanceUtil.getChance(getChanceForIteration(iteration))) {
                    parent.createAvalanche(targetLoc, iteration + 1);
                }
            }

            float damage = 1;
            boolean blockDamage = true;

            if (p.getShooter() instanceof Player) {
                damage = 2;
                blockDamage = false;
            } else {
                ++explosionCounter;

                if (rollingAvgExplosionsPerSecond > 300) {
                    // Drop some snowballs, no one is going to notice anyways
                    if (ChanceUtil.getChance((rollingAvgExplosionsPerSecond / 300) + 1)) {
                        return;
                    }
                }

                // Only do block damage
                if (rollingAvgExplosionsPerSecond > 50) {
                    blockDamage = ChanceUtil.getChance(((int) rollingAvgExplosionsPerSecond / 50) + 1);
                }
            }

            Location targetLoc = p.getLocation();
            ExplosionStateFactory.createExplosion(targetLoc, damage, false, blockDamage);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (parent.contains(event.getBlock()) && !parent.admin.isAdmin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (parent.contains(block) && !parent.admin.isAdmin(player)) {
            if (!FrostbornArea.BREAKABLE.contains(block.getType())) {
                event.setCancelled(true);
                return;
            }

            try {
                parent.blockState.pushBlock(BlockStateKind.FROSTBORN, player, block.getState());
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();

                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!parent.contains(event.getBlock())) {
            return;
        }

        event.setYield(0);

        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();

            Material blockType = block.getType();
            if (!FrostbornArea.BREAKABLE.contains(blockType)) {
                it.remove();
                continue;
            }

            // Prevent glowstone lighting the sides of the arena from being busted
            if (block.getY() > FrostbornArea.ARENA_FLOOR_LEVEL + 1) {
                it.remove();
                continue;
            }

            // Add restoreable blocks to the restoration system
            if (!FrostbornArea.RESTOREABLE.contains(blockType)) {
                continue;
            }

            try {
                parent.blockState.pushAnonymousBlock(BlockStateKind.FROSTBORN, block.getState());
            } catch (UnstorableBlockStateException e) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (parent.contains(event.getEntity())) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            LivingEntity e = event.getEntity();
            if (e.equals(parent.boss)) {
                parent.boss = null;
                parent.lastDeath = System.currentTimeMillis();

                parent.dropLoot();
                parent.freezeEntrance();
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (parent.contains(player) && !parent.admin.isAdmin(player)) {
            if (parent.contains(player) && parent.isBossSpawned()) {
                EntityUtil.heal(parent.boss, parent.boss.getMaxHealth() / 3);
            }

            String deathMessage;
            switch (ChanceUtil.getRandom(2)) {
                case 1:
                    deathMessage = " exploded from frosty joy";
                    break;
                default:
                    deathMessage = " lost a snowball fight";
                    break;
            }
            event.setDeathMessage(player.getName() + deathMessage);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseLocalSpawnEvent(ApocalypseLocalSpawnEvent event) {
        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
