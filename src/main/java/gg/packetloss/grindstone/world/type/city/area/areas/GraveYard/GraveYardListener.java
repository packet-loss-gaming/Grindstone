/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.GraveYard;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import gg.packetloss.grindstone.events.PlayerDeathDropRedirectEvent;
import gg.packetloss.grindstone.events.PlayerGraveProtectItemsEvent;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.events.PrayerTriggerEvent;
import gg.packetloss.grindstone.events.apocalypse.*;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.events.custom.item.RepairItemEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.environment.CreepSpeakEvent;
import gg.packetloss.grindstone.events.guild.GuildPowerUseEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePopEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.restoration.RestorationUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.EnvironmentUtil.hasThunderstorm;

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
    public void onPrayerApplication(PrayerTriggerEvent event) {
        if (parent.isHostileTempleArea(event.getPlayer().getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSpecialAttack(SpecialAttackEvent event) {
        Player player = event.getPlayer();
        if (!parent.isHostileTempleArea(player.getLocation())) {
            return;
        }

        if (ItemUtil.isInItemFamily(event.getSpec().getUsedItem(), ItemFamily.MASTER)) {
            event.setContextCooldown(event.getContext().getDelay() / 2);
        }
    }

    @EventHandler
    public void onApocalypsePreSpawn(ApocalypsePreSpawnEvent event) {
        World graveWorld = parent.getWorld();
        if (graveWorld != event.getInitialLightningStrikePoint().getWorld()) {
            return;
        }

        for (BlockVector3 headStone : parent.headStones) {
            if (!ChanceUtil.getChance(parent.getConfig().apocalypseChanceOfGraveSpawn)) {
                continue;
            }

            if (!LocationUtil.isChunkLoadedAt(graveWorld, headStone)) {
                continue;
            }

            event.getLightningStrikePoints().add(WorldEditBridge.toLocation(graveWorld, headStone));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseLightningSpawn(ApocalypseLightningStrikeSpawnEvent event) {
        if (!parent.contains(event.getLocation())) {
            return;
        }

        // Don't mess with strikes that weren't added/at a grave site
        if (event.getLocation().equals(event.getTriggeringLocation())) {
            return;
        }

        GraveYardConfig config = parent.getConfig();
        event.setNumberOfZombies(ChanceUtil.getRangedRandom(
                config.apocalypseNumZombiesMin,
                config.apocalypseNumZombiesMax
        ));
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypsePurge(ApocalypsePurgeEvent event) {
        Iterator<Zombie> it = event.getZombies().iterator();
        while (it.hasNext()) {
            Location entityLoc = it.next().getLocation();
            if (!parent.contains(entityLoc)) {
                continue;
            }

            // Don't kill all the zombies in the grave yard
            if (!ChanceUtil.getChance(parent.getConfig().apocalypseChanceOfPurge)) {
                it.remove();
                continue;
            }

            // Only kill zombies not in the dungeon
            if (parent.isHostileTempleArea(entityLoc)) {
                it.remove();
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

    private static EDBEExtractor<LivingEntity, LivingEntity, Projectile> extractor = new EDBEExtractor<>(
            LivingEntity.class,
            LivingEntity.class,
            Projectile.class
    );

    private void degradeGoodPotions(Player player) {
        List<PotionEffectType> affectedTypes = Arrays.asList(
                PotionEffectType.INCREASE_DAMAGE, PotionEffectType.REGENERATION, PotionEffectType.DAMAGE_RESISTANCE,
                PotionEffectType.WATER_BREATHING, PotionEffectType.FIRE_RESISTANCE
        );

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!affectedTypes.contains(effect.getType())) {
                continue;
            }

            int newDuration = (int) (effect.getDuration() * .9);
            if (newDuration == 0) {
                player.removePotionEffect(effect.getType());
                continue;
            }

            PotionEffect newEffect = new PotionEffect(
                    effect.getType(),
                    newDuration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles()
            );
            player.addPotionEffect(newEffect, true);
        }
    }

    private boolean shouldActivateAncientArmor(LivingEntity entity, DamageCause cause) {
        if (entity instanceof Player && !hasThunderstorm(parent.getWorld())) {
            return false;
        }

        if (!ItemUtil.hasAncientArmour(entity)) {
            return false;
        }

        if (SpecialAttackFactory.getCurrentSpecialAttack().isPresent()) {
            return false;
        }

        return cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.PROJECTILE;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        CombatantPair<LivingEntity, LivingEntity, Projectile> result = extractor.extractFrom(event);

        if (result == null) return;
        LivingEntity defender = result.getDefender();
        LivingEntity attacker = result.getAttacker();
        if (parent.isHostileTempleArea(event.getEntity().getLocation())) {
            double damage = event.getDamage();

            if (shouldActivateAncientArmor(defender, event.getCause())) {
                double diff = defender.getMaxHealth() - defender.getHealth();
                if (ChanceUtil.getChance((int) Math.max(3, Math.round(defender.getMaxHealth() - diff)))) {
                    EffectUtil.Ancient.powerBurst(defender, damage);
                }
            }

            if (attacker instanceof Player) {
                Player player = (Player) attacker;
                player.getActivePotionEffects().stream().filter(effect -> !excludedTypes.contains(effect.getType())).forEach(defender::addPotionEffect);

                if (parent.isHotTorchArea(player.getLocation())) {
                    EntityUtil.forceDamage(player, ChanceUtil.getRandom(5));
                }
            } else if (defender instanceof Player) {
                Location defenderLoc = defender.getLocation();
                if (parent.isRewardsArea(defenderLoc)) {
                    event.setDamage(event.getDamage() + (parent.rewardsRoomOccupiedTicks / 3.0));

                    degradeGoodPotions((Player) defender);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuildPowerUse(GuildPowerUseEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(parent.parkour, player)) {
            event.setCancelled(true);
        } else if (parent.isTorchArea(player.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        event.getAffectedEntities().stream().filter(entity -> entity instanceof Player && ChanceUtil.getChance(14)).forEach(entity -> {
            if (ChanceUtil.getChance(14)) {
                entity.removePotionEffect(PotionEffectType.REGENERATION);
            }
            if (ChanceUtil.getChance(14)) {
                entity.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            }
        });
    }

    private int getPhantomValueOfItem(ItemStack item, Location pointOfSacrifice) {
        if (ItemUtil.isItem(item, CustomItems.PHANTOM_GOLD)) {
            return parent.isRewardsArea(pointOfSacrifice) ? 100 : 50;
        }
        if (ItemUtil.isItem(item, CustomItems.PHANTOM_DIAMOND)) {
            return parent.isRewardsArea(pointOfSacrifice) ? 18000 : 15000;
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSacrifice(PlayerSacrificeItemEvent event) {
        ItemStack item = event.getItemStack();

        int phantomValue = getPhantomValueOfItem(item, event.getPointOfSacrifice());
        if (phantomValue > 0) {
            Player player = event.getPlayer();

            int totalAmount = phantomValue * item.getAmount();
            parent.economy.depositPlayer(player, totalAmount);

            // Delay by a tick to prevent this message from appearing before the ancient fire ignites message
            CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                ChatUtil.sendNotice(
                        player,
                        "You receive " + ChatUtil.makeCountString(parent.economy.format(totalAmount), ".")
                );
            }, 1);

            event.setItemStack(null);
        } else if (ItemUtil.isItem(item, CustomItems.PHANTOM_HYMN)) {
            Player player = event.getPlayer();

            for (int i = 0; i < item.getAmount(); ++i) {
                player.getInventory().addItem(CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE, 20));
            }

            event.setItemStack(null);
        }
    }

    @EventHandler
    public void onItemRepair(RepairItemEvent event) {
        if (parent.isRewardsArea(event.getPlayer().getLocation())) {
            event.setRepairPercentage(Math.min(1, event.getRepairPercentage() * 2));
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
                drops.removeIf(stack -> stack != null && stack.getType() == Material.ROTTEN_FLESH);

                if (ChanceUtil.getChance(15000)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_CLOCK, ChanceUtil.getRandom(3)));
                }
                if (ChanceUtil.getChance(1400)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_POTION));
                }
                if (ChanceUtil.getChance(700)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE));
                }
                if (ChanceUtil.getChance(325)) {
                    drops.add(CustomItemCenter.build(CustomItems.IMBUED_CRYSTAL));
                }
                if (ChanceUtil.getChance(325)) {
                    drops.add(CustomItemCenter.build(CustomItems.BAT_BOW));
                }
                if (ChanceUtil.getChance(325)) {
                    drops.add(CustomItemCenter.build(CustomItems.GEM_OF_DARKNESS));
                }
                if (ChanceUtil.getChance(325)) {
                    drops.add(CustomItemCenter.build(CustomItems.GEM_OF_LIFE));
                }
                if (ChanceUtil.getChance(150)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_GOLD, ChanceUtil.getRandom(3)));
                }
                if (ChanceUtil.getChance(1000000)) {
                    switch (ChanceUtil.getRandom(4)) {
                        case 1:
                            drops.add(CustomItemCenter.build(CustomItems.FEAR_SWORD));
                            break;
                        case 2:
                            drops.add(CustomItemCenter.build(CustomItems.FEAR_BOW));
                            break;
                        case 3:
                            drops.add(CustomItemCenter.build(CustomItems.UNLEASHED_SWORD));
                            break;
                        case 4:
                            drops.add(CustomItemCenter.build(CustomItems.UNLEASHED_BOW));
                            break;
                    }
                }
            } else if (customName.equals("Guardian Zombie")) {
                drops.removeIf(stack -> stack != null && stack.getType() == Material.ROTTEN_FLESH);

                if (ChanceUtil.getChance(500)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_CLOCK));
                }
                if (ChanceUtil.getChance(90)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_POTION));
                }
                if (ChanceUtil.getChance(80)) {
                    drops.add(CustomItemCenter.build(CustomItems.BARBARIAN_BONE, ChanceUtil.getRandom(8)));
                }
                if (ChanceUtil.getChance(45)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE));
                }
                if (ChanceUtil.getChance(25)) {
                    drops.add(CustomItemCenter.build(CustomItems.IMBUED_CRYSTAL));
                }
                if (ChanceUtil.getChance(25)) {
                    drops.add(CustomItemCenter.build(CustomItems.GEM_OF_DARKNESS));
                }
                if (ChanceUtil.getChance(25)) {
                    drops.add(CustomItemCenter.build(CustomItems.GEM_OF_LIFE));
                }
                if (ChanceUtil.getChance(15)) {
                    drops.add(CustomItemCenter.build(CustomItems.PHANTOM_GOLD, ChanceUtil.getRandom(8)));
                }
                if (ChanceUtil.getChance(8000)) {
                    switch (ChanceUtil.getRandom(4)) {
                        case 1:
                            drops.add(CustomItemCenter.build(CustomItems.FEAR_SWORD));
                            break;
                        case 2:
                            drops.add(CustomItemCenter.build(CustomItems.FEAR_BOW));
                            break;
                        case 3:
                            drops.add(CustomItemCenter.build(CustomItems.UNLEASHED_SWORD));
                            break;
                        case 4:
                            drops.add(CustomItemCenter.build(CustomItems.UNLEASHED_BOW));
                            break;
                    }
                }
                event.setDroppedExp(event.getDroppedExp() * 5);
            }
        } else if (parent.contains(entity)) {
            if (entity instanceof CaveSpider) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();
                    if (stack != null && !ChanceUtil.getChance(15)) {
                        if (stack.getType() == Material.STRING) it.remove();
                        if (stack.getType() == Material.SPIDER_EYE) it.remove();
                    }
                }
            } else if (entity instanceof Creeper) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();
                    if (stack != null && !ChanceUtil.getChance(15)) {
                        if (stack.getType() == Material.GUNPOWDER) it.remove();
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        Material fromType = event.getSource().getType();
        if (fromType == Material.GRASS && parent.contains(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSheepEatGrass(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Sheep && parent.contains(entity)) {
            event.setCancelled(true);
            Location loc = entity.getLocation();
            ExplosionStateFactory.createExplosion(loc, 4, false, false);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (parent.contains(block) && player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);

            if (!GraveYardArea.BREAKABLE.contains(block.getType())) {
                return;
            }

            try {
                parent.blockState.pushAnonymousBlock(BlockStateKind.GRAVEYARD, block.getState());

                block.setType(Material.AIR);
                RestorationUtil.handleToolDamage(event.getPlayer());
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onApocalypseBlockDamage(ApocalypseBlockDamagePreventionEvent event) {
        if (parent.contains(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (parent.contains(block) && player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        Block block = event.getBlock();
        Location contactedLoc = block.getLocation();
        if (parent.isHostileTempleArea(contactedLoc)) {
            if (block.getType() == Material.STONE_PRESSURE_PLATE) {
                // Workaround for: https://github.com/PaperMC/Paper/issues/4962
                if (event.getEntityType() == EntityType.BAT) {
                    return;
                }

                if (parent.isPressurePlateLocked || (contactedLoc.getBlockY() < 57 && ChanceUtil.getChance(5))) {
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
                player.teleport(parent.getRandomHeadstoneOrSpawn());
                ChatUtil.sendWarning(player, "You feel dazed and confused as you wake up near a head stone.");
            }
        }, 1);
    }

    private static Set<PlayerTeleportEvent.TeleportCause> watchedCauses = new HashSet<>();

    static {
        watchedCauses.add(PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        watchedCauses.add(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
        watchedCauses.add(PlayerTeleportEvent.TeleportCause.COMMAND);
        watchedCauses.add(PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (parent.isHostileTempleArea(event.getFrom()) && event.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) {
            event.setCancelled(true);
            event.getPlayer().teleport(parent.getRandomHeadstoneOrSpawn(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (parent.isHostileTempleArea(event.getTo()) && !parent.admin.isAdmin(player)) {
            if (!watchedCauses.contains(event.getCause())) return;
            if (parent.contains(event.getFrom())) {
                event.setCancelled(true);
            } else {
                event.setTo(parent.getRandomHeadstoneOrSpawn());
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

        Material blockType = block.getType();
        if (parent.isHostileTempleArea(clickedLoc)) {
            switch (block.getType()) {
                case LEVER:
                    server.getScheduler().runTaskLater(inst, () -> {
                        parent.isPressurePlateLocked = !parent.checkPressurePlateLock();
                    }, 1);
                    break;
                case STONE_PRESSURE_PLATE:
                    if ((parent.isPressurePlateLocked || clickedLoc.getBlockY() < 57) && action.equals(Action.PHYSICAL)) {
                        DeathUtil.throwSlashPotion(clickedLoc);
                    }
                    break;
            }
        }

        if (parent.contains(clickedLoc) && EnvironmentUtil.isContainer(blockType)) {
            boolean isSpectator = player.getGameMode() == GameMode.SPECTATOR;
            if (isSpectator) {
                // If the location is in the rewards room, and players are in the rewards room looting it,
                // allow players to see the loot.
                if (parent.isRewardsArea(clickedLoc) && !parent.getContainedParticipantsIn(parent.rewards).isEmpty()) {
                    return;
                }

                event.setUseInteractedBlock(Event.Result.DENY);
            } else {
                // If this is a grave, check if the player is allowed to open/loot this particular grave.
                BlockState blockState = block.getState();
                if (blockState instanceof Chest && !parent.isGraveOpenableBy((Chest) blockState, player)) {
                    event.setUseInteractedBlock(Event.Result.DENY);
                    ChatUtil.sendError(player, "This player died very recently! Show some respect!");
                }
            }
        }

        switch (action) {
            case RIGHT_CLICK_BLOCK:
                if (ItemUtil.isItem(stack, CustomItems.PHANTOM_CLOCK)) {
                    if (parent.getContainedParticipantsIn(parent.rewards).isEmpty()) {
                        player.teleport(new Location(parent.getWorld(), -126, 42, -685), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        final int amt = stack.getAmount() - 1;
                        server.getScheduler().runTaskLater(inst, () -> {
                            ItemStack newStack = null;
                            if (amt > 0) {
                                newStack = CustomItemCenter.build(CustomItems.PHANTOM_CLOCK, amt);
                            }
                            player.setItemInHand(newStack);
                        }, 1);
                    } else {
                        ChatUtil.sendError(player, "There are players already in the rewards room.");
                    }
                }
                break;
        }
    }

    private static final int REQUIRED_PHANTOM_ESSENCE = 10;

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        if (event.getHymn() != HymnSingEvent.Hymn.PHANTOM) {
            return;
        }

        Player player = event.getPlayer();
        if (!LocationUtil.isInRegion(parent.getWorld(), parent.creepers, player)) {
            return;
        }

        PlayerInventory pInv = player.getInventory();
        if (ItemUtil.countItemsOfName(pInv.getContents(), CustomItems.PHANTOM_ESSENCE.toString()) < REQUIRED_PHANTOM_ESSENCE) {
            ChatUtil.sendError(player, "You don't have enough phantom essence for spirit assistance.");
            return;
        }

        Location endOfMaze = new Location(parent.getWorld(), -162.5, 52, -704);
        if (player.getLocation().distanceSquared(endOfMaze) <= Math.pow(15, 2)) {
            ChatUtil.sendError(player, "You're too close to the end of the maze.");
            return;
        }

        boolean removed = ItemUtil.removeItemOfName(
                player,
                CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE),
                REQUIRED_PHANTOM_ESSENCE,
                false
        );
        Validate.isTrue(removed);

        ChatUtil.sendNotice(player, "A spirit carries you through the maze!");
        player.teleport(endOfMaze, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @EventHandler
    public void onPlayerDeathInGraveYard(PlayerDeathEvent event) { // Handles Grave Yard Specific Logic
        Player player = event.getEntity();
        Location playerLoc = player.getLocation();
        if (!parent.contains(playerLoc)) {
            return;
        }

        if (parent.isTorchArea(playerLoc)) {
            // Remove all zombies in the torch area if all players in the torch area are dead
            Collection<Entity> entities = parent.getContained(parent.torchArea, Player.class, Zombie.class);
            if (entities.stream().noneMatch((e) -> e instanceof Player)) {
                entities.forEach(Entity::remove);
            }
        }
    }

    /**
     * This is called to handle the default death logic given a previously called {@link PlayerGraveProtectItemsEvent}.
     *
     * This method will:
     * - Create a player state if a gem of life is used, and the {@link GemOfLifeUsageEvent} event isn't cancelled.
     * - Create a grave and call a {@link PlayerDeathDropRedirectEvent} if a gem of life isn't used,
     *   or a {@link GemOfLifeUsageEvent} was cancelled.
     *
     * @param event the respective grave protect items event
     *
     * @throws IOException upon failure to write the player state to disk for a gem of life
     * @throws ConflictingPlayerStateException upon failure to create a player state because of a state conflict
     */
    private void runDefaultDeathLogic(PlayerGraveProtectItemsEvent event) throws IOException, ConflictingPlayerStateException {
        Player player = event.getPlayer();

        if (event.isUsingGemOfLife()) {
            GemOfLifeUsageEvent aEvent = new GemOfLifeUsageEvent(player);
            server.getPluginManager().callEvent(aEvent);
            if (!aEvent.isCancelled()) {
                parent.playerState.pushState(PlayerStateKind.GRAVE_YARD, player);
                return;
            }
        }

        // Create final drops array
        ItemStack[] finalDrops = Arrays.stream(event.getDrops())
                .filter(Objects::nonNull)
                .map(ItemStack::clone)
                .toArray(ItemStack[]::new);

        Location graveLocation = parent.makeGrave(player.getName(), finalDrops);
        CommandBook.callEvent(new PlayerDeathDropRedirectEvent(player, graveLocation));
    }

    /**
     * This is called when the death is considered handled by the default death implementation
     * either via a gem of life, or a grave.
     *
     * @param event the respective death event
     */
    private void acceptDeathItemsHandled(PlayerDeathEvent event) {
        // Remove drops from ground, and drop no XP
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) { // Handles Death System
        Player player = event.getEntity();
        List<ItemStack> drops = event.getDrops();

        // Handle occasional double deaths better
        if (drops.isEmpty()) {
            return;
        }

        try {
            // Leave admin mode deaths out of this
            if (parent.admin.isAdmin(player) || parent.playerState.hasTempKind(player)) return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ItemStack[] dropsArray = ItemUtil.clone(drops.toArray(new ItemStack[0]));
        boolean useGemOfLife = player.hasPermission("aurora.tome.life") ||
                ItemUtil.findItemOfName(dropsArray, CustomItems.GEM_OF_LIFE.toString());

        PlayerGraveProtectItemsEvent protectItemsEvent = new PlayerGraveProtectItemsEvent(
                player,
                useGemOfLife,
                dropsArray
        );

        CommandBook.callEvent(protectItemsEvent);
        if (protectItemsEvent.isCancelled()) {
            return;
        }

        try {
            runDefaultDeathLogic(protectItemsEvent);
            acceptDeathItemsHandled(event);
        } catch (IOException | ConflictingPlayerStateException e) {
            e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();

            // Attempt to fallback to a player state protection, something likely went wrong with
            // grave generation.
            try {
                parent.playerState.pushState(PlayerStateKind.GRAVE_YARD, player);
                acceptDeathItemsHandled(event);
            } catch (IOException | ConflictingPlayerStateException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerStatePop(PlayerStatePopEvent event) {
        if (event.getKind() != PlayerStateKind.GRAVE_YARD) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("aurora.tome.life")) {
            return;
        }

        PlayerInventory pInv = player.getInventory();

        // Count then remove the Gems of Life
        int c = ItemUtil.countItemsOfName(pInv.getContents(), CustomItems.GEM_OF_LIFE.toString()) - 1;
        ItemStack[] newInv = ItemUtil.removeItemOfName(pInv.getContents(), CustomItems.GEM_OF_LIFE.toString());
        pInv.setContents(newInv);

        // Add back the gems of life as needed
        int amount = Math.min(c, 64);
        while (amount > 0) {
            player.getInventory().addItem(CustomItemCenter.build(CustomItems.GEM_OF_LIFE, amount));
            c -= amount;
            amount = Math.min(c, 64);
        }

        player.updateInventory();
    }

    @EventHandler
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (!GraveYardArea.GRAVE_YARD_SPECTATOR_KINDS.contains(event.getKind())) {
            return;
        }

        Player player = event.getPlayer();

        // Create a spawn point looking at something interesting.
        int minPlayersToAttemptRandomAssignment = parent.isParticipant(player) ? 2 : 1;
        List<Player> participants = parent.getContainedParticipants();

        Location pointOfInterest = RegionUtil.getCenterAt(parent.getWorld(), 92, parent.getRegion());
        Location targetLocation = LocationUtil.pickLocation(
                parent.getWorld(),
                pointOfInterest.getY() + 25,
                new RegionChecker(parent.getRegion())
        );

        if (participants.size() >= minPlayersToAttemptRandomAssignment) {
            while (true) {
                Player targetParticipant = CollectionUtil.getElement(participants);
                if (targetParticipant != player) {
                    pointOfInterest = targetParticipant.getLocation();
                    targetLocation = LocationUtil.findRandomLoc(
                            pointOfInterest,
                            10,
                            false,
                            false
                    ).add(0, 10, 0);
                    break;
                }
            }
        }

        targetLocation.setDirection(VectorUtil.createDirectionalVector(targetLocation, pointOfInterest));
        player.teleport(targetLocation, PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }
}
