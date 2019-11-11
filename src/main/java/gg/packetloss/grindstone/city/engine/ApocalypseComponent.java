/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.bosses.ThunderZombie;
import gg.packetloss.grindstone.buff.Buff;
import gg.packetloss.grindstone.buff.BuffCategory;
import gg.packetloss.grindstone.buff.BuffComponent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseBedSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLightningStrikeSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseLocalSpawnEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypseRespawnBoostEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.WeaponFamily;
import gg.packetloss.grindstone.items.implementations.FearBowImpl;
import gg.packetloss.grindstone.items.implementations.FearSwordImpl;
import gg.packetloss.grindstone.items.implementations.UnleashedBowImpl;
import gg.packetloss.grindstone.items.implementations.UnleashedSwordImpl;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.jail.JailComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.warps.WarpsComponent;
import org.bukkit.*;
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
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.EnvironmentUtil.hasThunderstorm;


@ComponentInformation(friendlyName = "Apocalypse", desc = "Sends an invasion force after the residents of the server.")
@Depend(components = {BuffComponent.class, JailComponent.class, AdminComponent.class, WarpsComponent.class})
public class ApocalypseComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private BuffComponent buffComponent;
    @InjectComponent
    private JailComponent jailComponent;
    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private WarpsComponent warpsComponent;
    @InjectComponent
    private SessionComponent sessions;

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
        @Setting("baby-chance")
        public int babyChance = 16;
        @Setting("amplification-noise")
        public int amplificationNoise = 12;
        @Setting("amplification-descale")
        public int amplificationDescale = 3;
        @Setting("strike-multiplier")
        public int strikeMultiplier = 5;
        @Setting("bed-multiplier")
        public int bedMultiplier = 3;
        @Setting("local-multiplier")
        public int localMultiplier = 1;
        @Setting("local-spawn-chance")
        public int localSpawnChance = 3;
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
        @Setting("death-grace")
        public long deathGrace = 60000 * 5;
        @Setting("buff-chance")
        public int buffChance = 15;
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

    private SpecialAttack getOverlordAttack(Player player, LivingEntity target, SpecType specType) {
        switch (specType) {
            case MELEE:
                if (ChanceUtil.getChance(2)) {
                    return new FearSwordImpl().getSpecial(player, target);
                } else {
                    return new UnleashedSwordImpl().getSpecial(player, target);
                }
            case RANGED:
                if (ChanceUtil.getChance(2)) {
                    return new FearBowImpl().getSpecial(player, target);
                } else {
                    return new UnleashedBowImpl().getSpecial(player, target);
                }
        }

        throw new IllegalStateException();
    }

    private static final int INIT_OVERLORD_EXTRA = 40;

    private int calculateOverlordCooldown(Player player) {
        int overlordCooldown = INIT_OVERLORD_EXTRA;

        if (ItemUtil.isHoldingItemInFamily(player, WeaponFamily.MASTER)) {
            overlordCooldown /= 2;
        }

        overlordCooldown -= buffComponent.getBuffLevel(Buff.APOCALYPSE_OVERLORD, player).orElse(0) * 3;

        return overlordCooldown;
    }

    private void processOverlord(Player player, LivingEntity target, boolean hasProjectile) {
        int overlordCooldown = calculateOverlordCooldown(player);

        // If a master weapon has been used OR the overlord buff is active, enable overlord specs
        if (overlordCooldown != INIT_OVERLORD_EXTRA) {
            SpecialAttack spec;

            do {
                spec = getOverlordAttack(player, target, hasProjectile ? SpecType.RANGED : SpecType.MELEE);
            } while (spec == null);

            new SpecialAttackFactory(sessions).process(player, spec, SpecType.OVERLORD, (specEvent) -> {
                specEvent.setContextCooldown(specEvent.getContextCoolDown() + (1000 * overlordCooldown));
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {

        CombatantPair<LivingEntity, LivingEntity, Projectile> result = extractor.extractFrom(event);

        if (result == null) return;

        LivingEntity target = result.getDefender();
        LivingEntity attacker = result.getAttacker();

        switch (target.getType()) {
            case PLAYER: {
                Player player = (Player) target;

                if (checkEntity(attacker)) {
                    if (ItemUtil.hasAncientArmour(player)) {
                        double diff = player.getMaxHealth() - player.getHealth();
                        if (ChanceUtil.getChance((int) Math.max(3, Math.round(player.getMaxHealth() - diff)))) {
                            EffectUtil.Ancient.powerBurst(player, event.getDamage());
                        }
                    }

                    buffComponent.getBuffLevel(Buff.APOCALYPSE_MAGIC_SHIELD, player).ifPresent((level) -> {
                        event.setDamage(Math.max(0, event.getDamage() - ChanceUtil.getRandom(level)));
                    });
                }

                break;
            }

            default: {
                if (attacker instanceof Player && checkEntity(target)) {
                    Player player = (Player) attacker;

                    // Handle damage buff
                    buffComponent.getBuffLevel(Buff.APOCALYPSE_DAMAGE_BOOST, player).ifPresent((level) -> {
                        event.setDamage(event.getDamage() + ChanceUtil.getRandom(level));
                    });

                    processOverlord(player, target, result.hasProjectile());
                }

                break;
            }
        }
    }

    private void cleanupRespawnPoint(Location respawnPoint) {
        int safeRespawnRadius = config.safeRespawnRadius * config.safeRespawnRadius;

        // Ensure the radius is at least 2
        if (safeRespawnRadius < 4) safeRespawnRadius = 4;

        for (Entity entity : respawnPoint.getWorld().getEntitiesByClass(attackMob)) {
            if (!(entity instanceof LivingEntity) || !checkEntity((LivingEntity) entity)) continue;
            if (entity.getLocation().distanceSquared(respawnPoint) < safeRespawnRadius) entity.remove();

            for (int i = 0; i < 20; i++) {
                entity.getWorld().playEffect(entity.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
            }
        }
    }

    private void boostPlayer(Player player, Location respawnLocation) {
        World world = respawnLocation.getWorld();
        if (!hasThunderstorm(world)) {
            return;
        }

        ApocalypseRespawnBoostEvent apocalypseEvent = new ApocalypseRespawnBoostEvent(player, respawnLocation);
        server.getPluginManager().callEvent(apocalypseEvent);
        if (apocalypseEvent.isCancelled()) {
            return;
        }

        server.getScheduler().scheduleSyncDelayedTask(inst, () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 45, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1));
        }, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (config.enableSafeRespawn) {
            cleanupRespawnPoint(event.getRespawnLocation());
            boostPlayer(event.getPlayer(), event.getRespawnLocation());
        }
        sessions.getSession(ApocalypseSession.class, event.getPlayer()).updateDeath(config.deathGrace);
    }

    private void maybeIncreaseBuff(Player player) {
        if (!hasThunderstorm(player.getWorld())) {
            return;
        }

        if (!ChanceUtil.getChance(config.buffChance)) {
            return;
        }

        switch (ChanceUtil.getRandom(4)) {
            case 1:
                buffComponent.notifyIncrease(Buff.APOCALYPSE_OVERLORD, player);
                break;
            case 2:
                buffComponent.notifyIncrease(Buff.APOCALYPSE_MAGIC_SHIELD, player);
                break;
            case 3:
                buffComponent.notifyIncrease(Buff.APOCALYPSE_DAMAGE_BOOST, player);
                break;
            case 4:
                buffComponent.notifyIncrease(Buff.APOCALYPSE_LIFE_LEACH, player);
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity ent = event.getEntity();
        World world = ent.getWorld();

        Player killer = ent.getKiller();
        if (ent instanceof Skeleton &&  killer != null) {
            ItemStack held = ent.getEquipment().getItemInHand();
            if (held != null && held.getTypeId() == ItemID.BOW) {
                if (hasThunderstorm(world) && ChanceUtil.getChance(5)) {
                    event.getDrops().add(new ItemStack(ItemID.ARROW, (ChanceUtil.getRandom(8) * 2)));
                } else {
                    event.getDrops().add(new ItemStack(ItemID.ARROW, (ChanceUtil.getRandom(8))));

                }
            }
        }

        if (checkEntity(ent)) {
            event.getDrops().removeIf(next -> next != null && next.getTypeId() == ItemID.ROTTEN_FLESH);

            if (attackMob.isInstance(ent)) {
                if (ChanceUtil.getChance(5)) {
                    event.getDrops().add(new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandomNTimes(16, 7)));
                }

                if (killer != null) {
                    maybeIncreaseBuff(killer);

                    buffComponent.getBuffLevel(Buff.APOCALYPSE_LIFE_LEACH, killer).ifPresent((level) -> {
                        EntityUtil.heal(killer, level);
                    });
                }
            }

            if (ChanceUtil.getChance(10000)) {
                event.getDrops().add(CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
            }
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
        if (hasThunderstorm(world) && !lightning.isEffect()) {
            lightning(lightningStrikeLoc);
        }
    }

    public void lightning(Location location) {

        Location strikeLoc = LocationUtil.findFreePosition(location);

        if (strikeLoc == null) return;

        // Spawn zombies at the strike location.
        strikeSpawn(strikeLoc);

        // Get players on this world.
        List<Player> applicable = location.getWorld().getPlayers();

        // Kill the flight of all players. They've been "startled" by the lighting strike.
        disableFlight(applicable);

        // Remove any players that have recently died from the list of applicable players.
        applicable.removeIf((player) -> sessions.getSession(ApocalypseSession.class, player).recentlyDied());

        // Spawn to all remaining players.
        bedSpawn(applicable);
        localSpawn(applicable);
    }

    private void disableFlight(List<Player> applicable) {
        applicable.stream().filter(GeneralPlayerUtil::takeFlightSafely).forEach(player -> {
            ChatUtil.sendNotice(player, "The lightning hinders your ability to fly.");
        });
    }

    public void bedSpawn(Player player, int multiplier) {
        // Find a free position at or near the player's bed.
        Optional<Location> bedLocation = warpsComponent.getRawBedLocation(player);
        if (bedLocation.isEmpty()) {
            return;
        }
        Optional<Location> freeBedLocation = warpsComponent.getBedLocation(player);

        // If the player has a bed location, but there's no "free" location, redirect and give them
        // an extra local spawn, they probably tried to out smart the mechanic.
        if (freeBedLocation.isEmpty()) {
            ChatUtil.sendWarning(player, "The zombies spawning at your bed couldn't find anywhere to spawn!");
            ChatUtil.sendWarning(player, "So... Instead they came to you!");

            localSpawn(player, multiplier);

            return;
        }

        // Fire an event for the bed spawn.
        ApocalypseBedSpawnEvent apocalypseEvent = new ApocalypseBedSpawnEvent(
          player, freeBedLocation.get(), ChanceUtil.getRandom(multiplier)
        );
        server.getPluginManager().callEvent(apocalypseEvent);
        if (apocalypseEvent.isCancelled()) {
            return;
        }

        // Spawn however many zombies we determined need spawned.
        ZombieSpawnConfig bedSpawnConfig = new ZombieSpawnConfig();
        for (int i = 0; i < apocalypseEvent.getNumberOfZombies(); i++) {
            spawnAndArm(apocalypseEvent.getLocation(), attackMob, bedSpawnConfig);
        }
    }

    public int getAmplification() {
        return ChanceUtil.getRandomNTimes(config.amplificationNoise, config.amplificationDescale);
    }

    public void strikeSpawn(Location strikeLocation) {
        ApocalypseLightningStrikeSpawnEvent apocalypseEvent = new ApocalypseLightningStrikeSpawnEvent(strikeLocation);
        server.getPluginManager().callEvent(apocalypseEvent);
        if (apocalypseEvent.isCancelled()) {
            return;
        }

        ZombieSpawnConfig strikeSpawnConfig = new ZombieSpawnConfig();
        strikeSpawnConfig.allowItemPickup = true;
        strikeSpawnConfig.allowMiniBoss = true;

        int multiplier = config.strikeMultiplier * config.amplificationNoise;
        for (int i = 0; i < multiplier; ++i) {
            spawnAndArm(strikeLocation, attackMob, strikeSpawnConfig);
        }
    }

    public void bedSpawn(List<Player> players) {
        for (Player player : players) {
            bedSpawn(player, config.bedMultiplier * getAmplification());
        }
    }

    public void localSpawn(Player player, int multiplier) {
        ZombieSpawnConfig localSpawnConfig = new ZombieSpawnConfig();
        for (int i = 0; i < multiplier; ++i) {
            Location l = findLocation(player.getLocation());

            ApocalypseLocalSpawnEvent apocalypseEvent = new ApocalypseLocalSpawnEvent(player, l);
            server.getPluginManager().callEvent(apocalypseEvent);
            if (apocalypseEvent.isCancelled()) {
                continue;
            }

            spawnAndArm(apocalypseEvent.getLocation(), attackMob, localSpawnConfig);
        }
    }

    public void localSpawn(List<Player> players) {
        for (Player player : players) {
            if (ChanceUtil.getChance(config.localSpawnChance))
                continue;

            localSpawn(player, config.localMultiplier * getAmplification());
        }
    }

    public Location findLocation(Location origin) {
        Location l = LocationUtil.findRandomLoc(origin, 8, true, false);
        return BlockType.isTranslucent(l.getBlock().getTypeId()) ? l : origin;
    }

    public boolean checkEntity(LivingEntity e) {

        return hasThunderstorm(e.getWorld())
                || (e.getCustomName() != null && e.getCustomName().equals("Apocalyptic Zombie"));
    }

    private <T extends LivingEntity> void spawnAndArm(Location location, Class<T> clazz, ZombieSpawnConfig spawnConfig) {
        if (!location.getChunk().isLoaded()) return;

        Entity e = spawn(location, clazz, spawnConfig);
        if (e == null) return;
        if (e instanceof Zombie && ChanceUtil.getChance(config.babyChance)) {
            ((Zombie) e).setBaby(true);
        }

        arm(e, spawnConfig);
    }

    private <T extends LivingEntity> T spawn(Location location, Class<T> clazz, ZombieSpawnConfig spawnConfig) {
        if (location == null) return null;
        T entity = location.getWorld().spawn(location, clazz);

        entity.setCustomName("Apocalyptic Zombie");
        entity.setCustomNameVisible(false);

        if (ChanceUtil.getChance(config.bossChance) && spawnConfig.allowMiniBoss) {
            bossManager.bind(entity);
        }

        return entity;
    }

    private void arm(Entity e, ZombieSpawnConfig spawnConfig) {
        // FIXME: This is 90% the same as the grave yard arm logic
        if (!(e instanceof LivingEntity)) return;

        EntityEquipment equipment = ((LivingEntity) e).getEquipment();
        ((LivingEntity) e).setCanPickupItems(spawnConfig.allowItemPickup);

        if (ChanceUtil.getChance(config.armourChance)) {
            if (ChanceUtil.getChance(35)) {
                equipment.setArmorContents(ItemUtil.DIAMOND_ARMOR);
            } else {
                equipment.setArmorContents(ItemUtil.IRON_ARMOR);
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

        equipment.setItemInHandDropChance(0.005F);
        equipment.setHelmetDropChance(0.005F);
        equipment.setChestplateDropChance(0.005F);
        equipment.setLeggingsDropChance(0.005F);
        equipment.setBootsDropChance(0.005F);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {
        if (!event.toThunderState()) {
            buffComponent.clearBuffs(BuffCategory.APOCALYPSE);
        }
    }

    private static class ZombieSpawnConfig {
        public boolean allowItemPickup = false;
        public boolean allowMiniBoss = false;

        public ZombieSpawnConfig() { }
    }

    private static class ApocalypseSession extends PersistentSession {

        private long nextAttack = 0;

        protected ApocalypseSession() {
            super(THIRTY_MINUTES);
        }

        public boolean recentlyDied() {
            return nextAttack != 0 && nextAttack < System.currentTimeMillis();
        }

        public void updateDeath(long time) {
            nextAttack = System.currentTimeMillis() + time;
        }
    }
}
