package us.arrowcraft.aurora.city.engine;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import us.arrowcraft.aurora.admin.AdminComponent;
import us.arrowcraft.aurora.admin.AdminState;
import us.arrowcraft.aurora.events.ApocalypseBedSpawnEvent;
import us.arrowcraft.aurora.events.ApocalypseLocalSpawnEvent;
import us.arrowcraft.aurora.events.PlayerAdminModeChangeEvent;
import us.arrowcraft.aurora.homes.EnderPearlHomesComponent;
import us.arrowcraft.aurora.jail.JailComponent;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.EffectUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;
import us.arrowcraft.aurora.util.ItemUtil;
import us.arrowcraft.aurora.util.LocationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("multiplier")
        public int multiplier = 3;
        @Setting("max-mobs")
        public int maxMobs = 1000;
        @Setting("armour-chance")
        public int armourChance = 100;
        @Setting("weapon-chance")
        public int weaponChance = 100;
        @Setting("mob")
        public String cityAttackMobString = "zombie";
        @Setting("enable-safe-respawn-location")
        public boolean enableSafeRespawn = true;
        @Setting("safe-respawn-radius")
        public int safeRespawnRadius = 10;
        @Setting("enable-ultimate-chaos")
        public boolean enableUltimateChaos = true;

        public EntityType attackMob = getAttackMob();

        private EntityType getAttackMob() {

            try {
                return EntityType.fromName(cityAttackMobString.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitySpawn(CreatureSpawnEvent event) {

        if (config.enableUltimateChaos
                && event.getLocation().getWorld().isThundering()
                && event.getEntity().getType().equals(config.attackMob)
                && !event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)
                && event.isCancelled())
            event.setCancelled(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {

        Entity target = event.getTarget();
        Entity targeter = event.getEntity();

        if (!(target instanceof Player) || !targeter.isValid() || !targeter.getType().equals(config.attackMob)) return;

        Player player = (Player) target;
        if (player.getWorld().isThundering() && ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(8)) {

            targeter.setFireTicks(ChanceUtil.getRandom(20 * 60));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {

        Entity target = event.getEntity();

        if (target == null || !(target instanceof LivingEntity) || target.getType() == null) return;
        if (!target.getWorld().isThundering()) return;

        Player player;
        switch (target.getType()) {
            case PLAYER:
                player = (Player) target;
                if (ItemUtil.hasAncientArmour(player)) {
                    if (ChanceUtil.getChance(7)) {

                        EffectUtil.Ancient.powerBurst(player, event.getDamage());
                    }
                }
                break;
            default:
                Entity attacker = event.getDamager();
                LivingEntity defender = (LivingEntity) target;
                if (attacker instanceof Player) {
                    player = (Player) attacker;
                    if (ItemUtil.hasMasterSword(player)) {

                        if (ChanceUtil.getChance(10)) {
                            EffectUtil.Master.healingBlade(player, defender);
                        }

                        if (ChanceUtil.getChance(18)) {
                            List<LivingEntity> entities = new ArrayList<>();
                            for (Entity e : player.getNearbyEntities(6, 4, 6)) {

                                if (EnvironmentUtil.isHostileEntity(e)) entities.add((LivingEntity) e);
                            }
                            EffectUtil.Master.doomBlade(player, entities);
                        }
                    }
                }
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
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

    @EventHandler
    public void onPlayerDeath(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        if (player.getWorld().isThundering() && config.enableSafeRespawn) {
            int safeRespawnRadius = config.safeRespawnRadius * config.safeRespawnRadius;

            // Ensure the radius is at least 2
            if (safeRespawnRadius < 4) safeRespawnRadius = 4;

            try {
                Location respawnLoc = event.getRespawnLocation();
                for (Entity attackMob : respawnLoc.getWorld().getEntitiesByClass(config.attackMob.getEntityClass())) {

                    if (attackMob.getLocation().distanceSquared(respawnLoc) < safeRespawnRadius) attackMob.remove();
                }
            } catch (Exception e) {
                log.warning("One or more " + config.cityAttackMobString + " could not be removed from the player: "
                        + player.getName() + "'s respawn location.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        Entity ent = event.getEntity();
        World world = ent.getWorld();

        EntityType entType = config.attackMob;

        if (ent instanceof Skeleton && ((Skeleton) ent).getKiller() != null) {
            if (world.isThundering() && ChanceUtil.getChance(5)) {
                event.getDrops().add(new ItemStack(ItemID.ARROW, (ChanceUtil.getRandom(8) * 2)));
            } else {
                event.getDrops().add(new ItemStack(ItemID.ARROW, (ChanceUtil.getRandom(8))));
            }
        }

        if (world.isThundering()) {
            if (ent.getType().equals(entType) && ChanceUtil.getChance(5)) {
                event.setDroppedExp(event.getDroppedExp() * 3);
                event.getDrops().add(new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRandom(8)));
            } else event.setDroppedExp(event.getDroppedExp() * 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdminChange(PlayerAdminModeChangeEvent event) {

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
        try {
            mobCount = world.getEntitiesByClasses(config.attackMob.getEntityClass()).size();
        } catch (Exception e) {
            log.warning("Misconfigurated configuration, cannot count mob: " + config.cityAttackMobString + ".");
            return;
        }

        // Lets not flood the world farther
        if (mobCount >= mobCountMax || world.getEntities().size() > (mobCountMax * 2)) return;

        // Do we care?
        if (world.isThundering()) {
            //Attack info
            List<Player> applicablePlayers = new ArrayList<>();
            int multiplier = config.multiplier;

            // Change Multiplier
            if (ChanceUtil.getChance(7)) {
                multiplier = (config.multiplier * ChanceUtil.getRandom(6));
            }

            for (Player player : server.getOnlinePlayers()) {

                if (player.getWorld() != world || jailComponent.isJailed(player) || adminComponent.isAdmin(player)) {
                    continue;
                }
                applicablePlayers.add(player);
            }

            try {
                // Lightning, Spawn, and Beds
                for (int i = 0; i < (multiplier * applicablePlayers.size()); i++) {

                    spawnAndArm(LocationUtil.findFreePosition(lightningStrikeLoc), config.attackMob, true);
                    spawnAndArm(LocationUtil.findFreePosition(world.getSpawnLocation()), config.attackMob, true);

                    if (ChanceUtil.getChance((multiplier / 2) * applicablePlayers.size())) {
                        for (Player player : applicablePlayers) {

                            if (inst.hasPermission(player, "aurora.apocalypse.bedsafe")) continue;
                            Location bedLocation = homesComponent.getBedLocation(player);
                            if (bedLocation == null) continue;
                            ApocalypseBedSpawnEvent apocalypseEvent = new ApocalypseBedSpawnEvent(player,
                                    LocationUtil.findFreePosition(bedLocation));
                            server.getPluginManager().callEvent(apocalypseEvent);
                            if (!apocalypseEvent.isCancelled()
                                    && apocalypseEvent.getLocation().getWorld().equals(world)) {

                                spawnAndArm(apocalypseEvent.getLocation(), config.attackMob, true);
                            }
                        }
                    }
                }

                // Local Spawn
                for (Player player : applicablePlayers) {
                    if (inst.hasPermission(player, "aurora.apocalypse.huntsafe") || ChanceUtil.getChance(2)) continue;
                    try {
                        Block playerBlock = player.getLocation().getBlock();
                        Location ls;

                        if (playerBlock.getLightLevel() < 4) {
                            for (int i = 0; i < ChanceUtil.getRandom(7); i++) {
                                ls = LocationUtil.findFreePosition(LocationUtil.findRandomLoc(playerBlock, 8, true));
                                ApocalypseLocalSpawnEvent apocalypseEvent = new ApocalypseLocalSpawnEvent(player, ls);
                                server.getPluginManager().callEvent(apocalypseEvent);

                                if (!apocalypseEvent.isCancelled()) {
                                    spawnAndArm(apocalypseEvent.getLocation(), config.attackMob, false);
                                }
                            }
                        } else {
                            ls = LocationUtil.findFreePosition(LocationUtil.findRandomLoc(playerBlock, 8, true));
                            ApocalypseLocalSpawnEvent apocalypseEvent = new ApocalypseLocalSpawnEvent(player, ls);
                            server.getPluginManager().callEvent(apocalypseEvent);

                            if (!apocalypseEvent.isCancelled()) {
                                spawn(apocalypseEvent.getLocation(), config.attackMob);
                            }
                        }
                    } catch (Exception e) {
                        log.warning("Could not find a location around the player: "
                                + player.getName() + " to spawn a mob called: "
                                + config.cityAttackMobString + ".");
                    }
                }
            } catch (Exception e) {
                log.warning("Could not spawn all: "
                        + config.cityAttackMobString + " in world: " + world.getName());
            }
        }
    }

    private void spawnAndArm(Location location, EntityType type, boolean allowItemPickup) {

        Entity e = spawn(location, type);
        if (e == null) return;
        if (e instanceof Zombie && ChanceUtil.getChance(16)) {
            ((Zombie) e).setBaby(true);
        }
        // Disabled until there is a better way to do it
        arm(e, false);
    }

    private Entity spawn(Location location, EntityType type) {

        return location != null ? location.getWorld().spawnEntity(location, type) : null;
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
