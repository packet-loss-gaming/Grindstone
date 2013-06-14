package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.skelril.aurora.anticheat.AntiCheatCompatibilityComponent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.events.entity.ProjectileTickEvent;
import com.skelril.aurora.prayer.PrayerFX.HulkFX;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.h31ix.anticheat.manage.CheckType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.skelril.aurora.events.custom.item.SpecialAttackEvent.Specs;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Custom Items Component", desc = "Custom Items")
@Depend(components = {AntiCheatCompatibilityComponent.class})
public class CustomItemsComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AntiCheatCompatibilityComponent antiCheat;

    private WorldGuardPlugin WG;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");
        WG = plugin != null && plugin instanceof WorldGuardPlugin ? (WorldGuardPlugin) plugin : null;
    }

    private ConcurrentHashMap<String, Long> fearSpec = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> unleashedSpec = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> animalSpec = new ConcurrentHashMap<>();

    private boolean canFearSpec(String name) {

        return !fearSpec.containsKey(name) || System.currentTimeMillis() - fearSpec.get(name) >= 3800;
    }

    private boolean canUnleashedSpec(String name) {

        return !unleashedSpec.containsKey(name) || System.currentTimeMillis() - unleashedSpec.get(name) >= 3800;
    }

    private boolean canAnimalSpec(String name) {

        return !animalSpec.containsKey(name) || System.currentTimeMillis() - animalSpec.get(name) >= 15000;
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
                    used = callSpec(owner, target, 5, 10);
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
                            event.setDamage(EffectUtil.Fear.fearStrike(owner, target, event.getDamage(), WG));
                            break;
                        case FEAR_BOMB:
                            EffectUtil.Fear.fearBomb(owner, target, WG);
                            break;
                    }
                }
            }

            if (canUnleashedSpec(owner.getName())) {
                if (ItemUtil.hasUnleashedSword(owner)) {
                    used = callSpec(owner, target, 10, 16);
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
                            EffectUtil.Unleashed.doomBlade(owner, target, WG);
                            break;
                        case LIFE_LEECH:
                            EffectUtil.Unleashed.lifeLeech(owner, target);
                            break;
                    }
                } else if (ItemUtil.hasUnleashedBow(owner)) {
                    //used = callSpec(owner, target, 17, 18);
                    //if (used == null) return;
                    unleashedSpec.put(owner.getName(), System.currentTimeMillis());
                    ChatUtil.sendError(owner, "This weapon is currently a WIP.");
                }
            }
        }
    }

    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();
        Entity shooter = projectile.getShooter();

        if (shooter != null && shooter instanceof Player) {

            Player owner = (Player) shooter;
            Location targetLoc = event.getEntity().getLocation();

            RegionManager mgr = WG != null ? WG.getGlobalRegionManager().get(owner.getWorld()) : null;

            if (canAnimalSpec(owner.getName())) {
                Specs used;
                if (ItemUtil.hasBatBow(owner)) {
                    used = callSpec(owner, targetLoc, Specs.MOB_ATTACK);
                    if (used == null) return;
                    animalSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (used) {
                        case MOB_ATTACK:
                            EffectUtil.Strange.mobBarrage(owner, targetLoc, EntityType.BAT);
                            break;
                    }
                } else if (ItemUtil.hasChickenBow(owner)) {
                    used = callSpec(owner, targetLoc, Specs.MOB_ATTACK);
                    if (used == null) return;
                    animalSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (used) {
                        case MOB_ATTACK:
                            EffectUtil.Strange.mobBarrage(owner, targetLoc, EntityType.CHICKEN);
                            break;
                    }
                }
            }

            if (!canFearSpec(owner.getName())) {

                if (ItemUtil.hasFearBow(owner)) {
                    if (!targetLoc.getWorld().isThundering() && targetLoc.getBlock().getLightFromSky() > 0) {
                        // Simulate a lightning strike
                        targetLoc.getWorld().strikeLightningEffect(targetLoc);
                        for (Entity e : projectile.getNearbyEntities(2, 4, 2)) {
                            if (!e.isValid() || !(e instanceof LivingEntity)) continue;
                            // Pig Zombie
                            if (e instanceof Pig) {
                                e.getWorld().spawnEntity(e.getLocation(), EntityType.PIG_ZOMBIE);
                                e.remove();
                                continue;
                            }
                            // Creeper
                            if (e instanceof Creeper) {
                                ((Creeper) e).setPowered(true);
                            }
                            // Player
                            if (mgr != null && e instanceof Player) {
                                ApplicableRegionSet app = mgr.getApplicableRegions(e.getLocation());
                                if (!app.allows(DefaultFlag.PVP)) continue;
                            }

                            ((LivingEntity) e).damage(5);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArrowTick(ProjectileTickEvent event) {

        Entity shooter = event.getEntity().getShooter();

        if (shooter != null && shooter instanceof Player) {

            final Location location = event.getEntity().getLocation();
            if (ItemUtil.hasBatBow((Player) shooter)) {

                if (!ChanceUtil.getChance(5)) return;
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
            } else if (ItemUtil.hasChickenBow((Player) shooter)) {

                if (!ChanceUtil.getChance(5)) return;
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        final Chicken chicken = (Chicken) location.getWorld().spawnEntity(location, EntityType.CHICKEN);
                        server.getScheduler().runTaskLater(inst, new Runnable() {

                            @Override
                            public void run() {

                                if (chicken.isValid()) {
                                    chicken.remove();
                                    for (int i = 0; i < 20; i++) {
                                        chicken.getWorld().playEffect(chicken.getLocation(), Effect.SMOKE, 0);
                                    }
                                }
                            }
                        }, 20 * 3);
                    }
                }, 3);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(handleRightClick(player, event.getClickedBlock().getLocation(), itemStack));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        event.setCancelled(handleRightClick(player, event.getRightClicked().getLocation(), itemStack));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        event.setCancelled(handleRightClick(player, event.getBlockClicked().getLocation(), itemStack));
        //noinspection deprecation
        player.updateInventory();
    }

    public boolean handleRightClick(Player player, Location location, ItemStack itemStack) {

        if (ItemUtil.matchesFilter(itemStack, ChatColor.DARK_PURPLE + "Magic Bucket")) {
            player.setAllowFlight(!player.getAllowFlight());
            if (player.getAllowFlight()) {
                antiCheat.exempt(player, CheckType.FLY);
                ChatUtil.sendNotice(player, "The bucket glows brightly.");
            } else {
                antiCheat.unexempt(player, CheckType.FLY);
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (ItemUtil.matchesFilter(itemStack, ChatColor.DARK_PURPLE + "Magic Bucket")) {
            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    ItemStack[] contents = player.getInventory().getContents();
                    if (!ItemUtil.findItemOfName(contents, ChatColor.DARK_PURPLE + "Magic Bucket")) {
                        player.setAllowFlight(false);
                        antiCheat.unexempt(player, CheckType.FLY);
                        ChatUtil.sendNotice(player, "The power of the bucket fades.");
                    }
                }
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        if (!player.getAllowFlight()) return;

        ItemStack[] chestContents = event.getInventory().getContents();
        if (!ItemUtil.findItemOfName(chestContents, ChatColor.DARK_PURPLE + "Magic Bucket")) return;

        ItemStack[] contents = player.getInventory().getContents();
        if (!ItemUtil.findItemOfName(contents, ChatColor.DARK_PURPLE + "Magic Bucket")) {
            player.setAllowFlight(false);
            antiCheat.unexempt(player, CheckType.FLY);
            ChatUtil.sendNotice(player, "The power of the bucket fades.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        ItemStack[] drops = event.getDrops().toArray(new ItemStack[event.getDrops().size()]);

        if (ItemUtil.findItemOfName(drops, ChatColor.DARK_PURPLE + "Magic Bucket")) {
            player.setAllowFlight(false);
            antiCheat.unexempt(player, CheckType.FLY);
            ChatUtil.sendNotice(player, "The power of the bucket fades.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {

        ItemStack stack = event.getItem();
        if (ItemUtil.matchesFilter(stack, ChatColor.BLUE + "God Fish")) {

            Player player = event.getPlayer();
            player.chat("The fish flow within me!");
            new HulkFX().add(player);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItem().getItemStack();

        if (itemStack.getTypeId() == ItemID.GOLD_BAR || itemStack.getTypeId() == ItemID.GOLD_NUGGET) {

            if (!ItemUtil.findItemOfName(player.getInventory().getContents(), ChatColor.AQUA + "Imbued Crystal")) {
                return;
            }
            server.getScheduler().runTaskLater(inst, new Runnable() {

                @Override
                public void run() {

                    int nugget = ItemUtil.countItemsOfType(player.getInventory().getContents(), ItemID.GOLD_NUGGET);
                    while (nugget / 9 > 0 && player.getInventory().firstEmpty() != -1) {
                        player.getInventory().removeItem(new ItemStack(ItemID.GOLD_NUGGET, 9));
                        player.getInventory().addItem(new ItemStack(ItemID.GOLD_BAR));
                        nugget -= 9;
                    }

                    int bar = ItemUtil.countItemsOfType(player.getInventory().getContents(), ItemID.GOLD_BAR);
                    while (bar / 9 > 0 && player.getInventory().firstEmpty() != -1) {
                        player.getInventory().removeItem(new ItemStack(ItemID.GOLD_BAR, 9));
                        player.getInventory().addItem(new ItemStack(BlockID.GOLD_BLOCK));
                        bar -= 9;
                    }

                    //noinspection deprecation
                    player.updateInventory();
                }
            }, 1);
        }
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
        if (animalSpec.containsKey(name)) animalSpec.remove(name);
    }
}
