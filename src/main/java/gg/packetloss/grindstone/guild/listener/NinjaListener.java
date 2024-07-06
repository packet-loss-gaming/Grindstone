/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.listener;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.Pitfall.bukkit.event.PitfallTriggerEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackSelectEvent;
import gg.packetloss.grindstone.events.guild.*;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.NinjaPower;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.NinjaState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.guild.ninja.Ignition;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.grindstone.world.type.city.combat.PvPComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NinjaListener implements Listener {
    private static final double DEFAULT_MAX_HEALTH = 20;

    private Function<Player, InternalGuildState> internalStateLookup;

    public NinjaListener(Function<Player, InternalGuildState> internalStateLookup) {
        this.internalStateLookup = internalStateLookup;
    }

    private Optional<NinjaState> getStateAllowDisabled(Player player) {
        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState instanceof NinjaState) {
            return Optional.of((NinjaState) internalState);
        }
        return Optional.empty();

    }

    private Optional<NinjaState> getState(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return Optional.empty();
        }

        return getStateAllowDisabled(player).filter(InternalGuildState::isEnabled);
    }

    private Optional<RogueState> getRogueState(Player player) {
        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState.isEnabled() && internalState instanceof RogueState) {
            return Optional.of((RogueState) internalState);
        }
        return Optional.empty();
    }

    private void applyPlayerModifications(Player player, NinjaState state) {
        double multiplier = state.hasPower(NinjaPower.EXTRA_HEALTH) ? 1.3f : 1;
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DEFAULT_MAX_HEALTH * multiplier);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPowersEnable(GuildPowersEnableEvent event) {
        if (event.getGuild() != GuildType.NINJA) {
            return;
        }

        Player player = event.getPlayer();
        applyPlayerModifications(player, getStateAllowDisabled(player).orElseThrow());

        ChatUtil.sendNotice(player, "You are inspired and become a ninja!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelUp(GuildLevelUpEvent event) {
        if (event.getGuild() != GuildType.NINJA) {
            return;
        }

        Player player = event.getPlayer();
        getState(player).ifPresent((state) -> {
            applyPlayerModifications(player, state);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPowersDisable(GuildPowersDisableEvent event) {
        if (event.getGuild() != GuildType.NINJA) {
            return;
        }

        Player player = event.getPlayer();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DEFAULT_MAX_HEALTH);

        ChatUtil.sendNotice(player, "You return to your previous boring existence.");
    }

    @EventHandler
    public void onProjectileLaunch(EntityShootBowEvent event) {
        Entity e = event.getProjectile();
        Projectile p = e instanceof Projectile ? (Projectile) e : null;
        if (p == null || p.getShooter() == null || !(p.getShooter() instanceof Player)) return;

        Player player = (Player) p.getShooter();
        if (p instanceof Arrow) {
            Optional<NinjaState> optState = getState(player);
            if (optState.isEmpty()) {
                return;
            }

            NinjaState state = optState.get();
            state.addArrow((Arrow) p);

            if (ChanceUtil.getChance(4)) {
                event.setConsumeArrow(false);
            }
        }
    }

    private static EDBEExtractor<Player, LivingEntity, Arrow> extractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Arrow.class
    );

    private void entityDamage(EntityDamageByEntityEvent event) {
        CombatantPair<Player, LivingEntity, Arrow> result = extractor.extractFrom(event);

        if (result == null) return;

        final Player attacker = result.getAttacker();
        Optional<NinjaState> optState = getState(attacker);
        if (optState.isEmpty()) {
            return;
        }

        if (!result.hasProjectile()) {
            return;
        }

        NinjaState state = optState.get();
        EntityDamageEvent lastEvent = attacker.getLastDamageCause();
        if (lastEvent != null && state.hasPower(NinjaPower.HEALING_ARROWS)) {
            double lastDamage = lastEvent.getFinalDamage();
            double scale = ((attacker.getMaxHealth() - attacker.getHealth()) / attacker.getMaxHealth());

            int chanceRuns = state.hasPower(NinjaPower.FULL_BLOODED_VAMPIRE) ? 1 : 2;
            EntityUtil.heal(attacker, ChanceUtil.getRandomNTimes((int) (scale * lastDamage), chanceRuns));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            entityDamage((EntityDamageByEntityEvent) event);
            return;
        }

        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Optional<NinjaState> optState = getState(player);
        if (optState.isEmpty()) {
            return;
        }

        NinjaState state = optState.get();
        switch (event.getCause()) {
            case FALL:
                if (state.hasPower(NinjaPower.GRACEFUL_FALLING)) {
                    event.setDamage(event.getDamage() * .8);
                }
                break;
            case FIRE:
            case FIRE_TICK:
                if (state.hasPower(NinjaPower.FIREPROOF)) {
                    player.setFireTicks(0);
                    event.setCancelled(true);
                }
                break;
        }
    }

    private void handleArrowBombArrow(Player player, NinjaState state, Arrow arrow) {
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setNumberOfRuns(20);

        taskBuilder.setAction((times) -> {
            SingleBlockParticleEffect.puffOfSmoke(arrow.getLocation());
            return true;
        });
        taskBuilder.setFinishAction(() -> {
            PotionType basePotionType = arrow.getBasePotionType();

            boolean isPotionArrow = basePotionType != null;
            if (isPotionArrow && state.hasPower(NinjaPower.POTION_ARROW_BOMBS)) {
                AreaEffectCloud effectCloud = arrow.getWorld().spawn(arrow.getLocation(), AreaEffectCloud.class);
                effectCloud.setBasePotionType(basePotionType);
                effectCloud.setDuration(20 * 10);
                effectCloud.setSource(arrow.getShooter());
            } else {
                float launchForce = arrow.getMetadata("launch-force").get(0).asFloat();

                ExplosionStateFactory.createPvPExplosion(
                    player,
                    arrow.getLocation(), Math.max(2, 4 * launchForce),
                    false,
                    state.getSettings().shouldAllowEnvironmentalDamage()
                );
            }

            arrow.remove();
        });

        taskBuilder.build();
    }

    public void arrowBomb(Player player, NinjaState state) {
        List<Arrow> arrows = state.getRecentArrows();
        if (arrows.isEmpty()) return;

        NinjaArrowBombEvent event = new NinjaArrowBombEvent(
                player,
                arrows
        );

        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        state.arrowBomb(arrows.size());

        for (Arrow arrow : arrows) {
            handleArrowBombArrow(player, state, arrow);
        }
    }

    private List<LivingEntity> refineSmokeBombTargets(Player player, Collection<LivingEntity> entities) {
        return entities.stream().filter(e -> {
            if (e instanceof Player) {
                return PvPComponent.allowsPvP(player, (Player) e);
            }

            return EntityUtil.isHostileMob(e);
        }).sorted(new EntityDistanceComparator(player.getLocation())).collect(Collectors.toList());
    }

    private Optional<Location> getSmokeBombOldLoc(Player player, List<LivingEntity> entities) {
        Entity targetEntity = null;

        for (Entity entity : entities) {
            if (entity.equals(player) || !(entity instanceof LivingEntity)) continue;

            targetEntity = entity;
        }

        if (targetEntity == null) {
            return Optional.empty();
        }

        Location targetLoc = targetEntity.getLocation();
        targetLoc.setDirection(VectorUtil.createDirectionalVector(player.getLocation(), targetLoc));
        return Optional.of(targetLoc);
    }

    public void smokeBomb(final Player player, NinjaState state) {

        Collection<LivingEntity> rawEntities = player.getLocation().getNearbyLivingEntities(4);
        List<LivingEntity> entities = refineSmokeBombTargets(player, rawEntities);

        Optional<Location> optSmokeBombLoc = getSmokeBombOldLoc(player, entities);
        if (optSmokeBombLoc.isEmpty()) {
            return;
        }

        NinjaSmokeBombEvent event = new NinjaSmokeBombEvent(
                player,
                3,
                30,
                entities,
                optSmokeBombLoc.get(),
                player.getLocation()
        );

        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled() || entities.isEmpty()) return;

        state.smokeBomb();

        Location targetLoc = event.getTargetLoc();
        int totalHealed = 0;
        for (LivingEntity entity : entities) {
            EntityUtil.forceDamage(player, entity, 1);
            totalHealed += 1;

            if (entity instanceof Player) {
                getRogueState((Player) entity).ifPresent(RogueState::stallBlip);

                ChatUtil.sendWarning(entity, "You hear a strange ticking sound...");
            }

            Location newEntityLoc = targetLoc.clone();

            newEntityLoc.setPitch((float) (ChanceUtil.getRandom(361.0) - 1));
            newEntityLoc.setYaw((float) (ChanceUtil.getRandom(181.0) - 91));

            entity.teleport(newEntityLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }

        if (state.hasPower(NinjaPower.VAMPIRIC_SMOKE_BOMB)) {
            int chanceRuns = state.hasPower(NinjaPower.FULL_BLOODED_VAMPIRE) ? 1 : 2;
            EntityUtil.heal(player, ChanceUtil.getRandomNTimes(totalHealed, chanceRuns));
        }

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setNumberOfRuns(event.getDelay());

        Location bombLocation = targetLoc.clone();

        // Offset by 1 so that the bomb is not messed up by blocks
        if (!EnvironmentUtil.isAirBlock(bombLocation.getBlock())) {
            bombLocation.add(0, 1, 0);
        }

        taskBuilder.setAction((times) -> {
            for (int i = 0; i < 10; ++i) {
                bombLocation.getWorld().spawnParticle(
                        Particle.CAMPFIRE_COSY_SMOKE,
                        bombLocation.getX() + ChanceUtil.getRangedRandom(-.5, .5),
                        bombLocation.getY(),
                        bombLocation.getZ() + ChanceUtil.getRangedRandom(-.5, .5),
                        0,
                        ChanceUtil.getRangedRandom(-.1, .1),
                        .1,
                        ChanceUtil.getRangedRandom(-.1, .1)
                );
            }

            return true;
        });
        taskBuilder.setFinishAction(() -> {
            ExplosionStateFactory.createPvPExplosion(
                    player,
                    bombLocation,
                    event.getExplosionPower(),
                    false,
                    state.getSettings().shouldAllowEnvironmentalDamage()
            );
        });

        taskBuilder.build();

        player.teleport(event.getTeleportLoc(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    private boolean isGrappleable(Material blockType) {
        return blockType.isSolid() && blockType != Material.BARRIER;
    }

    public void grapple(final Player player, NinjaState state, Block block, BlockFace clickedFace, double maxClimb) {

        NinjaGrappleEvent event = new NinjaGrappleEvent(player, maxClimb);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        Location k = player.getLocation();

        switch (k.getBlock().getType()) {
            case AIR:
                k.setX(k.getBlockX() + .5);
                k.setZ(k.getBlockZ() + .5);
                player.teleport(k, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                break;
        }


        CommandBook.server().getPluginManager().callEvent(new ThrowPlayerEvent(player));

        Vector vel = player.getLocation().getDirection();
        vel.multiply(.5);
        vel.setY(.6);

        int playerY = player.getLocation().getBlockY();
        int z;
        for (z = 0; block.getY() >= playerY; z++) {
            if (block.getY() == playerY) {
                if (EnvironmentUtil.isWater(block.getRelative(BlockFace.DOWN))) {
                    vel.setY(vel.getY() * 2);
                }
                break;
            }
            block = block.getRelative(BlockFace.DOWN);
        }

        Block nextBlock = block.getRelative(BlockFace.UP);
        boolean nextIsGrappleable = isGrappleable(nextBlock.getType());

        int i;
        Vector increment = new Vector(0, .1, 0);
        for (i = 0; i < event.getMaxClimb() && (i < z || isGrappleable(block.getType()) || nextIsGrappleable); i++) {

            // Determine whether we need to add more velocity
            double ctl = nextIsGrappleable ? 1 : 0; // FIXME: reimplement this BlockType.centralTopLimit(block.getType(), block.getData());

            if (EnvironmentUtil.isWater(block.getRelative(clickedFace))) {
                ctl *= 2;
            }

            vel.add(ctl > 1 ? increment.clone().multiply(ctl) : increment);

            // Update blocks
            block = nextBlock;
            nextBlock = nextBlock.getRelative(BlockFace.UP);

            // Update boolean
            nextIsGrappleable = isGrappleable(nextBlock.getType());
        }

        player.setFallDistance(0F);

        // As of 1.16.4, delaying by a tick significantly improves the reliability of the grapple velocity
        // adjustment. In particularly, this works around a problem where the player sort of "twitches"
        // and just ends up standing next to a block with no upward velocity -- perhaps the client is
        // "fighting back" since we clicked on a block?
        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            player.setVelocity(vel);
        }, 1);

        state.grapple(Math.max(i, 3) * 200);
    }

    public void teleport(Player player) {

        Location[] locations = new Location[]{
                player.getLocation(),
                player.getEyeLocation()
        };
        EnvironmentUtil.generateRadialEffect(locations, org.bukkit.Effect.SMOKE);

        ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.NINJA_STAR), 1, false);
        player.teleport(new Location(Bukkit.getWorld("City"), 150.0001, 45, -443.0001, -180, 0));
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        ItemStack stack = player.getItemInHand();

        Optional<NinjaState> optState = getState(player);
        if (optState.isEmpty()) {
            return;
        }

        NinjaState state = optState.get();

        Block clicked = event.getClickedBlock();
        BlockFace face = event.getBlockFace();

        boolean usingBow = stack.getType() == Material.BOW;

        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
                if (!state.canArrowBomb()) break;
                if (usingBow) {
                    arrowBomb(player, state);
                }
                break;
            case LEFT_CLICK_BLOCK:
                if (!state.canSmokeBomb()) break;
                if (usingBow) {
                    smokeBomb(player, state);
                }
                break;
            case RIGHT_CLICK_BLOCK: {
                if (!state.canGrapple()) break;

                boolean isClimbableItem = ItemUtil.isNullItemStack(stack)
                                       || ItemUtil.isSword(stack)
                                       || ItemUtil.isTool(stack);

                if (!isClimbableItem) break;

                // Do not attempt to climb shrubs.
                if (EnvironmentUtil.isShrubBlock(clicked)) {
                    break;
                }
                if (!face.equals(BlockFace.UP) && !face.equals(BlockFace.DOWN)) {
                    // Never climb if the clicked block was interactive, or could've been a misclick
                    // of a nearby interactive block.
                    if (EnvironmentUtil.isMaybeInteractiveBlock(clicked, face)) {
                        break;
                    }

                    // Check if the block is higher than the player
                    if (clicked.getLocation().getY() < player.getLocation().getY()) break;

                    // Check the 2D distance between the block and the player
                    if (LocationUtil.distanceSquared2D(clicked.getLocation().add(.5, 0, .5), player.getLocation()) <= 4) {
                        // If the player is sneaking treat this as a "slowing" option,
                        // if not sneaking, treat this as a proper grapple.
                        int maxClimb = state.hasPower(NinjaPower.MASTER_CLIMBER) ? 14 : 9;
                        grapple(player, state, clicked, face, player.isSneaking() ? 0 : maxClimb);
                    }
                }
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPitfallEvent(PitfallTriggerEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;

            Optional<NinjaState> optState = getState(player);
            if (optState.isEmpty()) {
                return;
            }

            NinjaState state = optState.get();
            if (state.hasPower(NinjaPower.PITFALL_SNEAK) && player.isSneaking()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackSelectEvent event) {
        Player player = event.getPlayer();

        Optional<NinjaState> optState = getState(player);
        if (optState.isEmpty()) {
            return;
        }

        NinjaState state = optState.get();

        SpecialAttack attack = event.getSpec();

        if (event.getContext().equals(SpecType.RANGED)) {
            if (state.hasPower(NinjaPower.IGNITION_SPECIAL) && ChanceUtil.getChance(14)) {
                event.setSpec(new Ignition(attack.getOwner(), attack.getUsedItem(), attack.getTarget()));
            }
        }
    }
}
