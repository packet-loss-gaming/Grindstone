/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.admin.AdminState;
import com.skelril.aurora.bosses.ThunderZombie;
import com.skelril.aurora.events.PlayerAdminModeChangeEvent;
import com.skelril.aurora.events.apocalypse.ApocalypseBedSpawnEvent;
import com.skelril.aurora.events.apocalypse.ApocalypseLocalSpawnEvent;
import com.skelril.aurora.homes.EnderPearlHomesComponent;
import com.skelril.aurora.jail.JailComponent;
import com.skelril.aurora.util.*;
import com.skelril.aurora.util.checker.Checker;
import com.skelril.aurora.util.extractor.entity.CombatantPair;
import com.skelril.aurora.util.extractor.entity.EDBEExtractor;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.item.custom.CustomItems;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Apocalypse", desc = "Sends an invasion force after the residents of the server.")
@Depend(components = {JailComponent.class, AdminComponent.class, EnderPearlHomesComponent.class})
public class ApocalypseComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private JailComponent jailComponent;
    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private EnderPearlHomesComponent homesComponent;

    private static final Class<Zombie> attackMob = Zombie.class;

    private LocalConfiguration config;
    private ThunderZombie bossManager;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        bossManager = new ThunderZombie();
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("boss-chance")
        public int bossChance = 100;
        @Setting("multiplier")
        public int multiplier = 6;
        @Setting("local-de-multiplier")
        public int deMultiplier = 4;
        @Setting("max-mobs")
        public int maxMobs = 1000;
        @Setting("armour-chance")
        public int armourChance = 100;
        @Setting("weapon-chance")
        public int weaponChance = 100;
        @Setting("enable-safe-respawn-location")
        public boolean enableSafeRespawn = true;
        @Setting("safe-respawn-radius")
        public int safeRespawnRadius = 10;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {

        Entity target = event.getTarget();
        Entity targeter = event.getEntity();

        if (!(target instanceof Player) || !targeter.isValid() || !attackMob.isInstance(targeter)) return;

        Player player = (Player) target;
        if (checkEntity((LivingEntity) targeter) && ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(8)) {
            targeter.setFireTicks(ChanceUtil.getRandom(20 * 60));
        }
    }

    private static EDBEExtractor<LivingEntity, LivingEntity, Projectile> extractor = new EDBEExtractor<>(
            LivingEntity.class,
            LivingEntity.class,
            Projectile.class
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {

        CombatantPair<LivingEntity, LivingEntity, Projectile> result = extractor.extractFrom(event);

        if (result == null || result.hasProjectile()) return;

        LivingEntity target = result.getDefender();
        LivingEntity attacker = result.getAttacker();

        Player player;
        switch (target.getType()) {
            case PLAYER:
                player = (Player) target;
                if (ItemUtil.hasAncientArmour(player) && checkEntity(attacker)) {
                    double diff = player.getMaxHealth() - player.getHealth();
                    if (ChanceUtil.getChance((int) Math.max(3, Math.round(player.getMaxHealth() - diff)))) {
                        EffectUtil.Ancient.powerBurst(player, event.getDamage());
                    }
                }
                break;
            default:
                if (attacker instanceof Player) {
                    player = (Player) attacker;
                    if (ItemUtil.isHoldingItem(player, CustomItems.MASTER_SWORD) && checkEntity(target)) {

                        if (ChanceUtil.getChance(10)) {
                            EffectUtil.Master.healingLight(player, target);
                        }

                        if (ChanceUtil.getChance(18)) {
                            Set<LivingEntity> entities = player.getNearbyEntities(6, 4, 6).stream().filter(EnvironmentUtil::isHostileEntity).map(e -> (LivingEntity) e).collect(Collectors.toSet());
                            EffectUtil.Master.doomBlade(player, entities);
                        }
                    }
                }
                break;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerRespawnEvent event) {
        if (config.enableSafeRespawn) {
            int safeRespawnRadius = config.safeRespawnRadius * config.safeRespawnRadius;

            // Ensure the radius is at least 2
            if (safeRespawnRadius < 4) safeRespawnRadius = 4;

            Location respawnLoc = event.getRespawnLocation();
            for (Entity entity : respawnLoc.getWorld().getEntitiesByClass(attackMob)) {

                if (!(entity instanceof LivingEntity) || !checkEntity((LivingEntity) entity)) continue;
                if (entity.getLocation().distanceSquared(respawnLoc) < safeRespawnRadius) entity.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        Entity ent = event.getEntity();
        World world = ent.getWorld();

        if (ent instanceof Skeleton && ((Skeleton) ent).getKiller() != null) {
            ItemStack held = ((Skeleton) ent).getEquipment().getItemInHand();
            if (held != null && held.getTypeId() == ItemID.BOW) {
                if (world.isThundering() && ChanceUtil.getChance(5)) {
                    event.getDrops().add(new ItemStack(ItemID.ARROW, (ChanceUtil.getRandom(8) * 2)));
                } else {
                    event.getDrops().add(new ItemStack(ItemID.ARROW, (ChanceUtil.getRandom(8))));

                }
            }
        }

        if (checkEntity((LivingEntity) ent)) {

            Iterator<ItemStack> dropIterator = event.getDrops().iterator();
            while (dropIterator.hasNext()) {
                ItemStack next = dropIterator.next();
                if (next != null && next.getTypeId() == ItemID.ROTTEN_FLESH) dropIterator.remove();
            }

            if (attackMob.isInstance(ent) && ChanceUtil.getChance(5)) {
                event.setDroppedExp(event.getDroppedExp() * 3);
                event.getDrops().add(new ItemStack(ItemID.GOLD_NUGGET, ChanceUtil.getRandom(8)));
            } else event.setDroppedExp(event.getDroppedExp() * 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdminChange(PlayerAdminModeChangeEvent event) {
        if (event.getNewAdminState().equals(AdminState.SYSOP)) return;
        if (!event.getNewAdminState().equals(AdminState.MEMBER) && event.getPlayer().getWorld().isThundering()) {
            event.setCancelled(true);
        }
    }

    // Thunderstorm Attack
    @EventHandler(ignoreCancelled = true)
    public void onLightningStrikeEvent(LightningStrikeEvent event) {

        LightningStrike lightning = event.getLightning();
        World world = lightning.getWorld();
        final int mobCount;
        final int mobCountMax = config.maxMobs;
        Location lightningStrikeLoc = lightning.getLocation();

        // Get the mob count
        mobCount = world.getEntitiesByClasses(attackMob).size();

        // Lets not flood the world farther
        if (mobCount >= mobCountMax || world.getEntities().size() > (mobCountMax * 2)) return;

        // Do we care?
        if (world.isThundering()) {
            lightning(lightningStrikeLoc);
        }
    }

    public void lightning(Location location) {

        Location strikeLoc = LocationUtil.findFreePosition(location);

        if (strikeLoc == null) return;

        List<Player> applicable = getApplicable(location.getWorld());
        spawnAndArm(strikeLoc, attackMob, true);

        startle(applicable);
        bedSpawn(CollectionUtil.removalAll(applicable, new Checker<CommandBook, Player>(inst) {
            @Override
            public Boolean evaluate(Player player) {
                return get().hasPermission(player, "aurora.apocalypse.bedsafe");
            }
        }), config.multiplier * (ChanceUtil.getRandom(6)));
        localSpawn(CollectionUtil.removalAll(applicable, new Checker<CommandBook, Player>(inst) {
            @Override
            public Boolean evaluate(Player player) {
                return get().hasPermission(player, "aurora.apocalypse.huntsafe");
            }
        }));
    }

    public List<Player> getApplicable(World world) {
        List<Player> applicablePlayers = new ArrayList<>();
        for (Player player : server.getOnlinePlayers()) {
            if (!player.getWorld().equals(world) || jailComponent.isJailed(player) || adminComponent.isAdmin(player)) {
                continue;
            }
            applicablePlayers.add(player);
        }
        return applicablePlayers;
    }

    private void startle(List<Player> applicable) {
        applicable.stream().filter(Player::isFlying).forEach(player -> {
            player.setFlying(false);
            player.setAllowFlight(false);
            ChatUtil.sendNotice(player, "The lightning hinders your ability to fly.");
        });
    }

    public void bedSpawn(List<Player> players, int multiplier) {

        for (int i = 0; i < players.size() * multiplier; i++) {
            if (ChanceUtil.getChance((multiplier / config.deMultiplier) * players.size())) {
                for (Player player : players) {
                    Location bedLocation = homesComponent.getBedLocation(player);
                    if (bedLocation == null) continue;

                    ApocalypseBedSpawnEvent apocalypseEvent = new ApocalypseBedSpawnEvent(player, LocationUtil.findFreePosition(bedLocation));
                    server.getPluginManager().callEvent(apocalypseEvent);

                    if (apocalypseEvent.isCancelled()) continue;

                    spawnAndArm(apocalypseEvent.getLocation(), attackMob, true);
                }
            }
        }
    }

    public void localSpawn(List<Player> players) {
        for (Player player : players) {
            if (ChanceUtil.getChance(2)) continue;
            Block playerBlock = player.getLocation().getBlock();

            for (int i = ChanceUtil.getRandom(16 - playerBlock.getLightLevel()); i > 0; --i) {

                Location l = findLocation(player.getLocation());

                ApocalypseLocalSpawnEvent apocalypseEvent = new ApocalypseLocalSpawnEvent(player, l);
                server.getPluginManager().callEvent(apocalypseEvent);

                if (apocalypseEvent.isCancelled()) continue;

                spawnAndArm(apocalypseEvent.getLocation(), attackMob, false);
            }
        }
    }

    public Location findLocation(Location origin) {
        Location l = LocationUtil.findRandomLoc(origin, 8, true, false);
        return BlockType.isTranslucent(l.getBlock().getTypeId()) ? l : origin;
    }

    public boolean checkEntity(LivingEntity e) {

        return e.getWorld().isThundering()
                || (e.getCustomName() != null && e.getCustomName().equals("Apocalyptic Zombie"));
    }

    private <T extends LivingEntity> void spawnAndArm(Location location, Class<T> clazz, boolean allowItemPickup) {

        if (!location.getChunk().isLoaded()) return;

        Entity e = spawn(location, clazz);
        if (e == null) return;
        if (e instanceof Zombie && ChanceUtil.getChance(16)) {
            ((Zombie) e).setBaby(true);
        }
        // Disabled until there is a better way to do it
        arm(e, false);
    }

    private <T extends LivingEntity> T spawn(Location location, Class<T> clazz) {
        if (location == null) return null;
        T entity = location.getWorld().spawn(location, clazz);
        entity.setCustomName("Apocalyptic Zombie");
        entity.setCustomNameVisible(false);
        if (ChanceUtil.getChance(config.bossChance)) {
            bossManager.bind(entity);
        }
        return entity;
    }

    private void arm(Entity e, boolean allowItemPickup) {

        if (!(e instanceof LivingEntity)) return;

        EntityEquipment equipment = ((LivingEntity) e).getEquipment();
        ((LivingEntity) e).setCanPickupItems(allowItemPickup);

        if (ChanceUtil.getChance(config.armourChance)) {
            if (ChanceUtil.getChance(35)) {
                equipment.setArmorContents(ItemUtil.diamondArmour);
            } else {
                equipment.setArmorContents(ItemUtil.ironArmour);
            }

            if (ChanceUtil.getChance(4)) equipment.setHelmet(null);
            if (ChanceUtil.getChance(4)) equipment.setChestplate(null);
            if (ChanceUtil.getChance(4)) equipment.setLeggings(null);
            if (ChanceUtil.getChance(4)) equipment.setBoots(null);
        }

        if (ChanceUtil.getChance(config.weaponChance)) {
            ItemStack sword = new ItemStack(ItemID.IRON_SWORD);
            if (ChanceUtil.getChance(35)) sword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ALL, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_UNDEAD, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.FIRE_ASPECT, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.KNOCKBACK, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, ChanceUtil.getRandom(3), false);
            sword.setItemMeta(meta);
            equipment.setItemInHand(sword);
        }

        if (allowItemPickup) {
            equipment.setItemInHandDropChance(1);
            equipment.setHelmetDropChance(1);
            equipment.setChestplateDropChance(1);
            equipment.setLeggingsDropChance(1);
            equipment.setBootsDropChance(1);
        } else {
            equipment.setItemInHandDropChance(.55F);
            equipment.setHelmetDropChance(.55F);
            equipment.setChestplateDropChance(.55F);
            equipment.setLeggingsDropChance(.55F);
            equipment.setBootsDropChance(.55F);
        }
    }
}
