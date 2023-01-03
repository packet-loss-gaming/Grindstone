/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.CursedMine;

import gg.packetloss.Pitfall.bukkit.event.PitfallTriggerEvent;
import gg.packetloss.grindstone.events.PlayerGraveProtectItemsEvent;
import gg.packetloss.grindstone.events.PrayerTriggerEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.checker.NonSolidRegionChecker;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.BookUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static gg.packetloss.grindstone.world.type.city.area.areas.CursedMine.CursedMineArea.AFFECTED_MATERIALS;
import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK;

public class CursedMineListener extends AreaListener<CursedMineArea> {
    public CursedMineListener(CursedMineArea parent) {
        super(parent);
    }

    private static final Set<Material> TRIGGER_BLOCKS = Set.of(
            Material.STONE_BUTTON, Material.TRIPWIRE
    );

    private static final Set<Action> TRIGGER_INTERACTIONS = Set.of(
            Action.PHYSICAL, Action.RIGHT_CLICK_BLOCK
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!TRIGGER_INTERACTIONS.contains(event.getAction())) {
            return;
        }

        Block block = event.getClickedBlock();
        assert block != null;

        if (parent.contains(block) && TRIGGER_BLOCKS.contains(block.getType())) {
            parent.lastActivation = System.currentTimeMillis();
        }
    }

    private boolean hasSilkTouch(ItemStack item) {
        return item.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    private boolean hasFortune(ItemStack item) {
        return item.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS);
    }

    private void getMiningResult(Player player, Material blockType, Consumer<ItemStack> result) {
        ItemStack itemInHand = player.getItemInHand();

        // Get the base drop
        ItemStack stack = EnvironmentUtil.getOreDrop(blockType, itemInHand);

        // Distribute item stacks
        int numClones = ChanceUtil.getRandomNTimes(8, 3);
        if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_CURSED_ORES)) {
            numClones *= 2;
        }

        for (int i = numClones; i > 0; --i) {
            result.accept(stack.clone());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (GeneralPlayerUtil.isInBuildMode(player) || !parent.contains(block)) {
            return;
        }

        ItemStack itemInHand = player.getItemInHand();

        if (EnvironmentUtil.isOre(block) && ItemUtil.isPickaxe(itemInHand)) {
            Material type = block.getType();

            getMiningResult(player, type, (item) -> {
                GeneralPlayerUtil.giveItemToPlayer(player, item);
            });

            event.setExpToDrop((70 - player.getLocation().getBlockY()) / 2);

            if (ChanceUtil.getChance(3000)) {
                ChatUtil.sendNotice(player, "You feel as though a spirit is trying to tell you something...");
                GeneralPlayerUtil.giveItemToPlayer(player, BookUtil.Lore.Areas.theGreatMine());
            }

            if (ChanceUtil.getChance(10000)) {
                ChatUtil.sendNotice(player, "You find a dusty old book...");
                GeneralPlayerUtil.giveItemToPlayer(player, CustomItemCenter.build(CustomItems.TOME_OF_CURSED_SMELTING));
            }

            parent.eatFood(player);
            parent.poison(player, 6);
            parent.ghost(player, type);

            try {
                parent.blockState.pushBlock(BlockStateKind.CURSED_MINE, player, block.getState());
                parent.restorationUtil.blockAndLogEvent(event);

                parent.highScores.update(player, ScoreTypes.CURSED_ORES_MINED, BigInteger.ONE);
                return;
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();
            }
        } else if (!parent.isOnHitList(player) && block.getType() == Material.FIRE) {
            return;
        }

        event.setCancelled(true);
        ChatUtil.sendWarning(player, "You cannot break this block for some reason.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!GeneralPlayerUtil.isInBuildMode(player) && parent.contains(event.getBlock())) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, ChatColor.DARK_RED, "You don't have permission for this area.");
        }
    }

    private boolean isTeleblocked(Player player) {
        if (parent.blockState.hasPlayerBrokenBlocks(BlockStateKind.CURSED_MINE, player)) {
            return true;
        }

        if (parent.isOnHitList(player)) {
            return true;
        }

        return false;
    }

    private static List<PlayerTeleportEvent.TeleportCause> ACCEPTED_TELEPORTS = List.of(
            PlayerTeleportEvent.TeleportCause.UNKNOWN
    );

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (isTeleblocked(player) && !ACCEPTED_TELEPORTS.contains(event.getCause())) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You have been tele-blocked!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (parent.contains(event.getBlockClicked())) {
            Player player = event.getPlayer();
            Block block = event.getBlockClicked();

            try {
                parent.blockState.pushBlock(BlockStateKind.CURSED_MINE, player, block.getState());
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();

                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SURVIVAL && parent.contains(event.getBlockClicked())) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, ChatColor.DARK_RED, "You don't have permission for this area.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerTriggerEvent event) {
        Player player = event.getPlayer();

        if (event.getPrayer().isHoly() && parent.contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSpecialAttack(SpecialAttackEvent event) {
        Player player = event.getPlayer();
        if (!parent.contains(player)) {
            return;
        }

        if (ItemUtil.isInItemFamily(event.getSpec().getUsedItem(), ItemFamily.MASTER)) {
            event.setContextCooldown(event.getContext().getDelay() / 2);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        parent.revertPlayerBlocks(event.getPlayer());
    }

    private void drainAllFromArray(ItemStack[] itemStacks) {
        for (Material type : AFFECTED_MATERIALS) {
            ItemUtil.removeItemOfType(itemStacks, type);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGraveProtectItemsEvent(PlayerGraveProtectItemsEvent event) {
        Player player = event.getPlayer();

        boolean isAffected = parent.contains(event.getDeathLocation()) || parent.isOnHitList(player);
        if (!isAffected) {
            return;
        }

        if (event.isUsingGemOfLife()) {
            ItemStack[] itemStacks = player.getInventory().getContents();
            drainAllFromArray(itemStacks);
            player.getInventory().setContents(itemStacks);
        } else {
            drainAllFromArray(event.getDrops());
        }
    }

    private static final List<String> DEATH_MESSAGES = List.of(
        " was killed by Dave",
        " got on Dave's bad side",
        " was slain by an evil spirit",
        " needs to stay away from the cursed mine",
        " enjoys death a little too much",
        " seriously needs to stop mining",
        " angered an evil spirit",
        " doesn't get a cookie from COOKIE",
        " should stay away",
        " needs to consider retirement",
        "'s head is now on Dave's mantel"
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        parent.revertPlayerBlocks(player);

        boolean containsPlayer = parent.contains(player);
        if (containsPlayer || parent.isOnHitList(player)) {
            parent.highScores.update(player, ScoreTypes.CURSED_MINE_DEATHS, BigInteger.ONE);

            if (parent.isOnHitList(player) && ChanceUtil.getChance(50)) {
                ChatUtil.sendNotice(player, "You feel as though a spirit is trying to tell you something...");
                event.getDrops().add(BookUtil.Lore.Areas.theGreatMine());
            }

            parent.removeFromHitList(player);
            event.setDeathMessage(player.getName() + CollectionUtil.getElement(DEATH_MESSAGES));
            parent.addSkull(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (parent.isOnHitList(player)) {
            player.setHealth(0);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (parent.blockState.hasPlayerBrokenBlocks(BlockStateKind.CURSED_MINE, player) && parent.contains(player)) {
            parent.addToHitList(player);
        }
    }

    @EventHandler
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (event.getKind() != PlayerStateKind.CURSED_MINE_SPECTATOR) {
            return;
        }

        Player player = event.getPlayer();

        // Create a spawn point looking at something interesting.
        int minPlayersToAttemptRandomAssignment = parent.isParticipant(player) ? 2 : 1;
        List<Player> participants = parent.getContainedParticipants();

        Location pointOfInterest = RegionUtil.getCenterAt(parent.getWorld(), 68, parent.getRegion());
        Location targetLocation = LocationUtil.pickLocation(
                parent.getWorld(),
                pointOfInterest.getY() + 7,
                new NonSolidRegionChecker(parent.getRegion(), parent.getWorld())
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

    private static EDBEExtractor<Zombie, Player, Projectile> GHOST_ATTACK_EXTRACTOR = new EDBEExtractor<>(
        Zombie.class,
        Player.class,
        Projectile.class
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        CombatantPair<Zombie, Player, Projectile> result = GHOST_ATTACK_EXTRACTOR.extractFrom(event);
        if (result == null) {
            return;
        }

        parent.tryTriggerHaunting(result.getAttacker(), result.getDefender());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallTrigger(PitfallTriggerEvent event) {
        Entity triggeringEntity = event.getEntity();
        if (!(triggeringEntity instanceof Zombie)) {
            return;
        }

        if (parent.isGhost((Zombie) triggeringEntity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEntity(EntityDamageEvent event) {
        if (event.getCause() == ENTITY_ATTACK) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Zombie)) {
            return;
        }

        if (parent.isGhost((Zombie) entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetEntityEvent(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Zombie)) {
            return;
        }

        if (!parent.isGhost((Zombie) entity)) {
            return;
        }

        Entity currentTarget = ((Zombie) entity).getTarget();
        if (currentTarget != null && parent.contains(currentTarget)) {
            event.setCancelled(true);
        }
    }
}
