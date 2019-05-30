/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.Frostborn;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BaseBlock;
import gg.packetloss.grindstone.city.engine.area.AreaListener;
import gg.packetloss.grindstone.city.engine.combat.PvMComponent;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLocalSpawnEvent;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.restoration.BlockRecord;
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

    public FrostbornListener(FrostbornArea parent) {
        super(parent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        if (!event.getHymn().equals(HymnSingEvent.Hymn.PHANTOM)) return;
        Player player = event.getPlayer();
        if (parent.contains(parent.gate_RG, player)) {
            // Teleport to gate area
            player.teleport(new Location(parent.getWorld(), -50.5, 81, 392, 90, 0), TeleportCause.UNKNOWN);
        } else if (parent.contains(player, 1)) {
            // Teleport back outside
            player.teleport(parent.gate, TeleportCause.UNKNOWN);
        }
    }

    @EventHandler
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
            event.getPlayer().getInventory().addItem(new ItemStack(Material.SNOW_BALL, 4));
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
            event.setTo(parent.gate);
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

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {
        Projectile p = event.getEntity();
        if (p instanceof Snowball && parent.contains(p)) {
            if (p.hasMetadata("forstborn-avalanche")) {
                Location targetLoc = p.getLocation();
                targetLoc.setY(79);
                parent.createAvalanche(targetLoc);
            }

            float damage = 1;
            boolean blockDamage = true;

            if (p.getShooter() instanceof Player) {
                damage = 2;
                blockDamage = false;
            }

            Location targetLoc = p.getLocation();
            p.getWorld().createExplosion(targetLoc.getX(), targetLoc.getY(), targetLoc.getZ(), damage, false, blockDamage);
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
        Block block = event.getBlock();
        BaseBlock baseBlock = new BaseBlock(block.getTypeId(), block.getData());
        if (parent.contains(block) && !parent.admin.isAdmin(event.getPlayer())) {
            if (!parent.accept(baseBlock, FrostbornArea.breakable)) {
                event.setCancelled(true);
                return;
            }

            parent.generalIndex.addItem(new BlockRecord(block));
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

            BaseBlock baseBlock = new BaseBlock(block.getTypeId(), block.getData());
            if (!parent.accept(baseBlock, FrostbornArea.breakable)) {
                it.remove();
                continue;
            }

            // Prevent glowstone lighting the sides of the arena from being busted
            if (block.getY() >= 78) {
                it.remove();
                continue;
            }

            // Add restoreable blocks to the restoration system
            if (!parent.accept(baseBlock, FrostbornArea.restoreable)) {
                continue;
            }
            parent.generalIndex.addItem(new BlockRecord(block));
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

    @EventHandler
    public void onApocalypseLocalSpawnEvent(ApocalypseLocalSpawnEvent event) {
        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
