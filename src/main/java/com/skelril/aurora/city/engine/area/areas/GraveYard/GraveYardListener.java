/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.GraveYard;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.city.engine.area.AreaListener;
import com.skelril.aurora.events.PlayerSacrificeItemEvent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.events.apocalypse.GemOfLifeUsageEvent;
import com.skelril.aurora.events.custom.item.HymnSingEvent;
import com.skelril.aurora.events.environment.CreepSpeakEvent;
import com.skelril.aurora.util.*;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.skelril.aurora.util.restoration.BlockRecord;
import com.skelril.aurora.util.restoration.RestorationUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GraveYardListener extends AreaListener<GraveYardArea> {
    protected final CommandBook inst = CommandBook.inst();
    protected final Logger log = inst.getLogger();
    protected final Server server = CommandBook.server();

    public GraveYardListener(GraveYardArea parent) {
        super(parent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {
        if (parent.contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {
        if (parent.isHostileTempleArea(event.getPlayer().getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onThunderChange(ThunderChangeEvent event) {
        if (!event.getWorld().equals(parent.getWorld())) return;
        if (event.toThunderState()) {
            parent.resetPressurePlateLock();
            parent.isPressurePlateLocked = !parent.checkPressurePlateLock();
            parent.resetRewardChest();
            List<Player> returnedList = new ArrayList<>();
            for (Player player : server.getOnlinePlayers()) {
                if (player.isValid() && LocationUtil.isInRegion(parent.getWorld(), parent.rewards, player)) returnedList.add(player);
            }
            for (Player player : returnedList) {
                ChatUtil.sendNotice(player, ChatColor.DARK_RED + "You dare disturb our graves!");
                ChatUtil.sendNotice(player, ChatColor.DARK_RED + "Taste the wrath of thousands!");
                for (int i = 0; i < 15; i++) {
                    parent.localSpawn(player, true);
                }
            }
        } else {
            ChatUtil.sendNotice(parent.getContained(Player.class), ChatColor.DARK_RED, "Rawwwgggggghhhhhhhhhh......");
            for (Entity entity : parent.getContained(Zombie.class)) {
                if (!ChanceUtil.getChance(5)) ((Zombie) entity).setHealth(0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent event) {

        World world = event.getWorld();
        if (parent.getWorld().equals(world) && world.isThundering()) {
            for (Location headStone : parent.headStones) {
                if (world.getEntitiesByClass(Zombie.class).size() > 1000) return;
                if (ChanceUtil.getChance(18)) {
                    for (int i = 0; i < ChanceUtil.getRangedRandom(3, 6); i++) {
                        parent.spawnAndArm(headStone, EntityType.ZOMBIE, true);
                    }
                }
            }
        }
    }

    private static Set<PotionEffectType> excludedTypes = new HashSet<>();

    static {
        excludedTypes.add(PotionEffectType.SLOW);
        excludedTypes.add(PotionEffectType.POISON);
        excludedTypes.add(PotionEffectType.WEAKNESS);
        excludedTypes.add(PotionEffectType.REGENERATION);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity aDefender = event.getEntity();
        Entity aAttacker = event.getDamager();
        if (!(aDefender instanceof LivingEntity)) return;
        if (parent.isHostileTempleArea(event.getEntity().getLocation())) {
            double damage = event.getDamage();
            LivingEntity defender = (LivingEntity) aDefender;
            if (ItemUtil.hasAncientArmour(defender) && !(parent.getWorld().isThundering() && defender instanceof Player)) {
                double diff = defender.getMaxHealth() - defender.getHealth();
                if (ChanceUtil.getChance((int) Math.max(3, Math.round(defender.getMaxHealth() - diff)))) {
                    EffectUtil.Ancient.powerBurst(defender, damage);
                }
            }
            if (aAttacker instanceof Player) {
                Player player = (Player) aAttacker;
                player.getActivePotionEffects().stream().filter(effect -> !excludedTypes.contains(effect.getType())).forEach(defender::addPotionEffect);
                if (parent.getWorld().isThundering()) return;
                if (ItemUtil.isHoldingItem(player, ItemUtil.CustomItems.MASTER_SWORD)) {
                    if (ChanceUtil.getChance(10)) {
                        EffectUtil.Master.healingLight(player, defender);
                    }
                    if (ChanceUtil.getChance(18)) {
                        List<LivingEntity> entities = player.getNearbyEntities(6, 4, 6).stream().filter(EnvironmentUtil::isHostileEntity).map(e -> (LivingEntity) e).collect(Collectors.toList());
                        EffectUtil.Master.doomBlade(player, entities);
                    }
                }
            } else if (defender instanceof Player) {
                Player player = (Player) defender;
                Iterator<PotionEffect> potionIt = player.getActivePotionEffects().iterator();
                while (potionIt.hasNext()) {
                    potionIt.next();
                    if (ChanceUtil.getChance(18)) {
                        potionIt.remove();
                    }
                }
                if (ItemUtil.findItemOfName(player.getInventory().getContents(), ItemUtil.CustomItems.PHANTOM_HYMN.toString())) {
                    event.setDamage(event.getDamage() * 1.5);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        event.getAffectedEntities().stream().filter(entity -> entity != null && entity instanceof Player && ChanceUtil.getChance(14)).forEach(entity -> {
            if (ChanceUtil.getChance(14)) {
                entity.removePotionEffect(PotionEffectType.REGENERATION);
            }
            if (ChanceUtil.getChance(14)) {
                entity.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            }
        });
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSacrifice(PlayerSacrificeItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemStack();
        Location origin = event.getBlock().getLocation();
        boolean isInRewardsRoom = LocationUtil.isInRegion(parent.getWorld(), parent.rewards, origin);
        int c;
        int o = 1;
        int m = item.getType().getMaxDurability();
        ItemStack[] i;
        if (ItemUtil.isItem(item, ItemUtil.CustomItems.PHANTOM_GOLD)) {
            int amount = 50;
            if (isInRewardsRoom) {
                amount = 100;
            }
            parent.economy.depositPlayer(player.getName(), amount * item.getAmount());
            event.setItemStack(null);
        } else if (ItemUtil.isItem(item, ItemUtil.CustomItems.FEAR_SWORD) || ItemUtil.isItem(item, ItemUtil.CustomItems.FEAR_BOW)) {
            if (!isInRewardsRoom) {
                o = 2;
            }
            c = ItemUtil.countItemsOfName(player.getInventory().getContents(), ItemUtil.CustomItems.GEM_OF_DARKNESS.toString());
            i = ItemUtil.removeItemOfName(player.getInventory().getContents(), ItemUtil.CustomItems.GEM_OF_DARKNESS.toString());
            player.getInventory().setContents(i);
            while (item.getDurability() > 0 && c >= o) {
                item.setDurability((short) Math.max(0, item.getDurability() - (m / 9)));
                c -= o;
            }
            player.getInventory().addItem(item);
            int amount = Math.min(c, 64);
            while (amount > 0) {
                player.getInventory().addItem(ItemUtil.Misc.gemOfDarkness(amount));
                c -= amount;
                amount = Math.min(c, 64);
            }
            player.updateInventory();
            event.setItemStack(null);
        } else if (ItemUtil.isItem(item, ItemUtil.CustomItems.UNLEASHED_SWORD) || ItemUtil.isItem(item, ItemUtil.CustomItems.UNLEASHED_BOW)) {
            if (!isInRewardsRoom) {
                o = 2;
            }
            c = ItemUtil.countItemsOfName(player.getInventory().getContents(), ItemUtil.CustomItems.IMBUED_CRYSTAL.toString());
            i = ItemUtil.removeItemOfName(player.getInventory().getContents(), ItemUtil.CustomItems.IMBUED_CRYSTAL.toString());
            player.getInventory().setContents(i);
            while (item.getDurability() > 0 && c >= o) {
                item.setDurability((short) Math.max(0, item.getDurability() - (m / 9)));
                c -= o;
            }
            player.getInventory().addItem(item);
            int amount = Math.min(c, 64);
            while (amount > 0) {
                player.getInventory().addItem(ItemUtil.Misc.imbuedCrystal(amount));
                c -= amount;
                amount = Math.min(c, 64);
            }
            event.setItemStack(null);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        World world = parent.getWorld();
        LivingEntity entity = event.getEntity();
        List<ItemStack> drops = event.getDrops();
        if (entity.getCustomName() != null) {
            String customName = entity.getCustomName();
            if (customName.equals("Grave Zombie")) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();
                    if (stack != null && stack.getTypeId() == ItemID.ROTTEN_FLESH) it.remove();
                }
                if (ChanceUtil.getChance(15000)) {
                    drops.add(ItemUtil.Misc.phantomClock(ChanceUtil.getRandom(3)));
                }
                if (ChanceUtil.getChance(10000)) {
                    drops.add(ItemUtil.Misc.imbuedCrystal(1));
                }
                if (ChanceUtil.getChance(6000) || world.isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.Misc.batBow());
                }
                if (ChanceUtil.getChance(6000) || world.isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.Misc.gemOfDarkness(1));
                }
                if (ChanceUtil.getChance(6000) || world.isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.Misc.gemOfLife(1));
                }
                if (ChanceUtil.getChance(400)) {
                    drops.add(ItemUtil.Misc.phantomGold(ChanceUtil.getRandom(3)));
                }
                if (ChanceUtil.getChance(1000000)) {
                    switch (ChanceUtil.getRandom(4)) {
                        case 1:
                            drops.add(ItemUtil.Fear.makeSword());
                            break;
                        case 2:
                            drops.add(ItemUtil.Fear.makeBow());
                            break;
                        case 3:
                            drops.add(ItemUtil.Unleashed.makeSword());
                            break;
                        case 4:
                            drops.add(ItemUtil.Unleashed.makeBow());
                            break;
                    }
                }
            } else if (customName.equals("Guardian Zombie")) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();

                    if (stack != null && stack.getTypeId() == ItemID.ROTTEN_FLESH) it.remove();
                }
                if (ChanceUtil.getChance(60)) {
                    drops.add(ItemUtil.CPotion.divineCombatPotion());
                } else if (ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.CPotion.holyCombatPotion());
                } else if (ChanceUtil.getChance(20)) {
                    drops.add(ItemUtil.CPotion.extremeCombatPotion());
                }
                if (ChanceUtil.getChance(250)) {
                    drops.add(ItemUtil.Misc.phantomClock(ChanceUtil.getRandom(3)));
                }
                if (ChanceUtil.getChance(100)) {
                    drops.add(ItemUtil.Misc.imbuedCrystal(1));
                }
                if (ChanceUtil.getChance(60) || world.isThundering() && ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.Misc.batBow());
                }
                if (ChanceUtil.getChance(60) || world.isThundering() && ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.Misc.gemOfDarkness(1));
                }
                if (ChanceUtil.getChance(60) || world.isThundering() && ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.Misc.gemOfLife(1));
                }
                if (ChanceUtil.getChance(20)) {
                    drops.add(ItemUtil.Misc.phantomGold(1));
                }
                if (ChanceUtil.getChance(8000)) {
                    switch (ChanceUtil.getRandom(4)) {
                        case 1:
                            drops.add(ItemUtil.Fear.makeSword());
                            break;
                        case 2:
                            drops.add(ItemUtil.Fear.makeBow());
                            break;
                        case 3:
                            drops.add(ItemUtil.Unleashed.makeSword());
                            break;
                        case 4:
                            drops.add(ItemUtil.Unleashed.makeBow());
                            break;
                    }
                }
            }
        } else if (parent.contains(entity)) {
            if (entity instanceof CaveSpider) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();
                    if (stack != null && !ChanceUtil.getChance(15)) {
                        if (stack.getTypeId() == ItemID.STRING) it.remove();
                        if (stack.getTypeId() == ItemID.SPIDER_EYE) it.remove();
                    }
                }
            } else if (entity instanceof Creeper) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();
                    if (stack != null && !ChanceUtil.getChance(15)) {
                        if (stack.getTypeId() == ItemID.SULPHUR) it.remove();
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        int fromType = event.getSource().getTypeId();
        if (fromType == BlockID.GRASS && parent.contains(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSheepEatGrass(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Sheep && parent.contains(entity)) {
            int type = event.getBlock().getTypeId();
            if (type == BlockID.GRASS || EnvironmentUtil.isShrubBlock(type)) {
                event.setCancelled(true);
                Location loc = entity.getLocation();
                entity.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 4, false, false);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BaseBlock baseBlock = new BaseBlock(block.getTypeId(), block.getData());
        if (parent.contains(block) && !parent.admin.isAdmin(event.getPlayer())) {
            event.setCancelled(true);
            if (!parent.accept(baseBlock, GraveYardArea.breakable)) {
                return;
            }
            parent.generalIndex.addItem(new BlockRecord(block));
            block.setTypeId(0);
            RestorationUtil.handleToolDamage(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (parent.contains(event.getBlock()) && !parent.admin.isAdmin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        Block block = event.getBlock();
        Location contactedLoc = block.getLocation();
        if (parent.isHostileTempleArea(contactedLoc)) {
            if (block.getTypeId() == BlockID.STONE_PRESSURE_PLATE) {
                if (contactedLoc.getBlockY() < 57) {
                    EntityUtil.heal(event.getEntity(), 1);
                } else if (parent.isPressurePlateLocked) {
                    DeathUtil.throwSlashPotion(contactedLoc);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        server.getScheduler().runTaskLater(inst, () -> {
            if (parent.isHostileTempleArea(player.getLocation()) && !parent.admin.isAdmin(player)) {
                player.teleport(parent.headStones.get(ChanceUtil.getRandom(parent.headStones.size()) - 1));
                ChatUtil.sendWarning(player, "You feel dazed and confused as you wake up near a head stone.");
            }
        }, 1);
    }

    private static Set<PlayerTeleportEvent.TeleportCause> watchedCauses = new HashSet<>();

    static {
        watchedCauses.add(PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        watchedCauses.add(PlayerTeleportEvent.TeleportCause.COMMAND);
        watchedCauses.add(PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (parent.isHostileTempleArea(event.getFrom()) && event.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) {
            Location tg = parent.headStones.get(ChanceUtil.getRandom(parent.headStones.size()) - 1);
            tg = LocationUtil.findFreePosition(tg);
            if (tg == null) tg = parent.getWorld().getSpawnLocation();
            event.setTo(tg);
            event.useTravelAgent(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (parent.isHostileTempleArea(event.getTo()) && !parent.admin.isSysop(player)) {
            if (!watchedCauses.contains(event.getCause())) return;
            if (parent.contains(event.getFrom())) {
                event.setCancelled(true);
            } else {
                Location tg = parent.headStones.get(ChanceUtil.getRandom(parent.headStones.size()) - 1);
                tg = LocationUtil.findFreePosition(tg);
                if (tg == null) tg = parent.getWorld().getSpawnLocation();
                event.setTo(tg);
            }
            ChatUtil.sendWarning(event.getPlayer(), "It would seem your teleport has failed to penetrate the temple.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Location clickedLoc = block.getLocation();
        ItemStack stack = player.getItemInHand();
        Action action = event.getAction();
        if (parent.isHostileTempleArea(clickedLoc)) {
            switch (block.getTypeId()) {
                case BlockID.LEVER:
                    server.getScheduler().runTaskLater(inst, () -> {
                        parent.isPressurePlateLocked = !parent.checkPressurePlateLock();
                    }, 1);
                    break;
                case BlockID.STONE_PRESSURE_PLATE:
                    if ((parent.isPressurePlateLocked || clickedLoc.getBlockY() < 57) && action.equals(Action.PHYSICAL)) {
                        DeathUtil.throwSlashPotion(clickedLoc);
                    }
                    break;
            }
        }
        switch (action) {
            case RIGHT_CLICK_BLOCK:
                if (ItemUtil.isItem(stack, ItemUtil.CustomItems.PHANTOM_CLOCK)) {
                    player.teleport(new Location(parent.getWorld(), -126, 42, -685), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    final int amt = stack.getAmount() - 1;
                    server.getScheduler().runTaskLater(inst, () -> {
                        ItemStack newStack = null;
                        if (amt > 0) {
                            newStack = ItemUtil.Misc.phantomClock(amt);
                        }
                        player.setItemInHand(newStack);
                    }, 1);
                }
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        Player player = event.getPlayer();
        if (event.getHymn().equals(HymnSingEvent.Hymn.PHANTOM)) {
            if (LocationUtil.isInRegion(parent.getWorld(), parent.creepers, player)) {
                ChatUtil.sendNotice(player, "A spirit carries you through the maze!");
                player.teleport(new Location(parent.getWorld(), -162.5, 52, -704), PlayerTeleportEvent.TeleportCause.UNKNOWN);
            } else if (LocationUtil.isInRegion(parent.getWorld(), parent.rewards, player) && !parent.getWorld().isThundering()) {
                ChatUtil.sendNotice(player, "A monstrous thunderstorm begins!");
                parent.getWorld().setThundering(true);
            }
        }
    }

    private static final String GEM_OF_LIFE = ChatColor.DARK_AQUA + "Gem of Life";

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        HashMap<String, PlayerState> playerState = parent.playerState;
        Player player = event.getEntity();
        boolean contained = parent.contains(player);
        if (contained || player.getWorld().isThundering()) {
            List<ItemStack> drops = event.getDrops();
            ItemStack[] dropArray = ItemUtil.clone(drops.toArray(new ItemStack[drops.size()]));
            if (ItemUtil.findItemOfName(dropArray, GEM_OF_LIFE)) {
                if (!playerState.containsKey(player.getName())) {
                    GemOfLifeUsageEvent aEvent = new GemOfLifeUsageEvent(player);
                    server.getPluginManager().callEvent(aEvent);
                    if (!aEvent.isCancelled()) {
                        playerState.put(player.getName(), new PlayerState(player.getName(),
                                player.getInventory().getContents(),
                                player.getInventory().getArmorContents(),
                                player.getLevel(),
                                player.getExp()));
                        if (contained) {
                            dropArray = null;
                        } else {
                            drops.clear();
                            return;
                        }
                    }
                }
            }
            // Leave admin mode deaths out of this
            if (!contained || parent.admin.isAdmin(player)) return;
            parent.makeGrave(player.getName(), dropArray);
            drops.clear();
            event.setDeathMessage(ChatColor.DARK_RED + "RIP ~ " + player.getDisplayName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        HashMap<String, PlayerState> playerState = parent.playerState;
        Player player = event.getPlayer();
        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !parent.admin.isAdmin(player)) {
            try {
                PlayerState identity = playerState.get(player.getName());
                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                // Count then remove the Gems of Life
                int c = ItemUtil.countItemsOfName(player.getInventory().getContents(), GEM_OF_LIFE) - 1;
                ItemStack[] newInv = ItemUtil.removeItemOfName(player.getInventory().getContents(), GEM_OF_LIFE);
                player.getInventory().setContents(newInv);
                // Add back the gems of life as needed
                int amount = Math.min(c, 64);
                while (amount > 0) {
                    player.getInventory().addItem(ItemUtil.Misc.gemOfLife(amount));
                    c -= amount;
                    amount = Math.min(c, 64);
                }
                //noinspection deprecation
                player.updateInventory();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playerState.remove(player.getName());
            }
        }
    }
}