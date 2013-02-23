package us.arrowcraft.aurora.city.engine.arena;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import us.arrowcraft.aurora.admin.AdminComponent;
import us.arrowcraft.aurora.events.CreepSpeakEvent;
import us.arrowcraft.aurora.events.PrayerApplicationEvent;
import us.arrowcraft.aurora.events.ThrowPlayerEvent;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.EffectUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;
import us.arrowcraft.aurora.util.ItemUtil;
import us.arrowcraft.aurora.util.player.PlayerState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class GiantBossArena extends AbstractRegionedArena implements BossArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private Giant boss = null;
    private long lastAttack = 0;
    private long lastDeath = 0;
    private boolean damageHeals = false;
    private BukkitTask mobDestroyer;
    private Random random = new Random();

    private List<Location> spawnPts = new ArrayList<>();
    private final HashMap<String, PlayerState> playerState = new HashMap<>();

    public GiantBossArena(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        // Mob Destroyer
        mobDestroyer = server.getScheduler().runTaskTimer(inst, new Runnable() {

            @Override
            public void run() {

                if (!getWorld().isThundering()) removeOutsideZombies();
            }
        }, 0, 20 * 2);

        // First spawn requirement
        getMinionSpawnPts();
    }

    @Override
    public boolean isBossSpawned() {

        if (!BukkitUtil.toLocation(getWorld(), getRegion().getMinimumPoint()).getChunk().isLoaded()) return true;
        boolean found = false;
        for (Entity e : getContainedEntities()) {

            if (e.isValid() && e instanceof Giant) {
                if (!found) {
                    boss = (Giant) e;
                    found = true;
                } else e.remove();
            }
        }
        return boss != null && boss.isValid();
    }

    @Override
    public void spawnBoss() {

        spawnPts.clear();

        BlockVector min = getRegion().getMinimumPoint();
        BlockVector max = getRegion().getMaximumPoint();

        getMinionSpawnPts(min, max);

        Region region = new CuboidRegion(min, max);
        Location l = BukkitUtil.toLocation(getWorld(), region.getCenter().setY(82));
        boss = (Giant) getWorld().spawnEntity(l, EntityType.GIANT);
        boss.setMaxHealth(750);
        boss.setHealth(750);
        boss.setRemoveWhenFarAway(false);
    }

    public void getMinionSpawnPts() {

        getMinionSpawnPts(getRegion().getMinimumPoint(), getRegion().getMaximumPoint());
    }

    public void getMinionSpawnPts(BlockVector min, BlockVector max) {

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        BlockState block;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; --y) {
                    block = getWorld().getBlockAt(x, y, z).getState();
                    if (!block.getChunk().isLoaded()) block.getChunk().load();
                    if (block.getTypeId() == BlockID.GOLD_BLOCK) {
                        spawnPts.add(block.getLocation().add(0, 2, 0));
                    }
                }
            }
        }
    }

    @Override
    public LivingEntity getBoss() {

        return boss;
    }

    @Override
    public void forceRestoreBlocks() {

        // Nothing to do here... YET!
    }

    @Override
    public void run() {

        if (!isBossSpawned() && (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 2)) {
            removeMobs();
            spawnBoss();
        } else if (!isEmpty()) {
            equalize();
            hurt();
            runTargetAI(ChanceUtil.getRandom(OPTION_COUNT));
        }
    }

    public void removeMobs() {

        for (Entity e : getContainedEntities(1)) {

            if (EnvironmentUtil.isHostileEntity(e)) {
                for (int i = 0; i < 20; i++) getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
                e.remove();
            }
        }
    }

    public void removeOutsideZombies() {

        for (Entity e : getContainedEntities(1)) {
            if (e instanceof Zombie && ((Zombie) e).isBaby() && !contains(e)) {
                for (int i = 0; i < 20; i++) getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
                e.remove();
            }
        }
    }

    @Override
    public void disable() {

        removeMobs();
        removeOutsideZombies();
        if (mobDestroyer != null) mobDestroyer.cancel();
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        for (Player player : getContainedPlayers()) {
            try {
                adminComponent.standardizePlayer(player);
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        if (contains(event.getPlayer(), 1) || contains(event.getTargeter(), 1)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {

        Entity e = event.getEntity();
        if (contains(e) && !event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {

        Entity defender = event.getEntity();
        Entity attacker = null;

        if (!contains(defender)) return;
        if (event instanceof EntityDamageByEntityEvent) attacker = ((EntityDamageByEntityEvent) event).getDamager();

        if (attacker != null) {
            if (attacker instanceof Projectile) {
                if (((Projectile) attacker).getShooter() != null) {
                    attacker = ((Projectile) attacker).getShooter();
                } else return;
            }

            if (defender instanceof Giant && attacker instanceof Player && !contains(attacker)) {

                // Heal boss
                boss.setHealth(Math.min(boss.getMaxHealth(), event.getDamage() + boss.getHealth()));

                // Evil code of doom
                ChatUtil.sendNotice((Player) attacker, "Come closer...");
                attacker.teleport(boss.getLocation());
                ((Player) attacker).damage(96, boss);
                server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) attacker));
                attacker.setVelocity(new Vector(
                        random.nextDouble() * 1.7 - 1.5,
                        random.nextDouble() * 2,
                        random.nextDouble() * 1.7 - 1.5
                ));
            }

            if (attacker instanceof Player) {

                Player player = (Player) attacker;
                if (ItemUtil.hasMasterSword(player) && defender instanceof LivingEntity) {

                    if (ChanceUtil.getChance(10)) {
                        EffectUtil.Master.healingBlade(player, (LivingEntity) defender);
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
        }

        if (attacker != null && !contains(attacker) || !contains(defender)) return;

        Player[] contained = getContainedPlayers();

        if (defender instanceof Giant) {
            Giant boss = (Giant) defender;
            if (damageHeals) {
                boss.setHealth(Math.min(boss.getMaxHealth(), event.getDamage() + boss.getHealth()));
            }

            if (ChanceUtil.getChance(7)) {
                for (Location spawnPt : spawnPts) {
                    if (ChanceUtil.getChance(18)) {
                        for (int i = 0; i < Math.max(3, contained.length); i++) {
                            Zombie z = (Zombie) getWorld().spawnEntity(spawnPt, EntityType.ZOMBIE);
                            z.setBaby(true);
                            EntityEquipment equipment = z.getEquipment();
                            equipment.setItemInHand(new ItemStack(ItemID.STONE_SWORD));
                            if (attacker != null && attacker instanceof LivingEntity) {
                                z.setTarget((LivingEntity) attacker);
                            }
                        }
                    }
                }
            }
        } else if (defender instanceof Player) {
            Player player = (Player) defender;
            if (ItemUtil.hasAncientArmour(player)) {
                if (attacker != null) {
                    if (attacker instanceof Zombie) {
                        Zombie zombie = (Zombie) attacker;
                        if (zombie.isBaby() && ChanceUtil.getChance(14)) {
                            ChatUtil.sendNotice(player, "Your armour weakens the zombies.");
                            for (Entity e : player.getNearbyEntities(8, 8, 8)) {
                                if (e.isValid() && e instanceof Zombie && ((Zombie) e).isBaby()) {
                                    ((Zombie) e).damage(18);
                                }
                            }
                        }
                    }

                    if (ChanceUtil.getChance(7)) EffectUtil.Ancient.powerBurst(player, event.getDamage());
                }
                if (ChanceUtil.getChance(3) && defender.getFireTicks() > 0) {
                    ChatUtil.sendNotice((Player) defender, "Your armour extinguishes the fire.");
                    defender.setFireTicks(0);
                }
                if (damageHeals && ChanceUtil.getChance(10)) {
                    ChatUtil.sendNotice(getContainedPlayers(),
                            player.getDisplayName() + " has broken the giant's spell.");
                    damageHeals = false;
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        if (contains(event.getEntity())) {
            Entity e = event.getEntity();
            if (e instanceof Giant) {
                for (int i = 0; i < ChanceUtil.getRandom(35); i++) {
                    event.getDrops().add(new ItemStack(BlockID.GOLD_BLOCK, 64));
                }
                if (ChanceUtil.getChance(250)) {
                    ItemStack masterSword = new ItemStack(Material.DIAMOND_SWORD);
                    ItemMeta masterMeta = masterSword.getItemMeta();
                    masterMeta.addEnchant(Enchantment.DAMAGE_ALL, 10, true);
                    masterMeta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 10, true);
                    masterMeta.addEnchant(Enchantment.DAMAGE_UNDEAD, 10, true);
                    masterMeta.addEnchant(Enchantment.FIRE_ASPECT, 10, true);
                    masterMeta.addEnchant(Enchantment.KNOCKBACK, 10, true);
                    masterMeta.addEnchant(Enchantment.LOOT_BONUS_MOBS, 10, true);
                    masterMeta.setDisplayName(ChatColor.DARK_PURPLE + "Master Sword");
                    ((Repairable) masterMeta).setRepairCost(400);
                    masterSword.setItemMeta(masterMeta);

                    event.getDrops().add(masterSword);
                }
                lastDeath = System.currentTimeMillis();
                boss = null;
                int amt = getContainedPlayers() != null ? getContainedPlayers().length : 0;
                for (int i = 0; i < Math.min(500, Math.max(21, amt ^ 3)); i++) {
                    Zombie z = (Zombie) getWorld().spawnEntity(event.getEntity().getLocation(), EntityType.ZOMBIE);
                    z.setBaby(true);
                    EntityEquipment equipment = z.getEquipment();
                    equipment.setItemInHand(new ItemStack(ItemID.DIAMOND_SWORD));
                }
                event.setDroppedExp(256);
            } else if (e instanceof Zombie && ((Zombie) e).isBaby()) {
                event.setDroppedExp(14);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        if (contains(player, 1)) {
            boss.setHealth(Math.min(boss.getMaxHealth(), boss.getHealth() + 40));
            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();
            // event.setDroppedExp(0);
            // event.setKeepLevel(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !adminComponent.isAdmin(player)) {

            try {
                PlayerState identity = playerState.get(player.getName());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                // player.setLevel(identity.getLevel());
                // player.setExp(identity.getExperience());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playerState.remove(player.getName());
            }
        }
    }

    private void hurt() {

        Player[] contained = getContainedPlayers();
        if (!isBossSpawned() || contained == null || contained.length <= 0) return;

        for (Player player : contained) {

            if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                player.damage(32, boss);
            }
        }
    }

    private final int OPTION_COUNT = 6;

    private void runTargetAI(int attackCase) {

        if (!isBossSpawned() || (lastAttack != 0
                && System.currentTimeMillis() - lastAttack <= ChanceUtil.getRangedRandom(13000, 17000))) return;

        Player[] contained = getContainedPlayers();
        if (contained == null || contained.length <= 0) return;

        for (Player player : contained) {

            if (player.hasPotionEffect(PotionEffectType.REGENERATION)) {
                player.setHealth(1);
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }

        if (attackCase < 1 || attackCase > OPTION_COUNT) attackCase = ChanceUtil.getRandom(OPTION_COUNT);
        switch (attackCase) {
            case 1:
                ChatUtil.sendWarning(contained, "Taste my wrath!");
                for (Player player : contained) {

                    // Call this event to notify AntiCheat
                    server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                    player.setVelocity(new Vector(
                            random.nextDouble() * 1.7 - 1.5,
                            random.nextDouble() * 2,
                            random.nextDouble() * 1.7 - 1.5
                    ));
                    player.setFireTicks(54);
                }
                break;
            case 2:
                ChatUtil.sendWarning(contained, "Embrace my corruption!");
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 12, 1));
                }
                break;
            case 3:
                ChatUtil.sendWarning(contained, "Are you BLIND? Mwhahahaha!");
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 1));
                }
                break;
            case 4:
                ChatUtil.sendWarning(contained, ChatColor.DARK_RED + "Lets dance...");
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        if (!isBossSpawned()) return;
                        for (Player player : getContainedPlayers()) {
                            if (player.getLocation().getBlock().getRelative(BlockFace.DOWN, 2).getTypeId()
                                    != BlockID.DIAMOND_BLOCK) {
                                ChatUtil.sendNotice(player, "Come closer...");
                                player.teleport(boss.getLocation());
                                player.damage(104, boss);

                                // Call this event to notify AntiCheat
                                server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                                player.setVelocity(new Vector(
                                        random.nextDouble() * 1.7 - 1.5,
                                        random.nextDouble() * 2,
                                        random.nextDouble() * 1.7 - 1.5
                                ));
                            } else {
                                ChatUtil.sendNotice(player, "Fine... No tango this time...");
                            }
                        }
                    }
                }, 20 * 7);
                break;
            case 5:
                if (!damageHeals) {
                    ChatUtil.sendWarning(contained, "I am everlasting!");
                    damageHeals = true;
                    server.getScheduler().runTaskLater(inst, new Runnable() {

                        @Override
                        public void run() {

                            if (damageHeals) {
                                damageHeals = false;
                                if (!isBossSpawned()) return;
                                ChatUtil.sendNotice(getContainedPlayers(), "Thank you for your assistance.");
                            }
                        }
                    }, 20 * 30);
                    break;
                }
                runTargetAI(ChanceUtil.getRandom(OPTION_COUNT));
                return;
            case 6:
                ChatUtil.sendWarning(contained, "Fire is your friend...");
                for (Player player : contained) {
                    player.setFireTicks(20 * 45);
                }
                break;
        }
        lastAttack = System.currentTimeMillis();
    }
}
