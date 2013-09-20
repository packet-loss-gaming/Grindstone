package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.FreezeComponent;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.anticheat.AntiCheatCompatibilityComponent;
import com.skelril.aurora.events.custom.item.SpecialAttackEvent;
import com.skelril.aurora.events.entity.ProjectileTickEvent;
import com.skelril.aurora.prayer.PrayerFX.HulkFX;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.InventoryUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.h31ix.anticheat.manage.CheckType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.skelril.aurora.events.custom.item.SpecialAttackEvent.Specs;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Custom Items Component", desc = "Custom Items")
@Depend(components = {AdminComponent.class, AntiCheatCompatibilityComponent.class, FreezeComponent.class})
public class CustomItemsComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private AntiCheatCompatibilityComponent antiCheat;
    @InjectComponent
    private FreezeComponent freeze;

    private WorldGuardPlugin WG;

    private List<String> players = new ArrayList<>();

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
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {

        Entity healed = event.getEntity();

        if (healed instanceof Player) {

            Player player = (Player) healed;

            if (ItemUtil.matchesFilter(player.getInventory().getHelmet(), ChatColor.GOLD + "Ancient Crown")) {
                event.setAmount(event.getAmount() * 2.5);
            }
        }
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
                    used = callSpec(owner, target, 0, 6);
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
                        case DECIMATE:
                            EffectUtil.Fear.decimate(owner, target);
                            break;
                        case SOUL_SMITE:
                            EffectUtil.Fear.soulSmite(owner, target);
                            break;
                    }
                } else if (ItemUtil.hasFearBow(owner)) {
                    used = callSpec(owner, target, 6, 11);
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
                    used = callSpec(owner, target, 11, 17);
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
                    used = callSpec(owner, target, 18, 20);
                    if (used == null) return;
                    unleashedSpec.put(owner.getName(), System.currentTimeMillis());
                    switch (used) {
                        case HEALING_LIFE_LEECH:
                            EffectUtil.Unleashed.lifeLeech(owner, target);
                            break;
                        case EVIL_FOCUS:
                            EffectUtil.Unleashed.evilFocus(owner, target, freeze);
                            break;
                    }
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

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && handleRightClick(player, event.getClickedBlock().getLocation(), itemStack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        if (handleRightClick(player, event.getRightClicked().getLocation(), itemStack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        if (handleRightClick(player, event.getBlockClicked().getLocation(), itemStack)) {
            event.setCancelled(true);
        }
        //noinspection deprecation
        player.updateInventory();
    }

    public boolean handleRightClick(final Player player, Location location, ItemStack itemStack) {

        if (admin.isAdmin(player)) return false;

        final long currentTime = System.currentTimeMillis();

        if (ItemUtil.matchesFilter(itemStack, ChatColor.DARK_PURPLE + "Magic Bucket")) {
            player.setAllowFlight(!player.getAllowFlight());
            if (player.getAllowFlight()) {
                player.setFlySpeed(.4F);
                antiCheat.exempt(player, CheckType.FLY);
                ChatUtil.sendNotice(player, "The bucket glows brightly.");
            } else {
                player.setFlySpeed(.1F);
                antiCheat.unexempt(player, CheckType.FLY);
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            return true;
        } else if (ItemUtil.matchesFilter(itemStack, ChatColor.GOLD + "Pixie Dust")) {

            if (player.getAllowFlight()) return false;

            if (players.contains(player.getName())) {

                ChatUtil.sendError(player, "You need to wait to regain your faith, and trust.");
                return false;
            }

            player.setAllowFlight(true);
            player.setFlySpeed(1);
            antiCheat.exempt(player, CheckType.FLY);

            ChatUtil.sendNotice(player, "You use the Pixie Dust to gain flight.");

            IntegratedRunnable integratedRunnable = new IntegratedRunnable() {
                @Override
                public boolean run(int times) {

                    // Just get out of here you stupid players who don't exist!
                    if (!player.isValid()) return true;

                    if (player.getAllowFlight()) {
                        int c = ItemUtil.countItemsOfName(player.getInventory().getContents(), ChatColor.GOLD + "Pixie Dust") - 1;

                        if (c >= 0) {
                            ItemStack[] itemStacks = ItemUtil.removeItemOfName(player.getInventory().getContents(), ChatColor.GOLD + "Pixie Dust");
                            player.getInventory().setContents(itemStacks);

                            int amount = Math.min(c, 64);
                            while (amount > 0) {
                                player.getInventory().addItem(ItemUtil.Misc.pixieDust(amount));
                                c -= amount;
                                amount = Math.min(c, 64);
                            }

                            //noinspection deprecation
                            player.updateInventory();

                            if (System.currentTimeMillis() >= currentTime + 13000) {
                                ChatUtil.sendNotice(player, "You use some more Pixie Dust to keep flying.");
                            }
                            return false;
                        }
                        ChatUtil.sendWarning(player, "The effects of the Pixie Dust are about to wear off!");
                    }
                    return true;
                }

                @Override
                public void end() {

                    if (player.isValid()) {
                        if (player.getAllowFlight()) {
                            ChatUtil.sendNotice(player, "You are no longer influenced by the Pixie Dust.");
                            antiCheat.unexempt(player, CheckType.FLY);
                        }
                        player.setFallDistance(0);
                        player.setAllowFlight(false);
                        player.setFlySpeed(.1F);
                    }
                }
            };

            TimedRunnable runnable = new TimedRunnable(integratedRunnable, 1);
            BukkitTask task = server.getScheduler().runTaskTimer(inst, runnable, 0, 20 * 15);
            runnable.setTask(task);
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {

        Player player = event.getPlayer();

        if (event.isSneaking() && player.getAllowFlight() && player.isOnGround() && !admin.isAdmin(player)) {

            if (!ItemUtil.findItemOfName(player.getInventory().getContents(), ChatColor.GOLD + "Pixie Dust")) return;

            player.setAllowFlight(false);
            antiCheat.unexempt(player, CheckType.FLY);
            ChatUtil.sendNotice(player, "You are no longer influenced by the Pixie Dust.");

            final String playerName = player.getName();

            players.add(playerName);

            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {

                    players.remove(playerName);
                }
            }, 20 * 30);
        }
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
                        if (player.getAllowFlight()) {
                            ChatUtil.sendNotice(player, "The power of the bucket fades.");
                        }
                        player.setAllowFlight(false);
                        antiCheat.unexempt(player, CheckType.FLY);
                    }
                }
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        InventoryType type = event.getInventory().getType();
        InventoryAction action = event.getAction();

        if (type.equals(InventoryType.ANVIL)) {
            if (action.equals(InventoryAction.NOTHING)) return;

            int rawSlot = event.getRawSlot();

            if (rawSlot < 2) {
                if (InventoryUtil.getPlaceActions().contains(action) && ItemUtil.isNamed(cursorItem)) {
                    boolean isCustomItem = ItemUtil.isAuthenticCustomItem(cursorItem.getItemMeta().getDisplayName());

                    if (!isCustomItem) return;

                    event.setResult(Event.Result.DENY);
                    ChatUtil.sendError(player, "You cannot place that here.");
                }
            } else if (rawSlot == 2) {
                if (InventoryUtil.getPickUpActions().contains(action) && ItemUtil.isNamed(currentItem)) {
                    boolean isCustomItem = ItemUtil.isAuthenticCustomItem(currentItem.getItemMeta().getDisplayName());

                    if (!isCustomItem) return;

                    event.setResult(Event.Result.DENY);
                    ChatUtil.sendError(player, "You cannot name this item that name.");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getType().equals(InventoryType.ANVIL)) {

            for (int i : event.getRawSlots()) {
                if (i + 1 <= event.getInventory().getSize()) {
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }
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
            if (player.getAllowFlight()) {
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            player.setAllowFlight(false);
            antiCheat.unexempt(player, CheckType.FLY);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        ItemStack[] drops = event.getDrops().toArray(new ItemStack[event.getDrops().size()]);

        if (ItemUtil.findItemOfName(drops, ChatColor.DARK_PURPLE + "Magic Bucket")) {
            if (player.getAllowFlight()) {
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            player.setAllowFlight(false);
            antiCheat.unexempt(player, CheckType.FLY);
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

        boolean hasCrown = ItemUtil.matchesFilter(player.getInventory().getHelmet(), ChatColor.GOLD + "Ancient Crown");

        int exp = event.getAmount();
        if (hasCrown) {
            exp *= 2;
        }

        if (ItemUtil.hasAncientArmour(player)) {
            ItemStack[] armour = player.getInventory().getArmorContents();
            ItemStack is = armour[ChanceUtil.getRandom(armour.length) - 1];
            if (exp > is.getDurability()) {
                exp -= is.getDurability();
                is.setDurability((short) 0);
            } else {
                is.setDurability((short) (is.getDurability() - exp));
                exp = 0;
            }
            player.getInventory().setArmorContents(armour);
            event.setAmount(exp);
        } else if (hasCrown) {
            event.setAmount(exp);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItem().getItemStack();

        if (itemStack.getTypeId() == ItemID.GOLD_BAR || itemStack.getTypeId() == ItemID.GOLD_NUGGET) {

            ItemStack[] inventoryContents = player.getInventory().getContents();
            ItemStack[] armorContents = player.getInventory().getArmorContents();
            if (!(ItemUtil.findItemOfName(inventoryContents, ChatColor.AQUA + "Imbued Crystal")
                    || ItemUtil.findItemOfName(armorContents, ChatColor.GOLD + "Ancient Crown"))) {
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
